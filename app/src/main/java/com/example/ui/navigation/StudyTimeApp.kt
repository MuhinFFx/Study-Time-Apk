package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.AmbientSoundPlayer
import com.example.core.LocalAppPreferences
import com.example.core.PreferencesManager
import com.example.core.UiState
import com.example.data.model.UserNotification
import com.example.data.repository.AiStudyRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.StudyRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommunityAndChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.OnboardingSetupScreen
import com.example.ui.screens.PlannerAndNotesScreen
import com.example.ui.screens.PremiumPlansScreen
import com.example.ui.screens.ProfileAndGamificationScreen
import com.example.ui.screens.SettingsAndPrivacyScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.theme.BrandViolet
import com.example.ui.theme.StudyTimeTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StudyViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Routes {
    const val MAIN_SHELL = "main_shell"
    const val SUBJECTS = "subjects"
    const val PLANNER = "planner"
    const val ANALYTICS = "analytics"
    const val AI_COACH = "ai_coach"
    const val PREMIUM = "premium"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
}

private enum class MainTab(val label: String, val icon: ImageVector, val testTag: String) {
    HOME("Home", Icons.Filled.Home, "nav_home"),
    FOCUS("Focus", Icons.Filled.Timer, "nav_focus"),
    COMMUNITY("Social", Icons.Filled.Groups, "nav_community"),
    LEADERBOARD("Ranks", Icons.Filled.EmojiEvents, "nav_leaderboard"),
    PROFILE("Profile", Icons.Filled.Person, "nav_profile")
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun StudyTimeRoot() {
    val context = LocalContext.current.applicationContext
    val prefsManager = remember(context) { PreferencesManager(context) }
    val localPrefs by prefsManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = LocalAppPreferences())
    val currentUser by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    StudyTimeTheme(
        themeMode = localPrefs.themeMode,
        appFont = localPrefs.appFont
    ) {
        val user = currentUser
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                AuthScreen(onAuthSuccess = { /* authStateFlow switches to authenticated shell */ })
            }
        } else {
            key(user.uid) {
                AuthenticatedStudyTimeApp(user = user)
            }
        }
    }
}

@Composable
private fun AuthenticatedStudyTimeApp(user: FirebaseUser) {
    val studyViewModel: StudyViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                StudyViewModel(
                    studyRepo = StudyRepository(db),
                    socialRepo = SocialRepository(db),
                    aiRepo = AiStudyRepository(),
                    prefsManager = PreferencesManager(app),
                    soundPlayer = AmbientSoundPlayer(),
                    currentUserId = user.uid
                )
            }
        }
    )

    LaunchedEffect(user.uid) {
        studyViewModel.initializeAccount(user)
    }

    val preferences by studyViewModel.preferences.collectAsStateWithLifecycle()
    val privateUserState by studyViewModel.privateUser.collectAsStateWithLifecycle()
    val publicProfileState by studyViewModel.publicProfile.collectAsStateWithLifecycle()
    val subjectsState by studyViewModel.subjects.collectAsStateWithLifecycle()
    val sessionsState by studyViewModel.sessions.collectAsStateWithLifecycle()
    val tasksState by studyViewModel.tasks.collectAsStateWithLifecycle()
    val notesState by studyViewModel.notes.collectAsStateWithLifecycle()
    val achievementsState by studyViewModel.achievements.collectAsStateWithLifecycle()
    val notificationsState by studyViewModel.notifications.collectAsStateWithLifecycle()
    val blockedUsersState by studyViewModel.blockedUsers.collectAsStateWithLifecycle()
    val allProfilesState by studyViewModel.allPublicProfiles.collectAsStateWithLifecycle()
    val friendRequestsState by studyViewModel.friendRequests.collectAsStateWithLifecycle()
    val studyGroupsState by studyViewModel.studyGroups.collectAsStateWithLifecycle()
    val directChatsState by studyViewModel.directChats.collectAsStateWithLifecycle()
    val challengesState by studyViewModel.challenges.collectAsStateWithLifecycle()
    val announcementsState by studyViewModel.announcements.collectAsStateWithLifecycle()
    val timerState by studyViewModel.timerState.collectAsStateWithLifecycle()
    val aiState by studyViewModel.aiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(studyViewModel) {
        studyViewModel.snackMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val privateUser = (privateUserState as? UiState.Success)?.data
    val profile = (publicProfileState as? UiState.Success)?.data
    val subjects = (subjectsState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val achievements = (achievementsState as? UiState.Success)?.data.orEmpty()
    val notifications = (notificationsState as? UiState.Success)?.data.orEmpty()
    val blockedUsers = (blockedUsersState as? UiState.Success)?.data.orEmpty()
    val allProfiles = (allProfilesState as? UiState.Success)?.data.orEmpty()
    val friendRequests = (friendRequestsState as? UiState.Success)?.data.orEmpty()
    val studyGroups = (studyGroupsState as? UiState.Success)?.data.orEmpty()
    val directChats = (directChatsState as? UiState.Success)?.data.orEmpty()
    val challenges = (challengesState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()

    val planTier = privateUser?.planTier ?: "FREE"

    // Show onboarding if newly created account hasn't completed setup yet
    if (privateUserState is UiState.Loading && !preferences.onboardingSeen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = BrandViolet)
                Spacer(Modifier.height(12.dp))
                Text("Syncing your Study Time workspace...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (privateUser != null && !privateUser.onboardingCompleted && !preferences.onboardingSeen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            OnboardingSetupScreen(
                displayName = user.displayName ?: privateUser.displayName,
                onComplete = { goalMins, country, seedDemo ->
                    studyViewModel.completeOnboarding(goalMins, country, seedDemo)
                }
            )
        }
        return
    }

    val navController = rememberNavController()
    var activeTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showNotificationsModal by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN_SHELL
        ) {
            composable(Routes.MAIN_SHELL) {
                val activeTab = MainTab.entries.getOrElse(activeTabIndex) { MainTab.HOME }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 600.dp

                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (!useRail) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                    MainTab.entries.forEachIndexed { index, tab ->
                                        NavigationBarItem(
                                            selected = activeTab == tab,
                                            onClick = { activeTabIndex = index },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (useRail) {
                                NavigationRail(
                                    modifier = Modifier.fillMaxHeight(),
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    Spacer(Modifier.height(12.dp))
                                    MainTab.entries.forEachIndexed { index, tab ->
                                        NavigationRailItem(
                                            selected = activeTab == tab,
                                            onClick = { activeTabIndex = index },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) },
                                            modifier = Modifier.testTag(tab.testTag)
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .widthIn(max = 920.dp)
                                        .fillMaxSize()
                                ) {
                                    when (activeTab) {
                                        MainTab.HOME -> DashboardScreen(
                                            profile = profile,
                                            subjects = subjects,
                                            sessions = sessions,
                                            tasks = tasks,
                                            achievements = achievements,
                                            notifications = notifications,
                                            announcements = announcements,
                                            allProfiles = allProfiles,
                                            friendRequests = friendRequests,
                                            onStartFocus = { activeTabIndex = MainTab.FOCUS.ordinal },
                                            onOpenSubjects = { navController.navigate(Routes.SUBJECTS) },
                                            onOpenPlanner = { navController.navigate(Routes.PLANNER) },
                                            onOpenAnalytics = { navController.navigate(Routes.ANALYTICS) },
                                            onOpenAiCoach = { navController.navigate(Routes.AI_COACH) },
                                            onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                            onOpenNotifications = { showNotificationsModal = true },
                                            onToggleTask = studyViewModel::toggleTaskCompleted,
                                            onSeedDemo = studyViewModel::seedStarterData
                                        )

                                        MainTab.FOCUS -> FocusTimerScreen(
                                            timerState = timerState,
                                            preferences = preferences,
                                            subjects = subjects,
                                            sessions = sessions,
                                            onSelectPreset = studyViewModel::selectTimerPreset,
                                            onSelectSubject = studyViewModel::selectTimerSubject,
                                            onUpdateNotes = studyViewModel::updateSessionNotes,
                                            onStartOrResume = studyViewModel::startOrResumeTimer,
                                            onPause = studyViewModel::pauseTimer,
                                            onReset = studyViewModel::resetTimer,
                                            onSwitchPhase = studyViewModel::switchBreakPhase,
                                            onSelectSound = studyViewModel::setAmbientSound,
                                            onToggleChime = studyViewModel::setChimeEnabled,
                                            onDismissReward = studyViewModel::dismissRewardBanner
                                        )

                                        MainTab.COMMUNITY -> CommunityAndChatScreen(
                                            currentUserId = user.uid,
                                            myProfile = profile,
                                            allProfiles = allProfiles,
                                            friendRequests = friendRequests,
                                            directChats = directChats,
                                            studyGroups = studyGroups,
                                            challenges = challenges,
                                            blockedUsers = blockedUsers,
                                            observeChatMessages = studyViewModel::observeChatMessages,
                                            observeGroupMessages = studyViewModel::observeGroupMessages,
                                            onSendFriendRequest = studyViewModel::sendFriendRequest,
                                            onRespondFriendRequest = studyViewModel::respondFriendRequest,
                                            onRemoveFriend = studyViewModel::removeFriend,
                                            onOpenDirectChat = studyViewModel::openDirectChat,
                                            onSendDirectMessage = studyViewModel::sendDirectMessage,
                                            onReactDirectMessage = studyViewModel::reactToDirectMessage,
                                            onDeleteDirectMessage = studyViewModel::deleteDirectMessage,
                                            onToggleChatMuted = studyViewModel::toggleChatMuted,
                                            onCreateGroup = studyViewModel::createStudyGroup,
                                            onJoinGroup = studyViewModel::joinStudyGroup,
                                            onLeaveGroup = studyViewModel::leaveStudyGroup,
                                            onSendGroupMessage = studyViewModel::sendGroupMessage,
                                            onReactGroupMessage = studyViewModel::reactToGroupMessage,
                                            onCreateChallenge = studyViewModel::createChallenge,
                                            onJoinChallenge = studyViewModel::joinChallenge,
                                            onBlockAndReport = studyViewModel::blockAndReportUser
                                        )

                                        MainTab.LEADERBOARD -> LeaderboardScreen(
                                            currentUserId = user.uid,
                                            myProfile = profile,
                                            allProfiles = allProfiles,
                                            friendRequests = friendRequests,
                                            studyGroups = studyGroups,
                                            onToggleHideLeaderboard = { hidden ->
                                                studyViewModel.updatePrivacy(
                                                    hideFromLeaderboard = hidden,
                                                    publicProfile = profile?.publicProfile ?: true,
                                                    notificationsMuted = privateUser?.notificationsMuted ?: false
                                                )
                                            }
                                        )

                                        MainTab.PROFILE -> ProfileAndGamificationScreen(
                                            privateUser = privateUser,
                                            profile = profile,
                                            achievements = achievements,
                                            notifications = notifications,
                                            friendsCount = friendRequests.count { it.status == "ACCEPTED" },
                                            onMarkNotificationRead = studyViewModel::markNotificationRead,
                                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                                            onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                                            onOpenAdmin = { navController.navigate(Routes.ADMIN) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            composable(Routes.SUBJECTS) {
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    SubjectsScreen(
                        subjects = subjects,
                        sessions = sessions,
                        planTier = planTier,
                        onAddSubject = studyViewModel::addSubject,
                        onUpdateSubject = studyViewModel::updateSubject,
                        onDeleteSubject = studyViewModel::deleteSubject,
                        onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.PLANNER) {
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    PlannerAndNotesScreen(
                        subjects = subjects,
                        tasks = tasks,
                        notes = notes,
                        onAddTask = studyViewModel::addTask,
                        onToggleTask = studyViewModel::toggleTaskCompleted,
                        onDeleteTask = studyViewModel::deleteTask,
                        onSaveNote = studyViewModel::saveNote,
                        onToggleNotePinned = studyViewModel::toggleNotePinned,
                        onDeleteNote = studyViewModel::deleteNote,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.ANALYTICS) {
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    AnalyticsScreen(
                        profile = profile,
                        subjects = subjects,
                        sessions = sessions,
                        tasks = tasks,
                        planTier = planTier,
                        onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                        onExportStatus = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.AI_COACH) {
                val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val usedToday = if (preferences.aiRequestsDayKey == dayKey) preferences.aiRequestsToday else 0
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    AiAssistantScreen(
                        aiState = aiState,
                        planTier = planTier,
                        usedToday = usedToday,
                        onAsk = studyViewModel::askAiCoach,
                        onGeneratePlan = studyViewModel::generateStructuredPlan,
                        onAddTaskToPlanner = studyViewModel::applyAiTaskToPlanner,
                        onOpenPremium = { navController.navigate(Routes.PREMIUM) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.PREMIUM) {
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    PremiumPlansScreen(
                        currentPlanTier = planTier,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.SETTINGS) {
                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    SettingsAndPrivacyScreen(
                        privateUser = privateUser,
                        profile = profile,
                        subjects = subjects,
                        sessions = sessions,
                        blockedUsers = blockedUsers,
                        preferences = preferences,
                        onUpdateProfile = studyViewModel::updateProfile,
                        onSetThemeMode = studyViewModel::setThemeMode,
                        onSetAppFont = studyViewModel::setAppFont,
                        onUpdatePrivacy = studyViewModel::updatePrivacy,
                        onUnblockUser = studyViewModel::unblockUser,
                        onExportStatus = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                        onDeleteAccountData = studyViewModel::deleteAccountData,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(Routes.ADMIN) {
                val adminViewModel: AdminViewModel = viewModel(
                    key = "admin_${user.uid}",
                    factory = viewModelFactory {
                        initializer {
                            val app = this[APPLICATION_KEY] as Application
                            val dbId = app.getString(R.string.firestore_database_id)
                            AdminViewModel(SocialRepository(FirebaseFirestore.getInstance(dbId)))
                        }
                    }
                )
                LaunchedEffect(adminViewModel) {
                    adminViewModel.events.collect { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }
                val adminProfiles by adminViewModel.allProfiles.collectAsStateWithLifecycle()
                val adminReports by adminViewModel.reports.collectAsStateWithLifecycle()
                val adminAnnouncements by adminViewModel.announcements.collectAsStateWithLifecycle()

                SecondaryScreenContainer(snackbarHostState = snackbarHostState) {
                    AdminDashboardScreen(
                        profilesState = adminProfiles,
                        reportsState = adminReports,
                        announcementsState = adminAnnouncements,
                        onToggleSuspend = adminViewModel::toggleUserSuspended,
                        onResolveReport = adminViewModel::resolveReport,
                        onPublishAnnouncement = adminViewModel::publishAnnouncement,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    if (showNotificationsModal) {
        NotificationsInboxDialog(
            notifications = notifications,
            onMarkRead = studyViewModel::markNotificationRead,
            onDismiss = { showNotificationsModal = false }
        )
    }
}

@Composable
private fun SecondaryScreenContainer(
    snackbarHostState: SnackbarHostState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 920.dp)
                .fillMaxSize()
        ) {
            content()
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        )
    }
}

@Composable
private fun NotificationsInboxDialog(
    notifications: List<UserNotification>,
    onMarkRead: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifications (${notifications.size})") },
        text = {
            if (notifications.isEmpty()) {
                Text("You're all caught up! Focus milestones, friend requests, and announcements appear here.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (notif.hasRead) MaterialTheme.colorScheme.surfaceVariant
                            else BrandViolet.copy(alpha = 0.14f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMarkRead(notif.id) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(notif.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text(notif.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}
