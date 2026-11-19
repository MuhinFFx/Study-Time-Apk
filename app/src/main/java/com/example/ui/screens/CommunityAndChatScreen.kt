package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BlockedUser
import com.example.data.model.Challenge
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.PublicProfile
import com.example.data.model.StudyGroup
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDurationMinutes
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentTeal
import com.example.ui.theme.BrandViolet
import kotlinx.coroutines.flow.Flow

@Composable
fun CommunityAndChatScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    allProfiles: List<PublicProfile>,
    friendRequests: List<FriendRequest>,
    directChats: List<DirectChat>,
    studyGroups: List<StudyGroup>,
    challenges: List<Challenge>,
    blockedUsers: List<BlockedUser>,
    observeChatMessages: (String) -> Flow<List<ChatMessage>>,
    observeGroupMessages: (String) -> Flow<List<GroupMessage>>,
    onSendFriendRequest: (String, String) -> Unit,
    onRespondFriendRequest: (String, Boolean) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onOpenDirectChat: (String, String, (String) -> Unit) -> Unit,
    onSendDirectMessage: (String, String, List<String>, String?) -> Unit,
    onReactDirectMessage: (String, String, String) -> Unit,
    onDeleteDirectMessage: (String, String) -> Unit,
    onToggleChatMuted: (DirectChat) -> Unit,
    onCreateGroup: (String, String, Int, Boolean) -> Unit,
    onJoinGroup: (StudyGroup) -> Unit,
    onLeaveGroup: (StudyGroup) -> Unit,
    onSendGroupMessage: (String, String, String?) -> Unit,
    onReactGroupMessage: (String, String, String) -> Unit,
    onCreateChallenge: (String, String, Int, Int, String) -> Unit,
    onJoinChallenge: (Challenge) -> Unit,
    onBlockAndReport: (String, String, String) -> Unit
) {
    var activeChatId by remember { mutableStateOf<String?>(null) }
    var activeGroupId by remember { mutableStateOf<String?>(null) }

    val selectedChat = directChats.firstOrNull { it.id == activeChatId }
    val selectedGroup = studyGroups.firstOrNull { it.id == activeGroupId }

    if (selectedChat != null) {
        DirectChatRoomScreen(
            currentUserId = currentUserId,
            chat = selectedChat,
            messagesFlow = remember(selectedChat.id) { observeChatMessages(selectedChat.id) },
            onSend = { text, reply ->
                onSendDirectMessage(selectedChat.id, text, selectedChat.participantUids, reply)
            },
            onReact = { msgId, emoji -> onReactDirectMessage(selectedChat.id, msgId, emoji) },
            onDelete = { msgId -> onDeleteDirectMessage(selectedChat.id, msgId) },
            onToggleMute = { onToggleChatMuted(selectedChat) },
            onBlockReport = { otherUid, otherName, reason ->
                onBlockAndReport(otherUid, otherName, reason)
                activeChatId = null
            },
            onBack = { activeChatId = null }
        )
        return
    }

    if (selectedGroup != null) {
        GroupChatRoomScreen(
            currentUserId = currentUserId,
            group = selectedGroup,
            messagesFlow = remember(selectedGroup.id) { observeGroupMessages(selectedGroup.id) },
            onSend = { text, reply -> onSendGroupMessage(selectedGroup.id, text, reply) },
            onReact = { msgId, emoji -> onReactGroupMessage(selectedGroup.id, msgId, emoji) },
            onLeave = {
                onLeaveGroup(selectedGroup)
                activeGroupId = null
            },
            onBack = { activeGroupId = null }
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchUserQuery by remember { mutableStateOf("") }
    var showCreateGroup by remember { mutableStateOf(false) }
    var showCreateChallenge by remember { mutableStateOf(false) }
    var viewingProfile by remember { mutableStateOf<PublicProfile?>(null) }

    val blockedSet = remember(blockedUsers) { blockedUsers.map { it.blockedUid }.toSet() }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1 || selectedTab == 3) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 1) showCreateGroup = true else showCreateChallenge = true
                    },
                    containerColor = BrandViolet,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("community_create_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Chats") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Groups") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Friends") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Challenges") })
            }

            when (selectedTab) {
                0 -> {
                    val visibleChats = directChats.filter { chat ->
                        chat.participantUids.none { it in blockedSet }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (visibleChats.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No messages yet",
                                    subtitle = "Find a study buddy in the Friends tab to start a private 1-on-1 study chat.",
                                    actionText = "Find Friends",
                                    onAction = { selectedTab = 2 }
                                )
                            }
                        } else {
                            items(visibleChats, key = { it.id }) { chat ->
                                val otherIdx = if (chat.participantUids.firstOrNull() == currentUserId) 1 else 0
                                val otherName = chat.participantLabels.getOrElse(otherIdx) { "Study Buddy" }
                                val isMuted = currentUserId in chat.mutedByUids
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activeChatId = chat.id },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarBadge("💬", "#6C63FF", 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(otherName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                chat.lastMessage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        if (isMuted) {
                                            Icon(Icons.Filled.NotificationsOff, contentDescription = "Muted", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (studyGroups.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No study groups yet",
                                    subtitle = "Create a collaborative study room with shared goals and live group chat.",
                                    actionText = "Create Group",
                                    onAction = { showCreateGroup = true }
                                )
                            }
                        } else {
                            items(studyGroups, key = { it.id }) { grp ->
                                val isMember = currentUserId in grp.memberUids
                                val progress = if (grp.goalMinutes > 0) {
                                    (grp.totalGroupMinutes.toFloat() / grp.goalMinutes.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(grp.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${grp.memberCount} members • Goal: ${grp.goalMinutes / 60}h",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isMember) {
                                                Button(onClick = { activeGroupId = grp.id }) {
                                                    Text("Open Chat")
                                                }
                                            } else {
                                                OutlinedButton(onClick = { onJoinGroup(grp) }) {
                                                    Text("Join")
                                                }
                                            }
                                        }
                                        if (grp.description.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(grp.description, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val incomingPending = friendRequests.filter { it.toUid == currentUserId && it.status == "PENDING" }
                    val accepted = friendRequests.filter { it.status == "ACCEPTED" }
                    val friendUids = accepted.mapNotNull { if (it.fromUid == currentUserId) it.toUid else it.fromUid }.toSet()
                    val pendingUids = friendRequests.filter { it.status == "PENDING" }
                        .flatMap { it.participantUids }.toSet()

                    val discoverable = remember(allProfiles, searchUserQuery, blockedSet, currentUserId) {
                        allProfiles.filter { prof ->
                            prof.userId != currentUserId &&
                                prof.userId !in blockedSet &&
                                !prof.suspended &&
                                prof.publicProfile &&
                                (searchUserQuery.isBlank() ||
                                    prof.displayName.contains(searchUserQuery, ignoreCase = true) ||
                                    prof.username.contains(searchUserQuery, ignoreCase = true) ||
                                    prof.country.contains(searchUserQuery, ignoreCase = true))
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = searchUserQuery,
                                onValueChange = { searchUserQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                placeholder = { Text("Search learners by name, @username, or country...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        if (incomingPending.isNotEmpty()) {
                            item { SectionHeader("Incoming Requests (${incomingPending.size})") }
                            items(incomingPending, key = { it.id }) { req ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(req.fromName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        TextButton(onClick = { onRespondFriendRequest(req.id, false) }) { Text("Decline") }
                                        Button(onClick = { onRespondFriendRequest(req.id, true) }) { Text("Accept") }
                                    }
                                }
                            }
                        }

                        if (accepted.isNotEmpty()) {
                            item { SectionHeader("My Study Friends (${accepted.size})") }
                            items(accepted, key = { it.id }) { req ->
                                val otherUid = if (req.fromUid == currentUserId) req.toUid else req.fromUid
                                val otherName = if (req.fromUid == currentUserId) req.toName else req.fromName
                                val otherProfile = allProfiles.firstOrNull { it.userId == otherUid }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarBadge(
                                            otherProfile?.avatarEmoji ?: "🦉",
                                            otherProfile?.avatarColorHex ?: "#6C63FF",
                                            42.dp,
                                            otherProfile?.onlineStatus
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(otherName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Lv.${otherProfile?.level ?: 1} • ${otherProfile?.country ?: "Global"}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            onOpenDirectChat(otherUid, otherName) { chatId ->
                                                activeChatId = chatId
                                            }
                                        }) {
                                            Icon(Icons.Filled.Chat, contentDescription = "Chat with $otherName", tint = BrandViolet)
                                        }
                                        IconButton(onClick = { onRemoveFriend(req.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove friend")
                                        }
                                    }
                                }
                            }
                        }

                        item { SectionHeader("Discover Learners") }
                        if (discoverable.isEmpty()) {
                            item {
                                Text(
                                    "No other learners matched your search yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(discoverable, key = { it.userId }) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewingProfile = user },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AvatarBadge(user.avatarEmoji, user.avatarColorHex, 42.dp, user.onlineStatus)
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "@${user.username} • ${user.country} • Lv.${user.level}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (user.userId !in friendUids && user.userId !in pendingUids) {
                                            IconButton(onClick = { onSendFriendRequest(user.userId, user.displayName) }) {
                                                Icon(Icons.Filled.PersonAdd, contentDescription = "Add friend", tint = BrandViolet)
                                            }
                                        }
                                        IconButton(onClick = {
                                            onOpenDirectChat(user.userId, user.displayName) { chatId ->
                                                activeChatId = chatId
                                            }
                                        }) {
                                            Icon(Icons.Filled.Chat, contentDescription = "Message", tint = AccentTeal)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    val myMins = myProfile?.totalStudyMinutes ?: 0L
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (challenges.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    title = "No challenges yet",
                                    subtitle = "Create a personal or global study sprint challenge and earn bonus XP!",
                                    actionText = "Create Challenge",
                                    onAction = { showCreateChallenge = true }
                                )
                            }
                        } else {
                            items(challenges, key = { it.id }) { ch ->
                                val joined = currentUserId in ch.participantUids
                                val progress = if (ch.targetMinutes > 0) (myMins.toFloat() / ch.targetMinutes.toFloat()).coerceIn(0f, 1f) else 0f
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ch.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${ch.scope} • ${ch.participantUids.size} participants • +${ch.xpReward} XP",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = AccentAmber
                                                )
                                            }
                                            if (!joined) {
                                                Button(onClick = { onJoinChallenge(ch) }) { Text("Join") }
                                            } else {
                                                Surface(
                                                    color = AccentTeal.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        "Joined",
                                                        color = AccentTeal,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (ch.description.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(ch.description, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "Target: ${formatDurationMinutes(ch.targetMinutes)}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroup) {
        CreateGroupDialog(
            onDismiss = { showCreateGroup = false },
            onCreate = { name, desc, goalHours, isPublic ->
                onCreateGroup(name, desc, goalHours, isPublic)
                showCreateGroup = false
            }
        )
    }

    if (showCreateChallenge) {
        CreateChallengeDialog(
            onDismiss = { showCreateChallenge = false },
            onCreate = { title, desc, targetMins, xp, scope ->
                onCreateChallenge(title, desc, targetMins, xp, scope)
                showCreateChallenge = false
            }
        )
    }

    viewingProfile?.let { prof ->
        var reportReason by remember { mutableStateOf("") }
        var showReportBox by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { viewingProfile = null },
            title = { Text(prof.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarBadge(prof.avatarEmoji, prof.avatarColorHex, 52.dp, prof.onlineStatus)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("@${prof.username}", style = MaterialTheme.typography.labelLarge)
                            Text("${prof.country} • Level ${prof.level}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(prof.bio.ifBlank { "No bio added." })
                    Text("Verified Study: ${formatDurationMinutes(prof.totalStudyMinutes)} • ${prof.totalXp} XP")
                    if (showReportBox) {
                        OutlinedTextField(
                            value = reportReason,
                            onValueChange = { reportReason = it },
                            label = { Text("Report reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (showReportBox) {
                    Button(
                        onClick = {
                            onBlockAndReport(prof.userId, prof.displayName, reportReason.ifBlank { "Inappropriate behavior" })
                            viewingProfile = null
                        }
                    ) { Text("Block & Submit Report") }
                } else {
                    Button(onClick = { viewingProfile = null }) { Text("Done") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportBox = !showReportBox }) {
                    Text(if (showReportBox) "Cancel Report" else "Report / Block")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectChatRoomScreen(
    currentUserId: String,
    chat: DirectChat,
    messagesFlow: Flow<List<ChatMessage>>,
    onSend: (String, String?) -> Unit,
    onReact: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleMute: () -> Unit,
    onBlockReport: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val otherIdx = if (chat.participantUids.firstOrNull() == currentUserId) 1 else 0
    val otherUid = chat.participantUids.getOrElse(otherIdx) { "" }
    val otherName = chat.participantLabels.getOrElse(otherIdx) { "Study Buddy" }
    val isMuted = currentUserId in chat.mutedByUids

    var draft by remember { mutableStateOf("") }
    var replyQuote by remember { mutableStateOf<String?>(null) }
    var showBlockDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            if (isMuted) Icons.Filled.NotificationsOff else Icons.Filled.Notifications,
                            contentDescription = "Mute chat"
                        )
                    }
                    IconButton(onClick = { showBlockDialog = true }) {
                        Icon(Icons.Filled.Flag, contentDescription = "Report or Block")
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isMine) BrandViolet else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.replyToText.isNotBlank()) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "↪ ${msg.replyToText}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                                Text(
                                    text = msg.text,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                if (msg.emojiReaction.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(msg.emojiReaction)
                                }
                            }
                        }
                        Row {
                            TextButton(onClick = { replyQuote = msg.text.take(80) }) {
                                Icon(Icons.Filled.Reply, contentDescription = "Reply", modifier = Modifier.size(15.dp))
                            }
                            listOf("🔥", "👏", "💯").forEach { emoji ->
                                TextButton(onClick = { onReact(msg.id, emoji) }) { Text(emoji) }
                            }
                            if (isMine) {
                                IconButton(onClick = { onDelete(msg.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (replyQuote != null) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Replying to: $replyQuote", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { replyQuote = null }) { Text("Clear") }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message $otherName...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft, replyQuote)
                            draft = ""
                            replyQuote = null
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandViolet)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }

    if (showBlockDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block & Report $otherName") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for report") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onBlockReport(otherUid, otherName, reason.ifBlank { "Spam or harassment" })
                    showBlockDialog = false
                }) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupChatRoomScreen(
    currentUserId: String,
    group: StudyGroup,
    messagesFlow: Flow<List<GroupMessage>>,
    onSend: (String, String?) -> Unit,
    onReact: (String, String) -> Unit,
    onLeave: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val messages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by remember { mutableStateOf("") }
    var replyQuote by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                        Text("${group.memberCount} members", style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (group.ownerId != currentUserId) {
                        TextButton(onClick = onLeave) { Text("Leave") }
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUserId
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        if (!isMine) {
                            Text(msg.senderName, style = MaterialTheme.typography.labelSmall, color = BrandViolet)
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isMine) BrandViolet else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.widthIn(max = 290.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.replyToText.isNotBlank()) {
                                    Text(
                                        "↪ ${msg.replyToText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(msg.text, color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface)
                                if (msg.emojiReaction.isNotBlank()) {
                                    Text(msg.emojiReaction)
                                }
                            }
                        }
                        Row {
                            TextButton(onClick = { replyQuote = msg.text.take(80) }) { Text("Reply") }
                            listOf("🔥", "👏").forEach { emoji ->
                                TextButton(onClick = { onReact(msg.id, emoji) }) { Text(emoji) }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Message ${group.name}...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onSend(draft, replyQuote)
                            draft = ""
                            replyQuote = null
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BrandViolet)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var goalHours by remember { mutableStateOf("20") }
    var publicGroup by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                OutlinedTextField(value = goalHours, onValueChange = { goalHours = it.filter(Char::isDigit) }, label = { Text("Shared Goal (Hours)") }, singleLine = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Public Group (Discoverable)")
                    Switch(checked = publicGroup, onCheckedChange = { publicGroup = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, desc, goalHours.toIntOrNull() ?: 20, publicGroup) },
                enabled = name.trim().length >= 2
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CreateChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var targetMins by remember { mutableStateOf("120") }
    var scope by remember { mutableStateOf("GLOBAL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Study Challenge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Challenge Title") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Rules / Details") }, maxLines = 2)
                OutlinedTextField(value = targetMins, onValueChange = { targetMins = it.filter(Char::isDigit) }, label = { Text("Target Minutes") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GLOBAL", "PERSONAL", "GROUP").forEach { s ->
                        FilterChip(selected = scope == s, onClick = { scope = s }, label = { Text(s) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, desc, targetMins.toIntOrNull() ?: 120, 250, scope) },
                enabled = title.trim().length >= 2
            ) { Text("Launch") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
