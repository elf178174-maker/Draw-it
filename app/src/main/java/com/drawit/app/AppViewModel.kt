package com.drawit.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drawit.app.data.Drawing
import com.drawit.app.data.ReminderSettings
import com.drawit.app.reminder.Notifier
import com.drawit.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DrawItApp

    val drawings: StateFlow<List<Drawing>> = app.drawings.drawings
    val settings: StateFlow<ReminderSettings> = app.settings.settings

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    init {
        viewModelScope.launch { app.drawings.load() }
    }

    fun fileFor(drawing: Drawing): File = app.drawings.fileFor(drawing)

    fun save(source: Uri, title: String, note: String, onDone: (Boolean) -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val result = app.drawings.add(source, title, note, System.currentTimeMillis())
            _saving.value = false
            onDone(result != null)
        }
    }

    fun update(id: String, title: String, note: String) {
        viewModelScope.launch { app.drawings.update(id, title, note) }
    }

    fun delete(id: String) {
        viewModelScope.launch { app.drawings.delete(id) }
    }

    fun setSettings(settings: ReminderSettings) {
        app.settings.update(settings)
        ReminderScheduler.apply(app, settings)
    }

    fun sendTestNotification() = Notifier.show(app, preview = true)

    fun canScheduleExact(): Boolean = ReminderScheduler.canScheduleExact(app)

    fun hasNotificationPermission(): Boolean = Notifier.hasPermission(app)

    fun nextReminderAt(settings: ReminderSettings): Long? =
        if (settings.enabled) ReminderScheduler.nextTrigger(settings) else null

    /** Consecutive days with at least one drawing, counting back from today. */
    fun streak(list: List<Drawing>): Int {
        if (list.isEmpty()) return 0
        val days = list.map { startOfDay(it.createdAt) }.toSet()
        var cursor = startOfDay(System.currentTimeMillis())
        if (!days.contains(cursor)) {
            cursor -= TimeUnit.DAYS.toMillis(1)
            if (!days.contains(startOfDay(cursor))) return 0
            cursor = startOfDay(cursor)
        }
        var count = 0
        while (days.contains(cursor)) {
            count++
            cursor = startOfDay(cursor - TimeUnit.DAYS.toMillis(1))
        }
        return count
    }

    fun drewToday(list: List<Drawing>): Boolean {
        val today = startOfDay(System.currentTimeMillis())
        return list.any { startOfDay(it.createdAt) == today }
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
