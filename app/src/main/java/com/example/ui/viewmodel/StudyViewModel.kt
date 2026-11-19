package com.example.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.AmbientSound
import com.example.core.AmbientSoundPlayer
import com.example.core.AppFont
import com.example.core.LocalAppPreferences
import com.example.core.PreferencesManager
import com.example.core.ThemeMode
import com.example.core.UiState
import com.example.data.model.AchievementCatalog
import com.example.data.model.Announcement
import com.example.data.model.BlockedUser
import com.example.data.model.Challenge
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.StudyGroup
import com.example.data.model.StudyNote
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.UserAchievement
import com.example.data.model.UserNotification
import com.example.data.repository.AiPlanSuggestion
import com.example.data.repository.AiStudyRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.StudyRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimerPhase(val label: String) {
    FOCUS("Focus Session"),
    SHORT_BREAK("Short Break"),
    LONG_BREAK("Long Break")
}

data class PomodoroTimerState(
    val mode: String = "POMODORO_25",
    val phase: TimerPhase = TimerPhase.FOCUS,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val running: Boolean = false,
    val startedAtWallMillis: Long = 0L,
    val selectedSubjectId: String = "general",
    val selectedSubjectName: String = "General Study",
    val sessionNotes: String = "",
    val completedRewardBanner: String? = null
)

data class AiBubble(
    val id: String,
    val fromUser: Boolean,
    val text: String
)

data class AiUiState(
    val messages: List<AiBubble> = listOf(
        AiBubble(
            id = "welcome",
            fromUser = false,
            text = "Hi! I'm your Study Time AI Coach powered by Gemini. Ask me for a revision schedule, active-recall quiz, or tap 'Generate Today's Plan' below."
        )
    ),
    val streaming: Boolean = false,
    val planLoading: Boolean = false,
    val latestPlan: AiPlanSuggestion? = null,
    val errorMessage: String? = null
)

class StudyViewModel(
    private val studyRepo: StudyRepository,
    private val socialRepo: SocialRepository,
    private val aiRepo: AiStudyRepository,
    private val prefsManager: PreferencesManager,
    private val soundPlayer: AmbientSoundPlayer,
    private val currentUserId: String
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(tagLabel: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $tagLabel", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $tagLabel"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val preferences: StateFlow<LocalAppPreferences> = prefsManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), LocalAppPreferences())

    val privateUser: StateFlow<UiState<PrivateUser?>> =
        studyRepo.observePrivateUser(currentUserId).toUiStateFlow("account")

    val publicProfile: StateFlow<UiState<PublicProfile?>> =
        studyRepo.observePublicProfile(currentUserId).toUiStateFlow("profile")

    val subjects: StateFlow<UiState<List<Subject>>> =
        studyRepo.observeSubjects(currentUserId).toUiStateFlow("subjects")

    val sessions: StateFlow<UiState<List<StudySession>>> =
        studyRepo.observeSessions(currentUserId).toUiStateFlow("sessions")

    val tasks: StateFlow<UiState<List<StudyTask>>> =
        studyRepo.observeTasks(currentUserId).toUiStateFlow("tasks")

    val notes: StateFlow<UiState<List<StudyNote>>> =
        studyRepo.observeNotes(currentUserId).toUiStateFlow("notes")

    val achievements: StateFlow<UiState<List<UserAchievement>>> =
        studyRepo.observeAchievements(currentUserId).toUiStateFlow("achievements")

    val notifications: StateFlow<UiState<List<UserNotification>>> =
        studyRepo.observeNotifications(currentUserId).toUiStateFlow("notifications")

    val blockedUsers: StateFlow<UiState<List<BlockedUser>>> =
        studyRepo.observeBlockedUsers(currentUserId).toUiStateFlow("blocked users")

    val allPublicProfiles: StateFlow<UiState<List<PublicProfile>>> =
        socialRepo.observeAllPublicProfiles().toUiStateFlow("leaderboard")

    val friendRequests: StateFlow<UiState<List<FriendRequest>>> =
        socialRepo.observeFriendRequests(currentUserId).toUiStateFlow("friend requests")

    val studyGroups: StateFlow<UiState<List<StudyGroup>>> =
        socialRepo.observePublicStudyGroups(currentUserId).toUiStateFlow("study groups")

    val directChats: StateFlow<UiState<List<DirectChat>>> =
        socialRepo.observeDirectChats(currentUserId).toUiStateFlow("chats")

    val challenges: StateFlow<UiState<List<Challenge>>> =
        socialRepo.observeChallenges(currentUserId).toUiStateFlow("challenges")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        socialRepo.observeAnnouncements().toUiStateFlow("announcements")

    private val _timerState = MutableStateFlow(PomodoroTimerState())
    val timerState: StateFlow<PomodoroTimerState> = _timerState.asStateFlow()

    private val _aiState = MutableStateFlow(AiUiState())
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _snackMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackMessages: SharedFlow<String> = _snackMessages.asSharedFlow()

    private var timerJob: Job? = null
    private var activeEndElapsedRealtime: Long = 0L

    fun initializeAccount(user: FirebaseUser) {
        viewModelScope.launch {
            studyRepo.ensureUserInitialized(user)
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Profile sync issue") }
        }
    }

    fun completeOnboarding(dailyGoalMinutes: Long, country: String, seedDemo: Boolean) {
        viewModelScope.launch {
            prefsManager.setOnboardingSeen(true)
            studyRepo.completeOnboarding(dailyGoalMinutes, country)
                .onSuccess {
                    if (seedDemo) {
                        studyRepo.seedStarterStudyKit()
                        _snackMessages.tryEmit("Starter subjects, tasks, and notes added!")
                    }
                }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to save onboarding") }
        }
    }

    fun seedStarterData() {
        viewModelScope.launch {
            studyRepo.seedStarterStudyKit()
                .onSuccess { _snackMessages.tryEmit("Starter Study Kit synced to your account!") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not seed starter kit") }
        }
    }

    // --- POMODORO TIMER (Anti-manipulation & accurate background wall-clock tracking) ---

    fun selectTimerPreset(mode: String, focusMins: Int, breakMins: Int) {
        val state = _timerState.value
        if (state.running || state.startedAtWallMillis > 0L) {
            _snackMessages.tryEmit("Reset the active session before changing timer duration.")
            return
        }
        val clampedFocus = focusMins.coerceIn(1, 180)
        val clampedBreak = breakMins.coerceIn(1, 60)
        _timerState.update {
            it.copy(
                mode = mode,
                phase = TimerPhase.FOCUS,
                focusMinutes = clampedFocus,
                breakMinutes = clampedBreak,
                totalSeconds = clampedFocus * 60,
                remainingSeconds = clampedFocus * 60,
                completedRewardBanner = null
            )
        }
    }

    fun selectTimerSubject(subjectId: String, subjectName: String) {
        _timerState.update {
            it.copy(selectedSubjectId = subjectId, selectedSubjectName = subjectName)
        }
    }

    fun updateSessionNotes(notes: String) {
        _timerState.update { it.copy(sessionNotes = notes.take(500)) }
    }

    fun startOrResumeTimer() {
        val current = _timerState.value
        if (current.running) return
        val nowWall = System.currentTimeMillis()
        val startedWall = if (current.startedAtWallMillis == 0L) nowWall else current.startedAtWallMillis
        activeEndElapsedRealtime = SystemClock.elapsedRealtime() + (current.remainingSeconds * 1000L)

        _timerState.update {
            it.copy(
                running = true,
                startedAtWallMillis = startedWall,
                completedRewardBanner = null
            )
        }

        val activeSound = preferences.value.ambientSound
        if (activeSound != AmbientSound.NONE && current.phase == TimerPhase.FOCUS) {
            soundPlayer.startAmbient(activeSound)
        }

        viewModelScope.launch {
            studyRepo.updatePrivacyAndPreferences(onlineStatus = if (current.phase == TimerPhase.FOCUS) "FOCUSING" else "ONLINE")
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val leftMs = activeEndElapsedRealtime - SystemClock.elapsedRealtime()
                val leftSec = ((leftMs + 999L) / 1000L).toInt().coerceAtLeast(0)
                _timerState.update { it.copy(remainingSeconds = leftSec) }
                if (leftSec <= 0) {
                    onTimerPhaseFinished()
                    break
                }
                delay(500L)
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        soundPlayer.stopAmbient()
        _timerState.update { it.copy(running = false) }
        viewModelScope.launch {
            studyRepo.updatePrivacyAndPreferences(onlineStatus = "ONLINE")
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        soundPlayer.stopAmbient()
        val current = _timerState.value
        val targetMins = when (current.phase) {
            TimerPhase.FOCUS -> current.focusMinutes
            TimerPhase.SHORT_BREAK -> current.breakMinutes
            TimerPhase.LONG_BREAK -> (current.breakMinutes * 3).coerceAtMost(30)
        }
        _timerState.update {
            it.copy(
                running = false,
                startedAtWallMillis = 0L,
                totalSeconds = targetMins * 60,
                remainingSeconds = targetMins * 60
            )
        }
        viewModelScope.launch {
            studyRepo.updatePrivacyAndPreferences(onlineStatus = "ONLINE")
        }
    }

    fun switchBreakPhase(phase: TimerPhase) {
        if (_timerState.value.running) {
            _snackMessages.tryEmit("Pause or reset before switching timer phase.")
            return
        }
        val mins = when (phase) {
            TimerPhase.FOCUS -> _timerState.value.focusMinutes
            TimerPhase.SHORT_BREAK -> _timerState.value.breakMinutes
            TimerPhase.LONG_BREAK -> (_timerState.value.breakMinutes * 3).coerceAtMost(30)
        }
        _timerState.update {
            it.copy(
                phase = phase,
                startedAtWallMillis = 0L,
                totalSeconds = mins * 60,
                remainingSeconds = mins * 60
            )
        }
    }

    fun dismissRewardBanner() {
        _timerState.update { it.copy(completedRewardBanner = null) }
    }

    private suspend fun onTimerPhaseFinished() {
        soundPlayer.stopAmbient()
        if (preferences.value.chimeEnabled) {
            soundPlayer.playCompletionChime()
        }
        val state = _timerState.value
        if (state.phase == TimerPhase.FOCUS) {
            val endWall = System.currentTimeMillis()
            val profile = (publicProfile.value as? UiState.Success)?.data
            val existingCount = (sessions.value as? UiState.Success)?.data?.size ?: 0
            val existingAch = (achievements.value as? UiState.Success)?.data ?: emptyList()

            val res = studyRepo.recordVerifiedStudySession(
                subjectId = state.selectedSubjectId,
                subjectName = state.selectedSubjectName,
                durationMinutes = state.focusMinutes.toLong(),
                mode = state.mode,
                notes = state.sessionNotes,
                verifiedStartMillis = state.startedAtWallMillis,
                verifiedEndMillis = endWall,
                currentProfile = profile,
                existingSessionsCount = existingCount,
                existingAchievements = existingAch
            )
            res.onSuccess { (xp, unlocked) ->
                val achSuffix = if (unlocked.isNotEmpty()) " • Unlocked: ${unlocked.joinToString(", ")}" else ""
                val msg = "+$xp XP earned for ${state.focusMinutes}m of ${state.selectedSubjectName}$achSuffix"
                _timerState.update {
                    it.copy(
                        running = false,
                        phase = TimerPhase.SHORT_BREAK,
                        startedAtWallMillis = 0L,
                        totalSeconds = it.breakMinutes * 60,
                        remainingSeconds = it.breakMinutes * 60,
                        sessionNotes = "",
                        completedRewardBanner = msg
                    )
                }
            }.onFailure { err ->
                _snackMessages.tryEmit(err.localizedMessage ?: "Failed to save study session")
                _timerState.update {
                    it.copy(
                        running = false,
                        startedAtWallMillis = 0L,
                        remainingSeconds = it.focusMinutes * 60
                    )
                }
            }
        } else {
            _timerState.update {
                it.copy(
                    running = false,
                    phase = TimerPhase.FOCUS,
                    startedAtWallMillis = 0L,
                    totalSeconds = it.focusMinutes * 60,
                    remainingSeconds = it.focusMinutes * 60,
                    completedRewardBanner = "Break complete! Ready for your next focus block."
                )
            }
        }
    }

    // --- SUBJECTS, TASKS, NOTES ---

    fun addSubject(name: String, iconKey: String, colorHex: String, description: String, targetHours: Int) {
        viewModelScope.launch {
            val plan = (privateUser.value as? UiState.Success)?.data?.planTier ?: "FREE"
            val count = (subjects.value as? UiState.Success)?.data?.size ?: 0
            studyRepo.addSubject(
                name = name,
                iconKey = iconKey,
                colorHex = colorHex,
                description = description,
                targetMinutes = targetHours.coerceIn(1, 1000) * 60L,
                planTier = plan,
                currentCount = count
            ).onSuccess {
                _snackMessages.tryEmit("Subject '$name' added")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Could not add subject")
            }
        }
    }

    fun updateSubject(subjectId: String, name: String, iconKey: String, colorHex: String, description: String, targetHours: Int) {
        viewModelScope.launch {
            studyRepo.updateSubject(
                subjectId = subjectId,
                name = name,
                iconKey = iconKey,
                colorHex = colorHex,
                description = description,
                targetMinutes = targetHours.coerceIn(1, 1000) * 60L
            ).onSuccess {
                _snackMessages.tryEmit("Subject updated")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Could not update subject")
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            studyRepo.deleteSubject(subjectId)
                .onSuccess { _snackMessages.tryEmit("Subject deleted") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not delete subject") }
        }
    }

    fun addTask(
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        priority: String,
        dueDateMillis: Long,
        reminderEnabled: Boolean,
        recurring: String,
        examFlag: Boolean
    ) {
        viewModelScope.launch {
            studyRepo.addTask(
                title = title,
                description = description,
                subjectId = subjectId,
                subjectName = subjectName,
                priority = priority,
                dueDateMillis = dueDateMillis,
                reminderEnabled = reminderEnabled,
                recurring = recurring,
                examFlag = examFlag
            ).onSuccess {
                _snackMessages.tryEmit(if (examFlag) "Exam countdown added" else "Task added to planner")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Failed to add task")
            }
        }
    }

    fun toggleTaskCompleted(taskId: String, completed: Boolean) {
        viewModelScope.launch {
            studyRepo.toggleTaskCompleted(taskId, completed)
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to update task") }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            studyRepo.deleteTask(taskId)
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to delete task") }
        }
    }

    fun saveNote(noteId: String?, title: String, content: String, subjectId: String, subjectName: String, pinned: Boolean) {
        viewModelScope.launch {
            studyRepo.saveNote(noteId, title, content, subjectId, subjectName, pinned)
                .onSuccess { _snackMessages.tryEmit("Note saved") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to save note") }
        }
    }

    fun toggleNotePinned(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            studyRepo.toggleNotePinned(noteId, pinned)
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            studyRepo.deleteNote(noteId)
                .onSuccess { _snackMessages.tryEmit("Note deleted") }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch { studyRepo.markNotificationRead(notificationId) }
    }

    // --- PROFILE, PRIVACY & PREFERENCES ---

    fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        country: String,
        avatarEmoji: String,
        avatarColorHex: String,
        dailyGoalMinutes: Long
    ) {
        viewModelScope.launch {
            studyRepo.updateProfile(
                displayName = displayName,
                username = username,
                bio = bio,
                country = country,
                avatarEmoji = avatarEmoji,
                avatarColorHex = avatarColorHex,
                dailyGoalMinutes = dailyGoalMinutes
            ).onSuccess {
                _snackMessages.tryEmit("Profile saved!")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Could not save profile")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefsManager.setThemeMode(mode)
            studyRepo.updatePrivacyAndPreferences(themePreference = mode.name)
        }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch {
            prefsManager.setAppFont(font)
            studyRepo.updatePrivacyAndPreferences(fontPreference = font.name)
        }
    }

    fun setAmbientSound(sound: AmbientSound) {
        viewModelScope.launch {
            prefsManager.setAmbientSound(sound)
            if (_timerState.value.running && _timerState.value.phase == TimerPhase.FOCUS) {
                soundPlayer.startAmbient(sound)
            }
        }
    }

    fun setChimeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setChimeEnabled(enabled) }
    }

    fun setBreakReminder(enabled: Boolean) {
        viewModelScope.launch { prefsManager.setBreakReminderEnabled(enabled) }
    }

    fun updatePrivacy(hideFromLeaderboard: Boolean, publicProfile: Boolean, notificationsMuted: Boolean) {
        viewModelScope.launch {
            studyRepo.updatePrivacyAndPreferences(
                hideFromLeaderboard = hideFromLeaderboard,
                publicProfile = publicProfile,
                notificationsMuted = notificationsMuted
            ).onSuccess {
                _snackMessages.tryEmit("Privacy preferences updated")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Failed to update privacy")
            }
        }
    }

    fun deleteAccountData(onDone: () -> Unit) {
        viewModelScope.launch {
            studyRepo.deleteAccountData()
                .onSuccess {
                    _snackMessages.tryEmit("Account data deleted.")
                    onDone()
                }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to delete account data") }
        }
    }

    // --- SOCIAL, CHAT, GROUPS, CHALLENGES ---

    fun sendFriendRequest(toUid: String, toName: String) {
        viewModelScope.launch {
            val myName = (publicProfile.value as? UiState.Success)?.data?.displayName ?: "Scholar"
            socialRepo.sendFriendRequest(myName, toUid, toName)
                .onSuccess { _snackMessages.tryEmit("Friend request sent to $toName") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not send request") }
        }
    }

    fun respondFriendRequest(requestId: String, accept: Boolean) {
        viewModelScope.launch {
            socialRepo.respondFriendRequest(requestId, accept)
                .onSuccess { _snackMessages.tryEmit(if (accept) "Friend request accepted!" else "Request declined") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Action failed") }
        }
    }

    fun removeFriend(requestId: String) {
        viewModelScope.launch {
            socialRepo.removeFriend(requestId)
                .onSuccess { _snackMessages.tryEmit("Connection removed") }
        }
    }

    fun openDirectChat(otherUid: String, otherName: String, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val myName = (publicProfile.value as? UiState.Success)?.data?.displayName ?: "Scholar"
            socialRepo.openOrCreateDirectChat(myName, otherUid, otherName)
                .onSuccess { chatId -> onOpened(chatId) }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not open chat") }
        }
    }

    fun observeChatMessages(chatId: String): Flow<List<ChatMessage>> =
        socialRepo.observeChatMessages(chatId, currentUserId)

    fun sendDirectMessage(chatId: String, text: String, memberUids: List<String>, replyTo: String?) {
        viewModelScope.launch {
            val myName = (publicProfile.value as? UiState.Success)?.data?.displayName ?: "Scholar"
            socialRepo.sendDirectMessage(chatId, myName, text, memberUids, replyTo)
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Message failed to send") }
        }
    }

    fun reactToDirectMessage(chatId: String, messageId: String, emoji: String) {
        viewModelScope.launch { socialRepo.reactToDirectMessage(chatId, messageId, emoji) }
    }

    fun deleteDirectMessage(chatId: String, messageId: String) {
        viewModelScope.launch { socialRepo.deleteDirectMessage(chatId, messageId) }
    }

    fun toggleChatMuted(chat: DirectChat) {
        viewModelScope.launch { socialRepo.toggleChatMuted(chat) }
    }

    fun createStudyGroup(name: String, description: String, goalHours: Int, publicGroup: Boolean) {
        viewModelScope.launch {
            socialRepo.createStudyGroup(name, description, goalHours.coerceAtLeast(1) * 60L, publicGroup)
                .onSuccess { _snackMessages.tryEmit("Study group '$name' created!") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to create group") }
        }
    }

    fun joinStudyGroup(group: StudyGroup) {
        viewModelScope.launch {
            socialRepo.joinStudyGroup(group)
                .onSuccess { _snackMessages.tryEmit("Joined ${group.name}!") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Failed to join group") }
        }
    }

    fun leaveStudyGroup(group: StudyGroup) {
        viewModelScope.launch {
            socialRepo.leaveStudyGroup(group)
                .onSuccess { _snackMessages.tryEmit("Left ${group.name}") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not leave group") }
        }
    }

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessage>> =
        socialRepo.observeGroupMessages(groupId)

    fun sendGroupMessage(groupId: String, text: String, replyTo: String?) {
        viewModelScope.launch {
            val myName = (publicProfile.value as? UiState.Success)?.data?.displayName ?: "Scholar"
            socialRepo.sendGroupMessage(groupId, myName, text, replyTo)
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Message failed") }
        }
    }

    fun reactToGroupMessage(groupId: String, messageId: String, emoji: String) {
        viewModelScope.launch { socialRepo.reactToGroupMessage(groupId, messageId, emoji) }
    }

    fun createChallenge(title: String, description: String, targetMinutes: Int, xpReward: Int, scope: String) {
        viewModelScope.launch {
            val myName = (publicProfile.value as? UiState.Success)?.data?.displayName ?: "Scholar"
            socialRepo.createChallenge(
                title = title,
                description = description,
                creatorName = myName,
                targetMinutes = targetMinutes.coerceIn(15, 10000).toLong(),
                xpReward = xpReward.coerceIn(10, 5000).toLong(),
                scope = scope
            ).onSuccess {
                _snackMessages.tryEmit("Challenge '$title' created!")
            }.onFailure {
                _snackMessages.tryEmit(it.localizedMessage ?: "Could not create challenge")
            }
        }
    }

    fun joinChallenge(challenge: Challenge) {
        viewModelScope.launch {
            socialRepo.joinChallenge(challenge)
                .onSuccess { _snackMessages.tryEmit("Joined challenge: ${challenge.title}") }
                .onFailure { _snackMessages.tryEmit(it.localizedMessage ?: "Could not join challenge") }
        }
    }

    fun blockAndReportUser(targetUid: String, targetName: String, reason: String) {
        viewModelScope.launch {
            studyRepo.blockUser(targetUid, targetName)
            if (reason.isNotBlank()) {
                socialRepo.submitReport(targetUid, reason)
            }
            _snackMessages.tryEmit("Blocked $targetName and submitted report.")
        }
    }

    fun unblockUser(targetUid: String) {
        viewModelScope.launch {
            studyRepo.unblockUser(targetUid)
                .onSuccess { _snackMessages.tryEmit("User unblocked") }
        }
    }

    // --- AI STUDY ASSISTANT (Firebase AI Logic gemini-3.8-flash) ---

    fun askAiCoach(userPrompt: String) {
        val clean = userPrompt.trim()
        if (clean.isEmpty() || _aiState.value.streaming) return

        val plan = (privateUser.value as? UiState.Success)?.data?.planTier ?: "FREE"
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentPrefs = preferences.value
        val usedToday = if (currentPrefs.aiRequestsDayKey == dayKey) currentPrefs.aiRequestsToday else 0
        if (plan != "PREMIUM" && usedToday >= 5) {
            _aiState.update {
                it.copy(errorMessage = "Free plan includes 5 AI Coach prompts per day. Connect Premium for unlimited AI coaching.")
            }
            return
        }

        val userBubble = AiBubble("u_${System.currentTimeMillis()}", true, clean)
        val modelBubbleId = "m_${System.currentTimeMillis() + 1}"

        val subjectsList = (subjects.value as? UiState.Success)?.data.orEmpty()
        val tasksList = (tasks.value as? UiState.Success)?.data.orEmpty()
        val profile = (publicProfile.value as? UiState.Success)?.data
        val contextPrompt = buildString {
            appendLine("Student Context:")
            appendLine("- Level ${profile?.level ?: 1}, Total study: ${profile?.totalStudyMinutes ?: 0} min, Daily goal: ${profile?.dailyGoalMinutes ?: 120} min, Streak: ${profile?.currentStreakDays ?: 0} days")
            if (subjectsList.isNotEmpty()) {
                appendLine("- Subjects: ${subjectsList.joinToString { "${it.name} (${it.completedMinutes}/${it.targetMinutes}m)" }}")
            }
            val openTasks = tasksList.filter { !it.completed }
            if (openTasks.isNotEmpty()) {
                appendLine("- Open Tasks: ${openTasks.take(5).joinToString { it.title }}")
            }
            appendLine()
            appendLine("Student Question: $clean")
        }

        _aiState.update {
            it.copy(
                messages = it.messages + userBubble + AiBubble(modelBubbleId, false, "Thinking..."),
                streaming = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                var receivedAny = false
                aiRepo.streamStudyAdvice(contextPrompt).collect { partial ->
                    receivedAny = true
                    _aiState.update { st ->
                        st.copy(
                            messages = st.messages.map { b ->
                                if (b.id == modelBubbleId) b.copy(text = partial) else b
                            }
                        )
                    }
                }
                if (receivedAny) {
                    prefsManager.incrementAiUsage(dayKey)
                }
                _aiState.update { it.copy(streaming = false) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI request timed out", e)
                _aiState.update { st ->
                    st.copy(
                        streaming = false,
                        messages = st.messages.filterNot { it.id == modelBubbleId && it.text == "Thinking..." },
                        errorMessage = "AI Coach timed out. Please verify your internet connection and try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Coach error", e)
                _aiState.update { st ->
                    st.copy(
                        streaming = false,
                        messages = st.messages.filterNot { it.id == modelBubbleId && it.text == "Thinking..." },
                        errorMessage = e.localizedMessage ?: "AI Coach failed to respond."
                    )
                }
            }
        }
    }

    fun generateStructuredPlan() {
        if (_aiState.value.planLoading) return
        _aiState.update { it.copy(planLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val subjectsList = (subjects.value as? UiState.Success)?.data.orEmpty()
            val tasksList = (tasks.value as? UiState.Success)?.data.orEmpty().filter { !it.completed }
            val goal = (publicProfile.value as? UiState.Success)?.data?.dailyGoalMinutes ?: 120L

            val subjText = if (subjectsList.isEmpty()) "General Study"
            else subjectsList.joinToString { "${it.name} (${it.completedMinutes}/${it.targetMinutes}m)" }
            val taskText = if (tasksList.isEmpty()) "No open tasks yet"
            else tasksList.take(6).joinToString { "${it.title} [${it.priority}]" }

            aiRepo.generateStructuredStudyPlan(subjText, taskText, goal)
                .onSuccess { plan ->
                    _aiState.update { it.copy(planLoading = false, latestPlan = plan) }
                }
                .onFailure { err ->
                    _aiState.update {
                        it.copy(
                            planLoading = false,
                            errorMessage = err.localizedMessage ?: "Failed to generate study plan"
                        )
                    }
                }
        }
    }

    fun applyAiTaskToPlanner(title: String, subject: String, priority: String) {
        addTask(
            title = title,
            description = "Added from Study Time AI Coach",
            subjectId = "",
            subjectName = subject,
            priority = priority,
            dueDateMillis = System.currentTimeMillis() + 86_400_000L,
            reminderEnabled = true,
            recurring = "NONE",
            examFlag = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundPlayer.stopAmbient()
    }

    private companion object {
        const val TAG = "StudyViewModel"
    }
}
