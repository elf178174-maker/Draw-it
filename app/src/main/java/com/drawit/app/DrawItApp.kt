package com.drawit.app

import android.app.Application
import com.drawit.app.data.BackupManager
import com.drawit.app.data.DrawingRepository
import com.drawit.app.data.ProfileRepository
import com.drawit.app.data.ProgressManager
import com.drawit.app.data.ReferenceRepository
import com.drawit.app.data.SettingsRepository
import com.drawit.app.data.StreakRepository
import com.drawit.app.data.WalletRepository
import com.drawit.app.feedback.Feedback
import com.drawit.app.reminder.Notifier
import com.drawit.app.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DrawItApp : Application() {

    lateinit var drawings: DrawingRepository
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var streak: StreakRepository
        private set
    lateinit var wallet: WalletRepository
        private set
    lateinit var profile: ProfileRepository
        private set
    lateinit var references: ReferenceRepository
        private set
    lateinit var progress: ProgressManager
        private set
    lateinit var backups: BackupManager
        private set
    lateinit var feedback: Feedback
        private set

    val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        drawings = DrawingRepository(this)
        settings = SettingsRepository(this)
        streak = StreakRepository(this)
        wallet = WalletRepository(this)
        profile = ProfileRepository(this)
        references = ReferenceRepository(this)
        progress = ProgressManager(streak, wallet, profile)
        backups = BackupManager(this, drawings, streak, wallet, profile, references, progress)
        feedback = Feedback(this).apply {
            val prefs = settings.feedback.value
            soundEnabled = prefs.soundEnabled
            hapticsEnabled = prefs.hapticsEnabled
        }
        Notifier.ensureChannel(this)

        // Pay for any days missed while the app was closed.
        streak.settle()
        scope.launch {
            drawings.load()
            // First run of this version: welcome gift and back-pay for the album.
            progress.migrateIfNeeded(drawings.snapshot())
            // Wallets from the first 2.0 builds get their ink filed by source.
            progress.splitLegacyWallet(drawings.snapshot())
        }
        // Make sure the alarm exists even if it was dropped by the system.
        ReminderScheduler.apply(this, settings.settings.value)
    }
}
