package com.example.core

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "study_time_prefs")

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    GRADIENT("Gradient")
}

enum class AppFont(val label: String) {
    INTER("Inter"),
    MANROPE("Manrope"),
    GEIST("Geist"),
    GENERAL_SANS("General Sans"),
    PLUS_JAKARTA_SANS("Plus Jakarta Sans")
}

enum class AmbientSound(val label: String) {
    NONE("Silent"),
    RAIN("Soft Rain"),
    FOREST("Forest Wind"),
    BROWN_NOISE("Deep Focus Noise")
}

data class LocalAppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appFont: AppFont = AppFont.PLUS_JAKARTA_SANS,
    val ambientSound: AmbientSound = AmbientSound.NONE,
    val chimeEnabled: Boolean = true,
    val breakReminderEnabled: Boolean = true,
    val onboardingSeen: Boolean = false,
    val aiRequestsToday: Int = 0,
    val aiRequestsDayKey: String = ""
)

class PreferencesManager(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_FONT = stringPreferencesKey("app_font")
        val AMBIENT_SOUND = stringPreferencesKey("ambient_sound")
        val CHIME_ENABLED = booleanPreferencesKey("chime_enabled")
        val BREAK_REMINDER = booleanPreferencesKey("break_reminder")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
        val AI_REQUESTS_TODAY = intPreferencesKey("ai_requests_today")
        val AI_REQUESTS_DAY = stringPreferencesKey("ai_requests_day")
    }

    val preferencesFlow: Flow<LocalAppPreferences> = context.studyPrefsDataStore.data.map { prefs ->
        val themeRaw = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val fontRaw = prefs[Keys.APP_FONT] ?: AppFont.PLUS_JAKARTA_SANS.name
        val soundRaw = prefs[Keys.AMBIENT_SOUND] ?: AmbientSound.NONE.name
        LocalAppPreferences(
            themeMode = enumValues<ThemeMode>().firstOrNull { it.name == themeRaw } ?: ThemeMode.SYSTEM,
            appFont = enumValues<AppFont>().firstOrNull { it.name == fontRaw } ?: AppFont.PLUS_JAKARTA_SANS,
            ambientSound = enumValues<AmbientSound>().firstOrNull { it.name == soundRaw } ?: AmbientSound.NONE,
            chimeEnabled = prefs[Keys.CHIME_ENABLED] ?: true,
            breakReminderEnabled = prefs[Keys.BREAK_REMINDER] ?: true,
            onboardingSeen = prefs[Keys.ONBOARDING_SEEN] ?: false,
            aiRequestsToday = prefs[Keys.AI_REQUESTS_TODAY] ?: 0,
            aiRequestsDayKey = prefs[Keys.AI_REQUESTS_DAY] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.studyPrefsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setAppFont(font: AppFont) {
        context.studyPrefsDataStore.edit { it[Keys.APP_FONT] = font.name }
    }

    suspend fun setAmbientSound(sound: AmbientSound) {
        context.studyPrefsDataStore.edit { it[Keys.AMBIENT_SOUND] = sound.name }
    }

    suspend fun setChimeEnabled(enabled: Boolean) {
        context.studyPrefsDataStore.edit { it[Keys.CHIME_ENABLED] = enabled }
    }

    suspend fun setBreakReminderEnabled(enabled: Boolean) {
        context.studyPrefsDataStore.edit { it[Keys.BREAK_REMINDER] = enabled }
    }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.studyPrefsDataStore.edit { it[Keys.ONBOARDING_SEEN] = seen }
    }

    suspend fun incrementAiUsage(dayKey: String) {
        context.studyPrefsDataStore.edit { prefs ->
            val currentDay = prefs[Keys.AI_REQUESTS_DAY] ?: ""
            val currentCount = if (currentDay == dayKey) (prefs[Keys.AI_REQUESTS_TODAY] ?: 0) else 0
            prefs[Keys.AI_REQUESTS_DAY] = dayKey
            prefs[Keys.AI_REQUESTS_TODAY] = currentCount + 1
        }
    }
}
