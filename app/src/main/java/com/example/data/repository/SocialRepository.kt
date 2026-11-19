package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.Announcement
import com.example.data.model.Challenge
import com.example.data.model.ChatMessage
import com.example.data.model.DirectChat
import com.example.data.model.FriendRequest
import com.example.data.model.GroupMessage
import com.example.data.model.ModerationReport
import com.example.data.model.PrivateUser
import com.example.data.model.PublicProfile
import com.example.data.model.StudyGroup
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialRepository(
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

    fun observeAllPublicProfiles(): Flow<List<PublicProfile>> = flow {
        requireUserId()
        emitAll(
            db.collection("public_profiles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PublicProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.totalXp }
                }
        )
    }

    fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> = flow {
        requireUserId()
        emitAll(
            db.collection("friend_requests")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    fun observePublicStudyGroups(userId: String): Flow<List<StudyGroup>> = flow {
        requireUserId()
        val publicFlow = db.collection("study_groups")
            .whereEqualTo("publicGroup", true)
            .snapshots()
            .map { it.toObjects(StudyGroup::class.java, ServerTimestampBehavior.ESTIMATE) }
        val memberFlow = db.collection("study_groups")
            .whereArrayContains("memberUids", userId)
            .snapshots()
            .map { it.toObjects(StudyGroup::class.java, ServerTimestampBehavior.ESTIMATE) }
        emitAll(
            combine(publicFlow, memberFlow) { pub, mine ->
                (pub + mine).distinctBy { it.id }.sortedByDescending { it.memberCount }
            }
        )
    }

    fun observeGroupMessages(groupId: String): Flow<List<GroupMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("study_groups").document(groupId).collection("messages")
                .snapshots()
                .map { snap ->
                    snap.toObjects(GroupMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeDirectChats(userId: String): Flow<List<DirectChat>> = flow {
        requireUserId()
        emitAll(
            db.collection("chats")
                .whereArrayContains("participantUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DirectChat::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
        )
    }

    fun observeChatMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        requireUserId()
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
        )
    }

    fun observeChallenges(userId: String): Flow<List<Challenge>> = flow {
        requireUserId()
        val globalFlow = db.collection("challenges")
            .whereEqualTo("scope", "GLOBAL")
            .snapshots()
            .map { it.toObjects(Challenge::class.java, ServerTimestampBehavior.ESTIMATE) }
        val mineFlow = db.collection("challenges")
            .whereArrayContains("participantUids", userId)
            .snapshots()
            .map { it.toObjects(Challenge::class.java, ServerTimestampBehavior.ESTIMATE) }
        emitAll(
            combine(globalFlow, mineFlow) { g, m ->
                (g + m).distinctBy { it.id }.sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        requireUserId()
        emitAll(
            db.collection("announcements")
                .snapshots()
                .map { snap ->
                    snap.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeAdminReports(): Flow<List<ModerationReport>> = flow {
        requireUserId()
        emitAll(
            db.collection("reports")
                .snapshots()
                .map { snap ->
                    snap.toObjects(ModerationReport::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
        )
    }

    fun observeAdminAllUsers(): Flow<List<PrivateUser>> = flow {
        requireUserId()
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrivateUser::class.java, ServerTimestampBehavior.ESTIMATE)
                }
        )
    }

    suspend fun sendFriendRequest(
        fromName: String,
        toUid: String,
        toName: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == toUid) return Result.failure(IllegalArgumentException("Cannot send a friend request to yourself."))
        val reqId = if (uid < toUid) "${uid}_${toUid}" else "${toUid}_${uid}"
        val path = "friend_requests/$reqId"
        return try {
            val existing = db.document(path).get().await()
            if (existing.exists()) {
                return Result.failure(IllegalStateException("Friend request or connection already exists."))
            }
            db.document(path).set(
                mapOf(
                    "fromUid" to uid,
                    "fromName" to fromName.ifBlank { "Scholar" }.take(60),
                    "toUid" to toUid,
                    "toName" to toName.ifBlank { "Scholar" }.take(60),
                    "status" to "PENDING",
                    "participantUids" to listOf(uid, toUid),
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

    suspend fun respondFriendRequest(requestId: String, accept: Boolean): Result<Unit> {
        requireUserId()
        val path = "friend_requests/$requestId"
        return try {
            db.document(path).update(
                mapOf(
                    "status" to if (accept) "ACCEPTED" else "REJECTED",
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

    suspend fun removeFriend(requestId: String): Result<Unit> {
        requireUserId()
        val path = "friend_requests/$requestId"
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

    suspend fun openOrCreateDirectChat(
        myName: String,
        otherUid: String,
        otherName: String
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val chatId = if (uid < otherUid) "${uid}_${otherUid}" else "${otherUid}_${uid}"
        val path = "chats/$chatId"
        return try {
            val doc = db.document(path).get().await()
            if (!doc.exists()) {
                db.document(path).set(
                    mapOf(
                        "participantUids" to listOf(uid, otherUid),
                        "participantLabels" to listOf(myName.take(40), otherName.take(40)),
                        "lastMessage" to "Started a study chat",
                        "lastSenderId" to uid,
                        "mutedByUids" to emptyList<String>(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun sendDirectMessage(
        chatId: String,
        senderName: String,
        text: String,
        memberUids: List<String>,
        replyToText: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val path = "chats/$chatId/messages"
        return try {
            val payload = mutableMapOf<String, Any>(
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "Scholar" }.take(60),
                "text" to cleanText,
                "memberUids" to memberUids,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!replyToText.isNullOrBlank()) {
                payload["replyToText"] = replyToText.take(200)
            }
            db.collection("chats").document(chatId).collection("messages").document().set(payload).await()
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to cleanText.take(200),
                    "lastSenderId" to uid,
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

    suspend fun reactToDirectMessage(chatId: String, messageId: String, emoji: String): Result<Unit> {
        requireUserId()
        val path = "chats/$chatId/messages/$messageId"
        return try {
            db.document(path).update(
                mapOf(
                    "emojiReaction" to emoji.take(16),
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

    suspend fun deleteDirectMessage(chatId: String, messageId: String): Result<Unit> {
        requireUserId()
        val path = "chats/$chatId/messages/$messageId"
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

    suspend fun toggleChatMuted(chat: DirectChat): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "chats/${chat.id}"
        return try {
            val updated = if (uid in chat.mutedByUids) chat.mutedByUids - uid else chat.mutedByUids + uid
            db.document(path).update(
                mapOf(
                    "mutedByUids" to updated,
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

    suspend fun createStudyGroup(
        name: String,
        description: String,
        goalMinutes: Long,
        publicGroup: Boolean
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "study_groups"
        return try {
            val cleanName = name.trim().take(60)
            require(cleanName.length >= 2) { "Group name must be at least 2 characters." }
            val docRef = db.collection("study_groups").document()
            docRef.set(
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(300),
                    "ownerId" to uid,
                    "goalMinutes" to goalMinutes.coerceIn(30L, 1000000L),
                    "totalGroupMinutes" to 0L,
                    "memberCount" to 1L,
                    "publicGroup" to publicGroup,
                    "memberUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinStudyGroup(group: StudyGroup): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid in group.memberUids) return Result.success(Unit)
        val path = "study_groups/${group.id}"
        return try {
            val nextMembers = (group.memberUids + uid).distinct().take(50)
            db.document(path).update(
                mapOf(
                    "memberUids" to nextMembers,
                    "memberCount" to nextMembers.size.toLong(),
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

    suspend fun leaveStudyGroup(group: StudyGroup): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (group.ownerId == uid) return Result.failure(IllegalStateException("Group owners cannot leave without deleting the group."))
        val path = "study_groups/${group.id}"
        return try {
            val nextMembers = group.memberUids - uid
            db.document(path).update(
                mapOf(
                    "memberUids" to nextMembers,
                    "memberCount" to nextMembers.size.coerceAtLeast(1).toLong(),
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

    suspend fun sendGroupMessage(
        groupId: String,
        senderName: String,
        text: String,
        replyToText: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty."))
        val path = "study_groups/$groupId/messages"
        return try {
            val payload = mutableMapOf<String, Any>(
                "groupId" to groupId,
                "senderId" to uid,
                "senderName" to senderName.ifBlank { "Scholar" }.take(60),
                "text" to cleanText,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!replyToText.isNullOrBlank()) {
                payload["replyToText"] = replyToText.take(200)
            }
            db.collection("study_groups").document(groupId).collection("messages").document().set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun reactToGroupMessage(groupId: String, messageId: String, emoji: String): Result<Unit> {
        requireUserId()
        val path = "study_groups/$groupId/messages/$messageId"
        return try {
            db.document(path).update(
                mapOf(
                    "emojiReaction" to emoji.take(16),
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

    suspend fun createChallenge(
        title: String,
        description: String,
        creatorName: String,
        targetMinutes: Long,
        xpReward: Long,
        scope: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "challenges"
        return try {
            val cleanTitle = title.trim().take(80)
            require(cleanTitle.length >= 2) { "Challenge title is required." }
            db.collection("challenges").document().set(
                mapOf(
                    "title" to cleanTitle,
                    "description" to description.trim().take(300),
                    "creatorId" to uid,
                    "creatorName" to creatorName.ifBlank { "Scholar" }.take(60),
                    "targetMinutes" to targetMinutes.coerceIn(15L, 100000L),
                    "xpReward" to xpReward.coerceIn(10L, 5000L),
                    "scope" to scope,
                    "participantUids" to listOf(uid),
                    "status" to "ACTIVE",
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

    suspend fun joinChallenge(challenge: Challenge): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid in challenge.participantUids) return Result.success(Unit)
        val path = "challenges/${challenge.id}"
        return try {
            val updated = (challenge.participantUids + uid).distinct().take(100)
            db.document(path).update(
                mapOf(
                    "participantUids" to updated,
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

    suspend fun submitReport(targetUid: String, reason: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "reports"
        return try {
            val cleanReason = reason.trim().take(300)
            require(cleanReason.length >= 3) { "Please provide at least 3 characters describing the report." }
            db.collection("reports").document().set(
                mapOf(
                    "reporterId" to uid,
                    "targetUid" to targetUid,
                    "reason" to cleanReason,
                    "status" to "OPEN",
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

    // --- ADMIN OPERATIONS (Protected in firestore.rules via isAdmin()) ---

    suspend fun adminPublishAnnouncement(title: String, message: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "announcements"
        return try {
            db.collection("announcements").document().set(
                mapOf(
                    "title" to title.trim().take(100),
                    "message" to message.trim().take(600),
                    "authorId" to uid,
                    "active" to true,
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

    suspend fun adminSetUserSuspended(targetUid: String, suspended: Boolean): Result<Unit> {
        requireUserId()
        val path = "public_profiles/$targetUid"
        return try {
            db.document(path).update(
                mapOf(
                    "suspended" to suspended,
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

    suspend fun adminResolveReport(reportId: String): Result<Unit> {
        requireUserId()
        val path = "reports/$reportId"
        return try {
            db.document(path).update(
                mapOf(
                    "status" to "RESOLVED",
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
}
