package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AchievementCatalog
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.UserAchievement
import com.example.data.model.UserNotification
import com.example.ui.components.AvatarBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.XpProgressBar
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileAndGamificationScreen(
    privateUser: PrivateUser?,
    profile: PublicProfile?,
    achievements: List<UserAchievement>,
    notifications: List<UserNotification>,
    friendsCount: Int,
    onMarkNotificationRead: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val authEmail = Firebase.auth.currentUser?.email
    val isAdminUser = privateUser?.role == "ADMIN" || authEmail == "muhinpetrol@gmail.com"
    val unlockedKeys = remember(achievements) { achievements.map { it.key }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cover + Avatar Header
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(BrandViolet, AccentLavender, AccentTeal)
                                )
                            )
                            .padding(14.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Row {
                            IconButton(
                                onClick = { showNotificationsDialog = true },
                                modifier = Modifier.testTag("profile_notifications_btn")
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = Color.White)
                            }
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("profile_settings_btn")
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarBadge(
                                emoji = profile?.avatarEmoji ?: "🦉",
                                colorHex = profile?.avatarColorHex ?: "#6C63FF",
                                size = 64.dp,
                                onlineStatus = profile?.onlineStatus
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile?.displayName?.ifBlank { "Scholar" } ?: "Scholar",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "@${profile?.username ?: "scholar"} • ${profile?.country ?: "Global"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "ID: ${(profile?.userId ?: "").take(10)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = profile?.bio?.ifBlank { "Building daily focus mastery." } ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(14.dp))
                        val totalXp = profile?.totalXp ?: 0L
                        val (inLvl, needed) = AchievementCatalog.xpProgressInLevel(totalXp)
                        XpProgressBar(
                            level = profile?.level ?: 1L,
                            currentInLevel = inLvl,
                            neededInLevel = needed,
                            totalXp = totalXp
                        )
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatPill("Study Time", formatDurationMinutes(profile?.totalStudyMinutes ?: 0L), Modifier.weight(1f))
                ProfileStatPill("Streak", "${profile?.currentStreakDays ?: 0} days", Modifier.weight(1f))
                ProfileStatPill("Friends", "$friendsCount", Modifier.weight(1f))
            }
        }

        // Action Cards: Premium, Settings, Admin
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPremium)
                    .testTag("profile_premium_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = BrandViolet)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Plan: ${privateUser?.planTier ?: "FREE"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("View Free vs Premium & Stripe checkout configuration", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (isAdminUser) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenAdmin)
                        .testTag("profile_admin_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.16f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = AccentAmber)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Admin Command Center", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Manage users, moderation reports, and announcements", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Achievements Showcase
        item {
            SectionHeader("Achievements (${unlockedKeys.size}/${AchievementCatalog.allAchievements.size})")
        }

        items(AchievementCatalog.allAchievements, key = { it.key }) { spec ->
            val isUnlocked = spec.key in unlockedKeys
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(spec.emoji, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(spec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isUnlocked) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Unlocked", tint = AccentTeal)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = "Locked", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(4.dp))
                            Text("+${spec.xpReward}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text("Notifications (${notifications.size})") },
            text = {
                if (notifications.isEmpty()) {
                    Text("No notifications yet.")
                } else {
                    LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (notif.hasRead) MaterialTheme.colorScheme.surfaceVariant else BrandViolet.copy(alpha = 0.14f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMarkNotificationRead(notif.id) }
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
                Button(onClick = { showNotificationsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun ProfileStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
