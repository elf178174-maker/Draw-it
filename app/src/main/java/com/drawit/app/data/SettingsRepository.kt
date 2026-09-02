package com.drawit.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reminder preferences, kept in SharedPreferences so they survive reboots and updates. */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

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
    }

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
    }
}
