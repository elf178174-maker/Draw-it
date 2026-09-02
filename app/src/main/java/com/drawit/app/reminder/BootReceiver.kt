package com.drawit.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drawit.app.data.SettingsRepository

/** Alarms do not survive a reboot, a time change or an app update, so re-arm them. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val app = context.applicationContext
                Notifier.ensureChannel(app)
                ReminderScheduler.apply(app, SettingsRepository(app).settings.value)
            }
        }
    }
}
