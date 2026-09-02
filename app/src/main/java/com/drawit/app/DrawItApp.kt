package com.drawit.app

import android.app.Application
import com.drawit.app.data.DrawingRepository
import com.drawit.app.data.SettingsRepository
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

    override fun onCreate() {
        super.onCreate()
        drawings = DrawingRepository(this)
        settings = SettingsRepository(this)
        Notifier.ensureChannel(this)

        CoroutineScope(SupervisorJob()).launch {
            drawings.load()
        }
        // Make sure the alarm exists even if it was dropped by the system.
        ReminderScheduler.apply(this, settings.settings.value)
    }
}
