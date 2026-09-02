package com.drawit.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.drawit.app.data.SettingsRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val app = context.applicationContext
        Notifier.show(app)
        // Re-arm for the next selected day.
        val settings = SettingsRepository(app).settings.value
        ReminderScheduler.apply(app, settings)
    }

    companion object {
        const val ACTION_REMIND = "com.drawit.app.action.REMIND"
    }
}
