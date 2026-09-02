package com.drawit.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether the streak celebration is allowed to make noise or buzz. */
data class FeedbackSettings(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

/** Reminder preferences, kept in SharedPreferences so they survive reboots and updates. */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    private val _feedback = MutableStateFlow(readFeedback())
    val feedback: StateFlow<FeedbackSettings> = _feedback.asStateFlow()

    fun updateFeedback(settings: FeedbackSettings) {
        prefs.edit()
            .putBoolean(KEY_SOUND, settings.soundEnabled)
            .putBoolean(KEY_HAPTICS, settings.hapticsEnabled)
            .apply()
        _feedback.value = settings
    }

    fun update(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.enabled)
            .putInt(KEY_HOUR, settings.hour)
            .putInt(KEY_MINUTE, settings.minute)
            .putStringSet(KEY_DAYS, settings.days.map { it.toString() }.toSet())
            .apply()
        _settings.value = settings
    }

    fun reload() {
        _settings.value = read()
        _feedback.value = readFeedback()
    }

    private fun readFeedback() = FeedbackSettings(
        soundEnabled = prefs.getBoolean(KEY_SOUND, true),
        hapticsEnabled = prefs.getBoolean(KEY_HAPTICS, true)
    )

    private fun read(): ReminderSettings {
        val days = prefs.getStringSet(KEY_DAYS, null)
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: ReminderSettings.ALL_DAYS
        return ReminderSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 19),
            minute = prefs.getInt(KEY_MINUTE, 0),
            days = days.ifEmpty { ReminderSettings.ALL_DAYS }
        )
    }

    private companion object {
        const val KEY_ENABLED = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val KEY_DAYS = "reminder_days"
        const val KEY_SOUND = "feedback_sound"
        const val KEY_HAPTICS = "feedback_haptics"
    }
}
