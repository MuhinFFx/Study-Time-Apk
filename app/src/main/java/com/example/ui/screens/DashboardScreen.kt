package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AchievementCatalog
import com.example.data.model.Announcement
import com.example.data.model.FriendRequest
import com.example.data.model.PublicProfile
import com.example.data.model.StudySession
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.data.model.UserAchievement
import com.example.data.model.UserNotification
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GradientBannerCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.XpProgressBar
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.parseColorSafe
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    profile: PublicProfile?,
    subjects: List<Subject>,
    sessions: List<StudySession>,
    tasks: List<StudyTask>,
    achievements: List<UserAchievement>,
    notifications: List<UserNotification>,
    announcements: List<Announcement>,
    allProfiles: List<PublicProfile>,
    friendRequests: List<FriendRequest>,
    onStartFocus: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenAiCoach: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenNotifications: () -> Unit,
    onToggleTask: (String, Boolean) -> Unit,
    onSeedDemo: () -> Unit
) {
    val todayMinutes = remember(sessions) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        sessions.filter { it.verifiedEndMillis >= startOfDay }.sumOf { it.durationMinutes }
    }

    val dailyGoal = (profile?.dailyGoalMinutes ?: 120L).coerceAtLeast(10L)
    val goalProgress = (todayMinutes.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f)
    val unreadNotifs = notifications.count { !it.hasRead }

    val globalRank = remember(allProfiles, profile) {
        val uid = profile?.userId ?: return@remember null
        val ranked = allProfiles.filter { !it.hideFromLeaderboard && !it.suspended }
            .sortedByDescending { it.totalXp }
        val idx = ranked.indexOfFirst { it.userId == uid }
        if (idx >= 0) idx + 1 else null
    }

    val weeklySeries = remember(sessions) {
        val result = MutableList(7) { 0L }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = cal.timeInMillis
        sessions.forEach { sess ->
            val diffDays = TimeUnit.MILLISECONDS.toDays(todayStart - sess.verifiedEndMillis)
            if (sess.verifiedEndMillis >= todayStart) {
                result[6] += sess.durationMinutes
            } else if (diffDays in 0..5) {
                val idx = (5 - diffDays).toInt()
                if (idx in 0..6) result[idx] += sess.durationMinutes
            }
        }
        result
    }

    val friendUids = remember(friendRequests, profile) {
        val myUid = profile?.userId.orEmpty()
        friendRequests.filter { it.status == "ACCEPTED" }
            .mapNotNull { req ->
                when (myUid) {
                    req.fromUid -> req.toUid
                    req.toUid -> req.fromUid
                    else -> null
                }
            }.toSet()
    }
    val friendsActivity = remember(allProfiles, friendUids) {
        allProfiles.filter { it.userId in friendUids }
    }

    val openTasks = remember(tasks) { tasks.filter { !it.completed && !it.examFlag } }
    val upcomingExams = remember(tasks) { tasks.filter { !it.completed && it.examFlag } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Header + Notification Bell
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(
                        emoji = profile?.avatarEmoji ?: "🦉",
                        colorHex = profile?.avatarColorHex ?: "#6C63FF",
                        size = 48.dp,
                        onlineStatus = profile?.onlineStatus ?: "ONLINE"
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = greetingForHour(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profile?.displayName?.ifBlank { "Scholar" } ?: "Scholar",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AccentAmber.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${profile?.currentStreakDays ?: 0}d",
                                style = MaterialTheme.typography.labelLarge,
                                color = AccentAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("dashboard_notifications_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge { Text(unreadNotifs.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            }
        }

        // Hero Banner Card with Daily Progress Ring + Quick Start Timer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_study_banner),
                    contentDescription = "Study workspace banner",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xE61A1642),
                                    Color(0xBF372FB2)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (globalRank != null) "Global Rank #$globalRank • Lv.${profile?.level ?: 1}" else "Level ${profile?.level ?: 1} Scholar",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = formatDurationMinutes(todayMinutes),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color.White
                            )
                            Text(
                                text = "of ${formatDurationMinutes(dailyGoal)} daily goal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }

                        Box(
                            modifier = Modifier.size(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 9.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.22f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = AccentTeal,
                                    startAngle = -90f,
                                    sweepAngle = goalProgress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "${(goalProgress * 100).toInt()}%",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onStartFocus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dashboard_quick_start_timer"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandViolet
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Start Quick Focus Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // XP Level Bar
        item {
            val totalXp = profile?.totalXp ?: 0L
            val (inLevel, needed) = AchievementCatalog.xpProgressInLevel(totalXp)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    XpProgressBar(
                        level = profile?.level ?: 1L,
                        currentInLevel = inLevel,
                        neededInLevel = needed,
                        totalXp = totalXp
                    )
                }
            }
        }

        // Announcements
        if (announcements.any { it.active }) {
            val topAnn = announcements.first { it.active }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = "Announcement", tint = BrandViolet)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(topAnn.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text(topAnn.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Quick Workspace Tools
        item {
            SectionHeader("Study Workspace")
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    QuickToolTile(Icons.Filled.Book, "Subjects", "${subjects.size} active", BrandViolet, "tool_subjects", onOpenSubjects)
                }
                item {
                    QuickToolTile(Icons.Filled.CalendarMonth, "Planner & Notes", "${openTasks.size} tasks", AccentTeal, "tool_planner", onOpenPlanner)
                }
                item {
                    QuickToolTile(Icons.Filled.Analytics, "Analytics", "Reports", AccentAmber, "tool_analytics", onOpenAnalytics)
                }
                item {
                    QuickToolTile(Icons.Filled.AutoAwesome, "AI Coach", "Gemini", AccentLavender, "tool_ai", onOpenAiCoach)
                }
            }
        }

        // Weekly Study Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionHeader("Weekly Focus Trend", "Analytics", onOpenAnalytics)
                    Spacer(Modifier.height(10.dp))
                    WeeklyBarChart(minutesPerDay = weeklySeries)
                }
            }
        }

        // Upcoming Exams
        if (upcomingExams.isNotEmpty()) {
            item {
                SectionHeader("Upcoming Exams", "Planner", onOpenPlanner)
                upcomingExams.take(3).forEach { exam ->
                    val daysLeft = ((exam.dueDateMillis - System.currentTimeMillis()) / 86_400_000L).coerceAtLeast(0L)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exam.title, style = MaterialTheme.typography.titleMedium)
                                Text(exam.subjectName.ifBlank { "General" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = if (daysLeft == 0L) "Today" else "${daysLeft}d left",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Today's Tasks
        item {
            SectionHeader("Today's Tasks", "View All", onOpenPlanner)
            if (openTasks.isEmpty()) {
                EmptyStateCard(
                    title = "No pending tasks",
                    subtitle = if (subjects.isEmpty()) "Load the Starter Study Kit or add tasks in the Planner." else "You're all caught up! Add your next study goal in Planner.",
                    actionText = if (subjects.isEmpty()) "Load Starter Kit" else "Open Planner",
                    onAction = if (subjects.isEmpty()) onSeedDemo else onOpenPlanner
                )
            } else {
                openTasks.take(4).forEach { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = task.completed,
                                onCheckedChange = { onToggleTask(task.id, it) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    text = "${task.subjectName.ifBlank { "General" }} • ${task.priority}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subjects Overview
        if (subjects.isNotEmpty()) {
            item {
                SectionHeader("Your Subjects", "Manage", onOpenSubjects)
                subjects.take(3).forEach { subject ->
                    val pct = if (subject.targetMinutes > 0) (subject.completedMinutes.toFloat() / subject.targetMinutes.toFloat()).coerceIn(0f, 1f) else 0f
                    val subjColor = parseColorSafe(subject.colorHex)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(onClick = onOpenSubjects),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(subject.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${formatDurationMinutes(subject.completedMinutes)} / ${formatDurationMinutes(subject.targetMinutes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = subjColor
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = subjColor
                            )
                        }
                    }
                }
            }
        }

        // Recent Achievements & Friends Activity
        item {
            SectionHeader("Recent Achievements")
            if (achievements.isEmpty()) {
                Text(
                    "Complete a focus session to unlock your first badge!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(achievements, key = { it.id }) { ach ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.14f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AccentAmber)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(ach.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text("+${ach.xpReward} XP", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (friendsActivity.isNotEmpty()) {
            item {
                SectionHeader("Friends Activity")
                friendsActivity.take(4).forEach { friend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBadge(friend.avatarEmoji, friend.avatarColorHex, 38.dp, friend.onlineStatus)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.displayName, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "${friend.onlineStatus.lowercase().replaceFirstChar { it.uppercase() }} • Lv.${friend.level} (${formatDurationMinutes(friend.totalStudyMinutes)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Premium Upgrade Card
        if ((profile?.planTier ?: "FREE") != "PREMIUM") {
            item {
                GradientBannerCard(
                    modifier = Modifier
                        .clickable(onClick = onOpenPremium)
                        .testTag("dashboard_premium_card")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = "Premium",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Unlock Study Time Premium",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Unlimited subjects, PDF/CSV exports, unlimited AI Coach & exclusive badges.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickToolTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeeklyBarChart(minutesPerDay: List<Long>) {
    val maxVal = (minutesPerDay.maxOrNull() ?: 60L).coerceAtLeast(30L).toFloat()
    val labels = listOf("-6d", "-5d", "-4d", "-3d", "-2d", "Yest", "Today")
    val barColor = BrandViolet
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = minutesPerDay.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = slotWidth * 0.48f
            minutesPerDay.forEachIndexed { idx, mins ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val barHeight = (mins / maxVal) * size.height
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = if (idx == count - 1) AccentTeal else barColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEachIndexed { idx, lbl ->
                Text(
                    text = "$lbl\n${minutesPerDay.getOrElse(idx) { 0L }}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun greetingForHour(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}
