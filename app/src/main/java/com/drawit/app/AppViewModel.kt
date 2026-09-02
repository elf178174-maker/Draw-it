package com.drawit.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drawit.app.data.BackupManager
import com.drawit.app.data.Drawing
import com.drawit.app.data.FeedbackSettings
import com.drawit.app.data.ReminderSettings
import com.drawit.app.data.StreakOutcome
import com.drawit.app.data.StreakState
import com.drawit.app.reminder.Notifier
import com.drawit.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DrawItApp

    val drawings: StateFlow<List<Drawing>> = app.drawings.drawings
    val settings: StateFlow<ReminderSettings> = app.settings.settings
    val feedbackSettings: StateFlow<FeedbackSettings> = app.settings.feedback
    val streak: StateFlow<StreakState> = app.streak.state

    /** Set when a freeze was spent or a streak was lost while the app was away. */
    val streakNotice: StateFlow<StreakOutcome?> = app.streak.pendingNotice

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Set right after a save so the celebration knows what to show. */
    private val _celebration = MutableStateFlow<StreakOutcome?>(null)
    val celebration: StateFlow<StreakOutcome?> = _celebration.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    init {
        viewModelScope.launch { app.drawings.load() }
    }

    fun fileFor(drawing: Drawing): File = app.drawings.fileFor(drawing)

    // -- drawings ---------------------------------------------------------

    fun save(source: Uri, title: String, note: String, onDone: (Boolean) -> Unit) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val result = app.drawings.add(source, title, note, System.currentTimeMillis())
            if (result != null) {
                val outcome = app.streak.recordDrawing()
                if (outcome.extended) _celebration.value = outcome
            }
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

    // -- streak -----------------------------------------------------------

    /** Brings the streak up to date; call when the app comes back to the front. */
    fun settleStreak() {
        app.streak.settle()
    }

    fun playCelebration(outcome: StreakOutcome) {
        if (outcome.milestone) app.feedback.milestoneReached() else app.feedback.streakExtended()
    }

    fun clearCelebration() {
        _celebration.value = null
    }

    fun playNotice(outcome: StreakOutcome) {
        if (outcome.freezesSpent > 0) app.feedback.freezeSpent() else app.feedback.streakLost()
    }

    fun clearStreakNotice() {
        app.streak.clearNotice()
    }

    fun tick() = app.feedback.tick()

    /** Plays the celebration cue on its own, for the Settings preview. */
    fun previewCelebration() = app.feedback.streakExtended()

    /** Whether a drawing has already been counted today. */
    fun drewToday(state: StreakState = streak.value): Boolean = state.drewOn(LocalDate.now())

    /** The set of local days, as epoch days, that have at least one drawing. */
    fun drawnDays(list: List<Drawing> = drawings.value): Set<Long> =
        list.map {
            java.time.Instant.ofEpochMilli(it.createdAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay()
        }.toSet()

    // -- reminder ---------------------------------------------------------

    fun setSettings(settings: ReminderSettings) {
        app.settings.update(settings)
        ReminderScheduler.apply(app, settings)
    }

    fun setFeedbackSettings(settings: FeedbackSettings) {
        app.settings.updateFeedback(settings)
        app.feedback.soundEnabled = settings.soundEnabled
        app.feedback.hapticsEnabled = settings.hapticsEnabled
    }

    fun sendTestNotification() = Notifier.show(app, preview = true)

    fun canScheduleExact(): Boolean = ReminderScheduler.canScheduleExact(app)

    fun hasNotificationPermission(): Boolean = Notifier.hasPermission(app)

    fun nextReminderAt(settings: ReminderSettings): Long? =
        if (settings.enabled) ReminderScheduler.nextTrigger(settings) else null

    // -- backup -----------------------------------------------------------

    fun suggestedBackupName(): String = app.backups.suggestedFileName()

    fun exportAlbum(target: Uri) {
        if (_backupBusy.value) return
        _backupBusy.value = true
        viewModelScope.launch {
            val result = app.backups.export(target)
            _backupBusy.value = false
            _backupMessage.value = result.fold(
                onSuccess = { r ->
                    val size = formatSize(r.bytes)
                    if (r.drawings == 1) "Exported 1 drawing ($size)."
                    else "Exported ${r.drawings} drawings ($size)."
                },
                onFailure = { it.message ?: "That export did not finish." }
            )
        }
    }

    fun importAlbum(source: Uri) {
        if (_backupBusy.value) return
        _backupBusy.value = true
        viewModelScope.launch {
            val result = app.backups.import(source)
            _backupBusy.value = false
            _backupMessage.value = result.fold(
                onSuccess = { r ->
                    buildString {
                        append(if (r.added == 1) "Added 1 drawing" else "Added ${r.added} drawings")
                        if (r.duplicates > 0) append(", skipped ${r.duplicates} already here")
                        append(".")
                        if (r.streakRestored) append(" Your streak came back too.")
                    }
                },
                onFailure = { it.message ?: "That file could not be read." }
            )
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
        bytes >= 1_000 -> "${bytes / 1_000} KB"
        else -> "$bytes bytes"
    }
}
