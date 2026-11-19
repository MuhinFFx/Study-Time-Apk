package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.core.AppFont
import com.example.core.ExportHelper
import com.example.core.LocalAppPreferences
import com.example.core.ThemeMode
import com.example.core.UiState
import com.example.data.model.Announcement
import com.example.data.model.BlockedUser
import com.example.data.model.ModerationReport
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.StudySession
import com.example.data.model.Subject
import com.example.ui.components.GradientBannerCard
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightOutline
import com.example.ui.theme.toFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPlansScreen(
    currentPlanTier: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var billingCycle by remember { mutableStateOf("YEARLY") }
    var showPaymentConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium & Billing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                GradientBannerCard {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Study Time Premium", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Current Status: $currentPlanTier",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = billingCycle == "MONTHLY", onClick = { billingCycle = "MONTHLY" }, label = { Text("Monthly ($4.99/mo)") })
                    FilterChip(selected = billingCycle == "YEARLY", onClick = { billingCycle = "YEARLY" }, label = { Text("Yearly ($39.99/yr • Save 33%)") })
                }
            }

            item {
                PlanTierCard(
                    title = "Free Scholar",
                    price = "$0 / forever",
                    features = listOf(
                        "Pomodoro 25/5, 50/10 & Custom timer",
                        "Up to 5 subjects",
                        "Study planner, tasks & notes",
                        "Friends, chats, groups & leaderboard",
                        "5 Gemini AI Coach prompts/day"
                    ),
                    highlighted = currentPlanTier == "FREE"
                )
            }

            item {
                PlanTierCard(
                    title = "Premium Scholar",
                    price = if (billingCycle == "MONTHLY") "$4.99 / month" else "$39.99 / year",
                    features = listOf(
                        "Unlimited subjects & study groups",
                        "Unlimited Gemini AI Coach & smart plans",
                        "PDF & CSV analytics report exports",
                        "Exclusive Premium badges & custom themes",
                        "7-day free trial via verified checkout"
                    ),
                    highlighted = currentPlanTier == "PREMIUM"
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = AccentAmber)
                            Spacer(Modifier.width(8.dp))
                            Text("Payment Gateway Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Live billing requires connecting your Stripe Publishable Key (STRIPE_PUBLISHABLE_KEY in .env) and server-side webhook verification before activating Premium entitlements.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showPaymentConfigDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("premium_checkout_config_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                        ) {
                            Text("Configure Payment Gateway / Checkout", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showPaymentConfigDialog) {
        AlertDialog(
            onDismissRequest = { showPaymentConfigDialog = false },
            title = { Text("Stripe Checkout Configuration") },
            text = {
                Text(
                    "Study Time never simulates fake card charges. To enable real subscriptions:\n\n" +
                        "1. Add STRIPE_PUBLISHABLE_KEY in AI Studio Secrets.\n" +
                        "2. Deploy a backend webhook endpoint that verifies Stripe checkout.session.completed events.\n" +
                        "3. Update /users/{uid}.planTier and /public_profiles/{uid}.planTier from your verified server webhook."
                )
            },
            confirmButton = {
                Button(onClick = { showPaymentConfigDialog = false }) { Text("Understood") }
            }
        )
    }
}

@Composable
private fun PlanTierCard(
    title: String,
    price: String,
    features: List<String>,
    highlighted: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(price, style = MaterialTheme.typography.titleMedium, color = BrandViolet, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            features.forEach { feat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = AccentTeal)
                    Spacer(Modifier.width(8.dp))
                    Text(feat, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndPrivacyScreen(
    privateUser: PrivateUser?,
    profile: PublicProfile?,
    subjects: List<Subject>,
    sessions: List<StudySession>,
    blockedUsers: List<BlockedUser>,
    preferences: LocalAppPreferences,
    onUpdateProfile: (String, String, String, String, String, String, Long) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAppFont: (AppFont) -> Unit,
    onUpdatePrivacy: (Boolean, Boolean, Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onExportStatus: (String) -> Unit,
    onDeleteAccountData: (() -> Unit) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var username by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var bio by remember(profile) { mutableStateOf(profile?.bio ?: "") }
    var country by remember(profile) { mutableStateOf(profile?.country ?: "Global") }
    var avatarEmoji by remember(profile) { mutableStateOf(profile?.avatarEmoji ?: "🦉") }
    var avatarColorHex by remember(profile) { mutableStateOf(profile?.avatarColorHex ?: "#6C63FF") }
    var goalMins by remember(profile) { mutableStateOf((profile?.dailyGoalMinutes ?: 120L).toString()) }

    var hideFromLb by remember(profile) { mutableStateOf(profile?.hideFromLeaderboard ?: false) }
    var isPublic by remember(profile) { mutableStateOf(profile?.publicProfile ?: true) }
    var muteNotifs by remember(privateUser) { mutableStateOf(privateUser?.notificationsMuted ?: false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Edit Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, maxLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = goalMins, onValueChange = { goalMins = it.filter(Char::isDigit) }, label = { Text("Daily Goal (Minutes)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                        Text("Avatar Badge", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("🦉", "🚀", "🎓", "⚡", "🧠", "🌿", "🔥", "📚")) { emoji ->
                                FilterChip(selected = avatarEmoji == emoji, onClick = { avatarEmoji = emoji }, label = { Text(emoji) })
                            }
                        }

                        Button(
                            onClick = {
                                onUpdateProfile(
                                    displayName,
                                    username,
                                    bio,
                                    country,
                                    avatarEmoji,
                                    avatarColorHex,
                                    goalMins.toLongOrNull() ?: 120L
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("settings_save_profile_btn")
                        ) { Text("Save Profile") }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}"),
                                    leadingIcon = {
                                        when (mode) {
                                            ThemeMode.GRADIENT -> Image(
                                                painter = painterResource(id = R.drawable.img_theme_gradient_bg),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            )
                                            ThemeMode.LIGHT -> Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(LightBackground)
                                                    .border(1.dp, LightOutline, CircleShape)
                                            )
                                            ThemeMode.DARK -> Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(DarkBackground)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                            )
                                            ThemeMode.SYSTEM -> Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandViolet)
                                            )
                                        }
                                    },
                                    label = { Text(mode.label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Font", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppFont.entries.forEach { font ->
                                val chipFamily = font.toFontFamily()
                                FilterChip(
                                    selected = preferences.appFont == font,
                                    onClick = { onSetAppFont(font) },
                                    modifier = Modifier.testTag("font_chip_${font.name.lowercase()}"),
                                    leadingIcon = {
                                        Text(
                                            text = "Aa",
                                            fontFamily = chipFamily,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = font.label,
                                            fontFamily = chipFamily
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = "Preview: Study smarter, stay consistent every day.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = preferences.appFont.toFontFamily(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Privacy & Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Public profile discovery")
                            Switch(checked = isPublic, onCheckedChange = {
                                isPublic = it
                                onUpdatePrivacy(hideFromLb, isPublic, muteNotifs)
                            })
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Hide from public leaderboards")
                            Switch(checked = hideFromLb, onCheckedChange = {
                                hideFromLb = it
                                onUpdatePrivacy(hideFromLb, isPublic, muteNotifs)
                            })
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text("Mute notifications")
                            Switch(checked = muteNotifs, onCheckedChange = {
                                muteNotifs = it
                                onUpdatePrivacy(hideFromLb, isPublic, muteNotifs)
                            })
                        }
                    }
                }
            }

            if (blockedUsers.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Blocked Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            blockedUsers.forEach { blk ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(blk.blockedName)
                                    TextButton(onClick = { onUnblockUser(blk.blockedUid) }) { Text("Unblock") }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        val csv = ExportHelper.buildSessionsCsv(profile, subjects, sessions)
                        val file = ExportHelper.writeCsvFile(context, csv)
                        onExportStatus("Exported personal data to ${file.name}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export My Study Data (CSV)")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRose)
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Data")
                    }

                    Button(
                        onClick = {
                            signOutUser(credentialManager, scope)
                        },
                        modifier = Modifier.weight(1f).testTag("settings_logout_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandViolet)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = Color.White)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Study Data?") },
            text = { Text("This permanently deletes your subjects, sessions, tasks, notes, and profile from Firestore and signs you out.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountData {
                            signOutUser(credentialManager, scope)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRose)
                ) { Text("Delete Forever", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    profilesState: UiState<List<PublicProfile>>,
    reportsState: UiState<List<ModerationReport>>,
    announcementsState: UiState<List<Announcement>>,
    onToggleSuspend: (String, Boolean) -> Unit,
    onResolveReport: (String) -> Unit,
    onPublishAnnouncement: (String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var tab by remember { mutableIntStateOf(0) }
    var annTitle by remember { mutableStateOf("") }
    var annMessage by remember { mutableStateOf("") }

    val profiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Overview") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Users (${profiles.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Reports (${reports.count { it.status == "OPEN" }})") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Announce") })
            }

            when (tab) {
                0 -> {
                    val totalStudyMins = profiles.sumOf { it.totalStudyMinutes }
                    val premiumCount = profiles.count { it.planTier == "PREMIUM" }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Platform Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Total Registered Users: ${profiles.size}")
                                    Text("Premium Users: $premiumCount")
                                    Text("Cumulative Study Time: ${formatDurationMinutes(totalStudyMins)}")
                                    Text("Open Moderation Reports: ${reports.count { it.status == "OPEN" }}")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(profiles, key = { it.userId }) { user ->
                            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${user.displayName} (@${user.username})", fontWeight = FontWeight.Bold)
                                        Text("${user.planTier} • ${user.totalXp} XP • ${if (user.suspended) "SUSPENDED" else "ACTIVE"}")
                                    }
                                    OutlinedButton(onClick = { onToggleSuspend(user.userId, !user.suspended) }) {
                                        Text(if (user.suspended) "Unsuspend" else "Suspend")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (reports.isEmpty()) {
                            item { Text("No moderation reports submitted.", style = MaterialTheme.typography.bodyMedium) }
                        } else {
                            items(reports, key = { it.id }) { r ->
                                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Target: ${r.targetUid.take(10)} • ${r.status}", fontWeight = FontWeight.Bold)
                                            Text(r.reason)
                                        }
                                        if (r.status == "OPEN") {
                                            Button(onClick = { onResolveReport(r.id) }) { Text("Resolve") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Publish Platform Announcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(value = annTitle, onValueChange = { annTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = annMessage, onValueChange = { annMessage = it }, label = { Text("Message") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            onPublishAnnouncement(annTitle, annMessage)
                                            annTitle = ""
                                            annMessage = ""
                                        },
                                        enabled = annTitle.trim().length >= 2 && annMessage.trim().length >= 2,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Publish Announcement") }
                                }
                            }
                        }
                        items(announcements, key = { it.id }) { ann ->
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text(ann.title, fontWeight = FontWeight.Bold)
                                    Text(ann.message)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
