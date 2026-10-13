package com.drawit.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drawit.app.data.Celebration
import com.drawit.app.data.Drawing
import com.drawit.app.data.FeedbackSettings
import com.drawit.app.data.Profile
import com.drawit.app.data.ProgressManager
import com.drawit.app.data.PurchaseResult
import com.drawit.app.data.Reference
import com.drawit.app.data.ReminderSettings
import com.drawit.app.data.StreakOutcome
import com.drawit.app.data.StreakState
import com.drawit.app.data.WalletState
import com.drawit.app.data.WelcomeGift
import com.drawit.app.feedback.Feedback
import com.drawit.app.model.Economy
import com.drawit.app.model.FlameTier
import com.drawit.app.reminder.Notifier
import com.drawit.app.reminder.ReminderScheduler
import com.drawit.app.ui.art.AvatarCatalog
import com.drawit.app.ui.art.AvatarItem
import com.drawit.app.ui.art.AvatarLook
import com.drawit.app.ui.art.AvatarSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DrawItApp

    val drawings: StateFlow<List<Drawing>> = app.drawings.drawings
    val settings: StateFlow<ReminderSettings> = app.settings.settings
    val feedbackSettings: StateFlow<FeedbackSettings> = app.settings.feedback
    val streak: StateFlow<StreakState> = app.streak.state
    val wallet: StateFlow<WalletState> = app.wallet.state
    val profile: StateFlow<Profile> = app.profile.profile
    val references: StateFlow<List<Reference>> = app.references.saved
    val todayReference: StateFlow<Reference?> = app.references.today
    val welcome: StateFlow<WelcomeGift?> = app.progress.welcome

    /** The avatar as it is currently dressed. */
    val look: StateFlow<AvatarLook> = app.profile.profile
        .map { lookOf(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, lookOf(app.profile.profile.value))

    /** Set when a freeze was spent or a streak was lost while the app was away. */
    val streakNotice: StateFlow<StreakOutcome?> = app.streak.pendingNotice

    val feedback: Feedback get() = app.feedback

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Set right after a save; the celebration overlay plays it and clears it. */
    private val _celebration = MutableStateFlow<Celebration?>(null)
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

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
                _celebration.value = app.progress.recordDrawing(app.drawings.drawings.value)
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

    fun clearCelebration() {
        _celebration.value = null
    }

    /** What the next save will earn, shown on the save button. */
    fun previewReward(): Int {
        val state = streak.value
        val rewards = wallet.value.rewards
        val today = LocalDate.now()
        return if (!state.drewOn(today)) {
            Economy.DAILY_BASE + Economy.streakBonus(state.current + 1)
        } else {
            val count = if (rewards.extrasDay == today.toEpochDay()) rewards.extrasCount else 0
            if (count < Economy.MAX_EXTRAS_PER_DAY) Economy.EXTRA_DRAWING else 0
        }
    }

    // -- streak -----------------------------------------------------------

    fun settleStreak() {
        app.streak.settle()
    }

    fun clearStreakNotice() = app.streak.clearNotice()

    fun drewToday(state: StreakState = streak.value): Boolean = state.drewOn(LocalDate.now())

    fun tier(state: StreakState = streak.value): FlameTier = FlameTier.forStreak(state.current)

    /** The set of local days, as epoch days, that have at least one drawing. */
    fun drawnDays(list: List<Drawing> = drawings.value): Set<Long> = ProgressManager.drawnDays(list)

    fun buyFreeze(): Boolean {
        val ok = app.progress.buyFreeze()
        if (ok) {
            feedback.play(Feedback.Sound.Freeze)
            feedback.buzz(Feedback.Buzz.Success)
        } else {
            feedback.error()
        }
        return ok
    }

    // -- avatar and shop --------------------------------------------------------

    fun owns(item: AvatarItem): Boolean = item.isStarter || item.id in profile.value.owned

    fun isEquipped(item: AvatarItem): Boolean = look.value[item.slot] == item.id

    fun buy(item: AvatarItem): PurchaseResult {
        val result = app.progress.buy(item)
        when (result) {
            is PurchaseResult.Bought -> {
                app.progress.equip(item)
                feedback.play(Feedback.Sound.Purchase)
                feedback.buzz(Feedback.Buzz.Success)
            }
            PurchaseResult.NotEnoughInk -> feedback.error()
            else -> Unit
        }
        return result
    }

    fun equip(item: AvatarItem) {
        app.progress.equip(item)
        feedback.play(Feedback.Sound.Equip)
        feedback.buzz(Feedback.Buzz.Pop)
    }

    fun unequip(slot: AvatarSlot) {
        app.progress.unequip(slot)
        feedback.play(Feedback.Sound.Pop)
    }

    fun markSeen(ids: Collection<String>) = app.progress.markSeen(ids)

    fun rename(name: String) = app.progress.rename(name)

    fun clearWelcome() = app.progress.clearWelcome()

    // -- references -------------------------------------------------------------

    fun toggleReference(ref: Reference) {
        val wasSaved = app.references.isSaved(ref.id)
        app.references.toggle(ref)
        if (!wasSaved) {
            feedback.play(Feedback.Sound.Pop)
            feedback.buzz(Feedback.Buzz.Pop)
        } else {
            feedback.tick()
        }
    }

    fun isReferenceSaved(id: String) = references.value.any { it.id == id }

    fun drawFromReference(ref: Reference?) {
        app.references.setToday(ref)
        if (ref != null) {
            feedback.play(Feedback.Sound.Ding, semitones = 5f, gain = 0.6f)
            feedback.buzz(Feedback.Buzz.Pop)
        }
    }

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

    /** False when Android's battery saver may stop delivering alarms in the background. */
    fun isBatteryExempt(): Boolean = ReminderScheduler.isBatteryExempt(app)

    /**
     * Arms a real alarm [seconds] from now through the same AlarmManager path the
     * reminder uses, so it can be verified with the app closed and the screen off.
     * The receiver puts the normal schedule back afterwards.
     */
    fun scheduleTestAlarm(seconds: Long = 30) {
        ReminderScheduler.armAt(app, System.currentTimeMillis() + seconds * 1000)
    }

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
                    val what = if (r.drawings == 1) "1 drawing" else "${r.drawings} drawings"
                    "Exported $what, your streak, ink and avatar ($size)."
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
                        if (r.inkRestored > 0) append(" ${r.inkRestored} ink restored.")
                        if (r.inkRestored < 0) append(" ${-r.inkRestored} ink you'd already spent on your other phone came off.")
                        if (r.inkAlreadyAdded) append(" Its ink was already added by an earlier import.")
                        if (r.avatarRestored) append(" Your avatar is back.")
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

    companion object {
        fun lookOf(p: Profile): AvatarLook = AvatarLook(
            p.equipped.mapNotNull { (slot, id) ->
                val s = runCatching { AvatarSlot.valueOf(slot) }.getOrNull() ?: return@mapNotNull null
                if (AvatarCatalog.get(id)?.slot == s) s to id else null
            }.toMap()
        )
    }
}
