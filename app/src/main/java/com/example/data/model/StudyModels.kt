package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val country: String = "Global",
    val avatarEmoji: String = "🦉",
    val avatarColorHex: String = "#6C63FF",
    val planTier: String = "FREE",
    val totalXp: Long = 0L,
    val level: Long = 1L,
    val totalStudyMinutes: Long = 0L,
    val currentStreakDays: Long = 0L,
    val longestStreakDays: Long = 0L,
    val dailyGoalMinutes: Long = 120L,
    val onlineStatus: String = "ONLINE",
    val hideFromLeaderboard: Boolean = false,
    val publicProfile: Boolean = true,
    val suspended: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val country: String = "Global",
    val dailyGoalMinutes: Long = 120L,
    val themePreference: String = "SYSTEM",
    val fontPreference: String = "PLUS_JAKARTA_SANS",
    val planTier: String = "FREE",
    val role: String = "USER",
    val notificationsMuted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val iconKey: String = "book",
    val colorHex: String = "#6C63FF",
    val description: String = "",
    val targetMinutes: Long = 600L,
    val completedMinutes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudySession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val durationMinutes: Long = 25L,
    val mode: String = "POMODORO_25",
    val notes: String = "",
    val xpEarned: Long = 0L,
    val verifiedStartMillis: Long = 0L,
    val verifiedEndMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = "MEDIUM",
    val dueDateMillis: Long = 0L,
    val reminderEnabled: Boolean = true,
    val recurring: String = "NONE",
    val examFlag: Boolean = false,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyNote(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val subjectName: String = "",
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FriendRequest(
    @DocumentId val id: String = "",
    val fromUid: String = "",
    val fromName: String = "",
    val toUid: String = "",
    val toName: String = "",
    val status: String = "PENDING",
    val participantUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudyGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val goalMinutes: Long = 600L,
    val totalGroupMinutes: Long = 0L,
    val memberCount: Long = 1L,
    val publicGroup: Boolean = true,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMessage(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val replyToText: String = "",
    val emojiReaction: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DirectChat(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val participantLabels: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val mutedByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val replyToText: String = "",
    val emojiReaction: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Challenge(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val targetMinutes: Long = 120L,
    val xpReward: Long = 200L,
    val scope: String = "GLOBAL",
    val participantUids: List<String> = emptyList(),
    val status: String = "ACTIVE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserAchievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val key: String = "",
    val title: String = "",
    val description: String = "",
    val xpReward: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "STUDY",
    val hasRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val authorId: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetUid: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BlockedUser(
    @DocumentId val id: String = "",
    val userId: String = "",
    val blockedUid: String = "",
    val blockedName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
