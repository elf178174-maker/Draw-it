package com.drawit.app

import android.app.Application
import com.drawit.app.data.BackupManager
import com.drawit.app.data.DrawingRepository
import com.drawit.app.data.SettingsRepository
import com.drawit.app.data.StreakRepository
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
    lateinit var backups: BackupManager
        private set
    lateinit var feedback: Feedback
        private set

    override fun onCreate() {
        super.onCreate()
        drawings = DrawingRepository(this)
        settings = SettingsRepository(this)
        streak = StreakRepository(this)
        backups = BackupManager(this, drawings, streak)
        feedback = Feedback(this).apply {
            val prefs = settings.feedback.value
            soundEnabled = prefs.soundEnabled
            hapticsEnabled = prefs.hapticsEnabled
        }
        Notifier.ensureChannel(this)

        CoroutineScope(SupervisorJob()).launch {
            drawings.load()
        }
        // Pay for any days missed while the app was closed.
        streak.settle()
        // Make sure the alarm exists even if it was dropped by the system.
        ReminderScheduler.apply(this, settings.settings.value)
    }
}
