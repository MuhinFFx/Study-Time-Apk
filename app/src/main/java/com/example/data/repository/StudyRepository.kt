package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.AchievementCatalog
import com.example.data.model.BlockedUser
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.StudyNote
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.UserAchievement
import com.example.data.model.UserNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class StudyRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserInitialized(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val existingPrivate = privateRef.get().await()
            val rawName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: firebaseUser.email?.substringBefore("@")
                ?: "Scholar"
            val cleanUsername = rawName.lowercase()
                .replace(Regex("[^a-z0-9_]"), "_")
                .take(24)
                .ifBlank { "scholar_${uid.take(5)}" }

            if (!existingPrivate.exists()) {
                val privatePayload = mapOf(
                    "userId" to uid,
                    "email" to (firebaseUser.email ?: ""),
                    "username" to cleanUsername,
                    "displayName" to rawName.take(60),
                    "country" to "Global",
                    "dailyGoalMinutes" to 120L,
                    "themePreference" to "SYSTEM",
                    "planTier" to "FREE",
                    "role" to "USER",
                    "notificationsMuted" to false,
                    "onboardingCompleted" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                privateRef.set(privatePayload).await()
            }

            val existingPublic = publicRef.get().await()
            if (!existingPublic.exists()) {
                val publicPayload = mapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to rawName.take(60),
                    "bio" to "Building daily study consistency.",
                    "country" to "Global",
                    "avatarEmoji" to "🦉",
                    "avatarColorHex" to "#6C63FF",
                    "planTier" to "FREE",
                    "totalXp" to 0L,
                    "level" to 1L,
                    "totalStudyMinutes" to 0L,
                    "currentStreakDays" to 0L,
                    "longestStreakDays" to 0L,
                    "dailyGoalMinutes" to 120L,
                    "onlineStatus" to "ONLINE",
                    "hideFromLeaderboard" to false,
                    "publicProfile" to true,
                    "suspended" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                publicRef.set(publicPayload).await()
            } else {
                publicRef.update(
                    mapOf(
                        "onlineStatus" to "ONLINE",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    fun observePrivateUser(userId: String): Flow<PrivateUser?> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(PrivateUser::class.java, ServerTimestampBehavior.ESTIMATE) }
        )
    }

    fun observePublicProfile(userId: String): Flow<PublicProfile?> = flow {
        requireUserId()
        emitAll(
            db.collection("public_profiles").document(userId)
                .snapshots()
                .map { it.toObject(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
        )
    }

    fun observeSubjects(userId: String): Flow<List<Subject>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("subjects")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Subject::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
        )
    }

    fun observeSessions(userId: String): Flow<List<StudySession>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("sessions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudySession::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.verifiedEndMillis }
                }
        )
    }

    fun observeTasks(userId: String): Flow<List<StudyTask>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyTask::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareBy<StudyTask> { it.completed }.thenBy { it.dueDateMillis })
                }
        )
    }

    fun observeNotes(userId: String): Flow<List<StudyNote>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("notes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudyNote::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(compareByDescending<StudyNote> { it.pinned }
                            .thenByDescending { it.updatedAt?.seconds ?: 0L })
                }
        )
    }

    fun observeAchievements(userId: String): Flow<List<UserAchievement>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("achievements")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserAchievement::class.java, ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<UserNotification>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> = flow {
        requireUserId()
        emitAll(
            db.collection("users").document(userId).collection("blocked")
                .snapshots()
                .map { snap -> snap.toObjects(BlockedUser::class.java, ServerTimestampBehavior.ESTIMATE) }
        )
    }

    suspend fun completeOnboarding(dailyGoalMinutes: Long, country: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val clampedGoal = dailyGoalMinutes.coerceIn(15L, 720L)
            val cleanCountry = country.trim().ifBlank { "Global" }.take(60)
            db.collection("users").document(uid).update(
                mapOf(
                    "dailyGoalMinutes" to clampedGoal,
                    "country" to cleanCountry,
                    "onboardingCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "dailyGoalMinutes" to clampedGoal,
                    "country" to cleanCountry,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String,
        username: String,
        bio: String,
        country: String,
        avatarEmoji: String,
        avatarColorHex: String,
        dailyGoalMinutes: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val cleanName = displayName.trim().take(60)
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").take(30)
            require(cleanName.isNotEmpty()) { "Display name cannot be empty." }
            require(cleanUsername.length >= 2) { "Username must be at least 2 characters." }
            val goal = dailyGoalMinutes.coerceIn(10L, 720L)
            val cleanCountry = country.trim().ifBlank { "Global" }.take(60)
            val cleanBio = bio.trim().take(240)

            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "username" to cleanUsername,
                    "country" to cleanCountry,
                    "dailyGoalMinutes" to goal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "displayName" to cleanName,
                    "username" to cleanUsername,
                    "bio" to cleanBio,
                    "country" to cleanCountry,
                    "avatarEmoji" to avatarEmoji.take(16),
                    "avatarColorHex" to avatarColorHex.take(16),
                    "dailyGoalMinutes" to goal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyAndPreferences(
        hideFromLeaderboard: Boolean? = null,
        publicProfile: Boolean? = null,
        notificationsMuted: Boolean? = null,
        themePreference: String? = null,
        fontPreference: String? = null,
        onlineStatus: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val userUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            val publicUpdates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (notificationsMuted != null) userUpdates["notificationsMuted"] = notificationsMuted
            if (themePreference != null) userUpdates["themePreference"] = themePreference
            if (fontPreference != null) userUpdates["fontPreference"] = fontPreference
            if (hideFromLeaderboard != null) publicUpdates["hideFromLeaderboard"] = hideFromLeaderboard
            if (publicProfile != null) publicUpdates["publicProfile"] = publicProfile
            if (onlineStatus != null) publicUpdates["onlineStatus"] = onlineStatus

            if (userUpdates.size > 1) {
                db.collection("users").document(uid).update(userUpdates).await()
            }
            if (publicUpdates.size > 1) {
                db.collection("public_profiles").document(uid).update(publicUpdates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun addSubject(
        name: String,
        iconKey: String,
        colorHex: String,
        description: String,
        targetMinutes: Long,
        planTier: String,
        currentCount: Int
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/subjects"
        return try {
            if (planTier != "PREMIUM" && currentCount >= 5) {
                return Result.failure(IllegalStateException("Free plan supports up to 5 subjects. Upgrade to Premium for unlimited subjects."))
            }
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Subject name is required." }
            val docRef = db.collection("users").document(uid).collection("subjects").document()
            val payload = mapOf(
                "userId" to uid,
                "name" to cleanName,
                "iconKey" to iconKey.ifBlank { "book" }.take(30),
                "colorHex" to colorHex.ifBlank { "#6C63FF" }.take(16),
                "description" to description.trim().take(300),
                "targetMinutes" to targetMinutes.coerceIn(30L, 100000L),
                "completedMinutes" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSubject(
        subjectId: String,
        name: String,
        iconKey: String,
        colorHex: String,
        description: String,
        targetMinutes: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/subjects/$subjectId"
        return try {
            val cleanName = name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Subject name cannot be empty." }
            db.document(path).update(
                mapOf(
                    "name" to cleanName,
                    "iconKey" to iconKey.take(30),
                    "colorHex" to colorHex.take(16),
                    "description" to description.trim().take(300),
                    "targetMinutes" to targetMinutes.coerceIn(30L, 100000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/subjects/$subjectId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordVerifiedStudySession(
        subjectId: String,
        subjectName: String,
        durationMinutes: Long,
        mode: String,
        notes: String,
        verifiedStartMillis: Long,
        verifiedEndMillis: Long,
        currentProfile: PublicProfile?,
        existingSessionsCount: Int,
        existingAchievements: List<UserAchievement>
    ): Result<Pair<Long, List<String>>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/sessions"
        return try {
            val elapsedMs = verifiedEndMillis - verifiedStartMillis
            val minRequiredMs = durationMinutes * 50_000L
            if (elapsedMs < minRequiredMs) {
                return Result.failure(IllegalStateException("Session duration could not be verified. Complete the focus timer naturally."))
            }
            val safeDuration = durationMinutes.coerceIn(1L, 240L)
            val baseXp = safeDuration * 5L
            val sessionRef = db.collection("users").document(uid).collection("sessions").document()
            val payload = mapOf(
                "userId" to uid,
                "subjectId" to subjectId.ifBlank { "general" },
                "subjectName" to subjectName.ifBlank { "General Study" }.take(60),
                "durationMinutes" to safeDuration,
                "mode" to mode,
                "notes" to notes.trim().take(500),
                "xpEarned" to baseXp,
                "verifiedStartMillis" to verifiedStartMillis,
                "verifiedEndMillis" to verifiedEndMillis,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            sessionRef.set(payload).await()

            if (subjectId.isNotBlank() && subjectId != "general") {
                db.collection("users").document(uid).collection("subjects").document(subjectId)
                    .update(
                        mapOf(
                            "completedMinutes" to FieldValue.increment(safeDuration),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }

            val oldTotalMins = currentProfile?.totalStudyMinutes ?: 0L
            val newTotalMins = oldTotalMins + safeDuration
            val newSessionCount = existingSessionsCount + 1
            val currentStreak = (currentProfile?.currentStreakDays ?: 0L).coerceAtLeast(1L)
            val longestStreak = maxOf(currentProfile?.longestStreakDays ?: 0L, currentStreak)

            val unlockedKeys = existingAchievements.map { it.key }.toSet()
            val newlyUnlocked = mutableListOf<String>()
            var bonusXp = 0L

            suspend fun tryUnlock(key: String) {
                if (key in unlockedKeys) return
                val spec = AchievementCatalog.allAchievements.firstOrNull { it.key == key } ?: return
                bonusXp += spec.xpReward
                newlyUnlocked += spec.title
                db.collection("users").document(uid).collection("achievements").document(key)
                    .set(
                        mapOf(
                            "userId" to uid,
                            "key" to spec.key,
                            "title" to spec.title,
                            "description" to spec.description,
                            "xpReward" to spec.xpReward,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }

            if (newSessionCount >= 1) tryUnlock("first_session")
            if (newSessionCount >= 10) tryUnlock("focus_master")
            if (newTotalMins >= 600L) tryUnlock("hours_10")
            if (newTotalMins >= 3000L) tryUnlock("hours_50")
            if (newTotalMins >= 6000L) tryUnlock("hours_100")
            if (currentStreak >= 7L) tryUnlock("streak_7")
            if (currentStreak >= 30L) tryUnlock("streak_30")

            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour < 8) tryUnlock("early_bird")
            if (hour >= 22) tryUnlock("night_owl")

            val totalEarnedXp = baseXp + bonusXp
            val newTotalXp = (currentProfile?.totalXp ?: 0L) + totalEarnedXp
            val newLevel = AchievementCatalog.levelForXp(newTotalXp)

            db.collection("public_profiles").document(uid).update(
                mapOf(
                    "totalXp" to newTotalXp,
                    "level" to newLevel,
                    "totalStudyMinutes" to newTotalMins,
                    "currentStreakDays" to currentStreak,
                    "longestStreakDays" to longestStreak,
                    "onlineStatus" to "ONLINE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (newlyUnlocked.isNotEmpty()) {
                createNotification(
                    uid,
                    "Achievement Unlocked!",
                    "You earned ${newlyUnlocked.joinToString(", ")} (+$bonusXp bonus XP).",
                    "ACHIEVEMENT"
                )
            }

            Result.success(totalEarnedXp to newlyUnlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addTask(
        title: String,
        description: String,
        subjectId: String,
        subjectName: String,
        priority: String,
        dueDateMillis: Long,
        reminderEnabled: Boolean,
        recurring: String,
        examFlag: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/tasks"
        return try {
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Task title is required." }
            val payload = mapOf(
                "userId" to uid,
                "subjectId" to subjectId.take(128),
                "subjectName" to subjectName.take(60),
                "title" to cleanTitle,
                "description" to description.trim().take(400),
                "priority" to priority,
                "dueDateMillis" to dueDateMillis.coerceAtLeast(1L),
                "reminderEnabled" to reminderEnabled,
                "recurring" to recurring,
                "examFlag" to examFlag,
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).collection("tasks").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompleted(taskId: String, completed: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/tasks/$taskId"
        return try {
            db.document(path).update(
                mapOf(
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/tasks/$taskId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNote(
        noteId: String?,
        title: String,
        content: String,
        subjectId: String,
        subjectName: String,
        pinned: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/notes"
        return try {
            val cleanTitle = title.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Note title cannot be empty." }
            val cleanContent = content.take(5000)
            if (noteId.isNullOrBlank()) {
                val payload = mapOf(
                    "userId" to uid,
                    "subjectId" to subjectId.take(128),
                    "subjectName" to subjectName.take(60),
                    "title" to cleanTitle,
                    "content" to cleanContent,
                    "pinned" to pinned,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("users").document(uid).collection("notes").document().set(payload).await()
            } else {
                db.collection("users").document(uid).collection("notes").document(noteId).update(
                    mapOf(
                        "subjectId" to subjectId.take(128),
                        "subjectName" to subjectName.take(60),
                        "title" to cleanTitle,
                        "content" to cleanContent,
                        "pinned" to pinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleNotePinned(noteId: String, pinned: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/notes/$noteId"
        return try {
            db.document(path).update(
                mapOf(
                    "pinned" to pinned,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(noteId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/notes/$noteId"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/notifications/$notificationId"
        return try {
            db.document(path).update(
                mapOf(
                    "hasRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createNotification(uid: String, title: String, body: String, category: String) {
        try {
            db.collection("users").document(uid).collection("notifications").document().set(
                mapOf(
                    "userId" to uid,
                    "title" to title.take(100),
                    "body" to body.take(300),
                    "category" to category,
                    "hasRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun blockUser(targetUid: String, targetName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            db.document(path).set(
                mapOf(
                    "userId" to uid,
                    "blockedUid" to targetUid,
                    "blockedName" to targetName.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUid: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid/blocked/$targetUid"
        return try {
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedStarterStudyKit(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val existingSubjects = db.collection("users").document(uid).collection("subjects").get().await()
            if (existingSubjects.isEmpty) {
                val starterSubjects = listOf(
                    Triple("Computer Science", "code", "#6C63FF") to "Algorithms, Data Structures & Systems",
                    Triple("Calculus & Linear Algebra", "calculate", "#14B8A6") to "Derivatives, Integrals & Matrices",
                    Triple("Organic Chemistry", "science", "#F59E0B") to "Reaction Mechanisms & Synthesis",
                    Triple("World Literature", "book", "#EC4899") to "Essays, Critical Reading & Rhetoric"
                )
                starterSubjects.forEach { (meta, desc) ->
                    db.collection("users").document(uid).collection("subjects").document().set(
                        mapOf(
                            "userId" to uid,
                            "name" to meta.first,
                            "iconKey" to meta.second,
                            "colorHex" to meta.third,
                            "description" to desc,
                            "targetMinutes" to 600L,
                            "completedMinutes" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val existingTasks = db.collection("users").document(uid).collection("tasks").get().await()
            if (existingTasks.isEmpty) {
                val now = System.currentTimeMillis()
                db.collection("users").document(uid).collection("tasks").document().set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to "",
                        "subjectName" to "Computer Science",
                        "title" to "Review Dynamic Programming Patterns",
                        "description" to "Practice knapsack & memoization problems",
                        "priority" to "HIGH",
                        "dueDateMillis" to now + 86_400_000L,
                        "reminderEnabled" to true,
                        "recurring" to "NONE",
                        "examFlag" to false,
                        "completed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("users").document(uid).collection("tasks").document().set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to "",
                        "subjectName" to "Calculus & Linear Algebra",
                        "title" to "Calculus Midterm Exam",
                        "description" to "Chapters 1-5 comprehensive exam",
                        "priority" to "HIGH",
                        "dueDateMillis" to now + 5 * 86_400_000L,
                        "reminderEnabled" to true,
                        "recurring" to "NONE",
                        "examFlag" to true,
                        "completed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val existingNotes = db.collection("users").document(uid).collection("notes").get().await()
            if (existingNotes.isEmpty) {
                db.collection("users").document(uid).collection("notes").document().set(
                    mapOf(
                        "userId" to uid,
                        "subjectId" to "",
                        "subjectName" to "Computer Science",
                        "title" to "Big-O Complexity Cheat Sheet",
                        "content" to "O(1) Hash lookup\nO(log n) Binary Search\nO(n log n) Merge Sort\nO(n^2) Nested Loops\n\nTip: Always analyze worst-case and space complexity.",
                        "pinned" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun deleteAccountData(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val subcollections = listOf("subjects", "sessions", "tasks", "notes", "achievements", "notifications", "blocked")
            for (sub in subcollections) {
                val snap = db.collection("users").document(uid).collection(sub).get().await()
                snap.documents.forEach { it.reference.delete().await() }
            }
            db.collection("public_profiles").document(uid).delete().await()
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }
}
