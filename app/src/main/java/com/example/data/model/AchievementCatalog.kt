package com.example.data.model

data class AchievementSpec(
    val key: String,
    val title: String,
    val description: String,
    val xpReward: Long,
    val emoji: String
)

object AchievementCatalog {
    val allAchievements = listOf(
        AchievementSpec("first_session", "First Study Session", "Complete your very first verified focus session.", 50L, "🌱"),
        AchievementSpec("focus_master", "Focus Master", "Complete 10 verified study sessions.", 150L, "🎯"),
        AchievementSpec("hours_10", "10 Hours Studied", "Accumulate 600 verified minutes of deep study.", 250L, "⏳"),
        AchievementSpec("hours_50", "50 Hours Studied", "Accumulate 3,000 verified minutes of study.", 500L, "📚"),
        AchievementSpec("hours_100", "100 Hours Club", "Join the elite 6,000-minute scholar club.", 1000L, "👑"),
        AchievementSpec("streak_7", "7 Day Streak", "Study consistently for 7 days in a row.", 300L, "🔥"),
        AchievementSpec("streak_30", "30 Day Streak", "Maintain a full month study streak.", 1200L, "⚡"),
        AchievementSpec("early_bird", "Early Bird", "Finish a verified study session before 8:00 AM.", 100L, "🌅"),
        AchievementSpec("night_owl", "Night Owl", "Finish a verified study session after 10:00 PM.", 100L, "🦉"),
        AchievementSpec("goal_crusher", "Goal Crusher", "Hit 100% of your daily study goal.", 150L, "🏆"),
        AchievementSpec("social_star", "Social Star", "Join a study group or connect with a study friend.", 100L, "🤝"),
        AchievementSpec("challenge_champion", "Challenge Champion", "Join or create a community study challenge.", 200L, "🚀")
    )

    fun levelForXp(totalXp: Long): Long {
        if (totalXp <= 0L) return 1L
        var lvl = 1L
        var threshold = 100L
        var remaining = totalXp
        while (remaining >= threshold && lvl < 100L) {
            remaining -= threshold
            lvl++
            threshold = 100L + (lvl - 1L) * 40L
        }
        return lvl.coerceIn(1L, 100L)
    }

    fun xpProgressInLevel(totalXp: Long): Pair<Long, Long> {
        var lvl = 1L
        var threshold = 100L
        var remaining = totalXp.coerceAtLeast(0L)
        while (remaining >= threshold && lvl < 100L) {
            remaining -= threshold
            lvl++
            threshold = 100L + (lvl - 1L) * 40L
        }
        return remaining to threshold
    }
}
