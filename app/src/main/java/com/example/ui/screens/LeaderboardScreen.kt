package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FriendRequest
import com.example.data.model.PublicProfile
import com.example.data.model.StudyGroup
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.BrandViolet
import com.example.ui.theme.BronzeMedal
import com.example.ui.theme.GoldMedal
import com.example.ui.theme.SilverMedal

@Composable
fun LeaderboardScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    allProfiles: List<PublicProfile>,
    friendRequests: List<FriendRequest>,
    studyGroups: List<StudyGroup>,
    onToggleHideLeaderboard: (Boolean) -> Unit
) {
    var scope by remember { mutableStateOf("GLOBAL") }

    val friendUids = remember(friendRequests, currentUserId) {
        friendRequests.filter { it.status == "ACCEPTED" }
            .mapNotNull { if (it.fromUid == currentUserId) it.toUid else it.fromUid }
            .toSet() + currentUserId
    }

    val groupUids = remember(studyGroups, currentUserId) {
        studyGroups.filter { currentUserId in it.memberUids }
            .flatMap { it.memberUids }
            .toSet() + currentUserId
    }

    val ranked = remember(allProfiles, scope, myProfile, friendUids, groupUids) {
        allProfiles
            .filter { !it.hideFromLeaderboard && !it.suspended }
            .filter { prof ->
                when (scope) {
                    "COUNTRY" -> prof.country.equals(myProfile?.country ?: "Global", ignoreCase = true)
                    "FRIENDS" -> prof.userId in friendUids
                    "GROUP" -> prof.userId in groupUids
                    else -> true
                }
            }
            .sortedByDescending { it.totalXp }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Leaderboard", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Verified XP & study hours • Fair anti-cheat rankings",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("GLOBAL" to "Global", "COUNTRY" to "Country", "FRIENDS" to "Friends", "GROUP" to "Groups").forEach { (k, lbl) ->
                    FilterChip(
                        selected = scope == k,
                        onClick = { scope = k },
                        label = { Text(lbl) },
                        modifier = Modifier.testTag("lb_filter_$k")
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hide my profile from public rankings", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = myProfile?.hideFromLeaderboard ?: false,
                        onCheckedChange = onToggleHideLeaderboard
                    )
                }
            }
        }

        if (ranked.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No ranked scholars yet",
                    subtitle = "Complete a verified study session to claim #1 on this leaderboard!"
                )
            }
        } else {
            // Top 3 Podium
            item {
                PodiumCard(topThree = ranked.take(3))
            }

            itemsIndexed(ranked, key = { _, item -> item.userId }) { idx, user ->
                val isMe = user.userId == currentUserId
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) BrandViolet.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(38.dp),
                            color = when (idx) {
                                0 -> GoldMedal
                                1 -> SilverMedal
                                2 -> BronzeMedal
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        AvatarBadge(user.avatarEmoji, user.avatarColorHex, 42.dp, user.onlineStatus)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isMe) "${user.displayName} (You)" else user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lv.${user.level} • ${formatDurationMinutes(user.totalStudyMinutes)} • ${user.country}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${user.totalXp} XP",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandViolet,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumCard(topThree: List<PublicProfile>) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(BrandViolet.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                val second = topThree.getOrNull(1)
                val first = topThree.getOrNull(0)
                val third = topThree.getOrNull(2)

                if (second != null) PodiumColumn(second, 2, SilverMedal, 72)
                if (first != null) PodiumColumn(first, 1, GoldMedal, 96)
                if (third != null) PodiumColumn(third, 3, BronzeMedal, 58)
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    user: PublicProfile,
    rank: Int,
    medalColor: Color,
    barHeightDp: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        if (rank == 1) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = "1st place", tint = GoldMedal, modifier = Modifier.size(26.dp))
        }
        AvatarBadge(user.avatarEmoji, user.avatarColorHex, if (rank == 1) 56.dp else 46.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("${user.totalXp} XP", style = MaterialTheme.typography.labelSmall, color = medalColor)
        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeightDp.dp),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            color = medalColor.copy(alpha = 0.25f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("#$rank", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = medalColor)
            }
        }
    }
}
