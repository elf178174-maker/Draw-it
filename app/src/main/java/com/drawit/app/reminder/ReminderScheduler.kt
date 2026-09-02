package com.drawit.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.drawit.app.data.ReminderSettings
import java.util.Calendar

/**
 * Owns the single repeating alarm behind the reminder. The alarm is one-shot and
 * re-armed each time it fires, which keeps day-of-week schedules exact across
 * daylight-saving changes.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 4201

    fun apply(context: Context, settings: ReminderSettings) {
        if (settings.enabled) schedule(context, settings) else cancel(context)
    }

    fun schedule(context: Context, settings: ReminderSettings) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = nextTrigger(settings) ?: return
        val pending = pendingIntent(context)

        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        if (canBeExact) {
            runCatching {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }.onFailure {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } else {
            // Without the exact-alarm permission the system may shift this by a few
            // minutes; the reminder still lands close to the chosen time.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        manager.cancel(pendingIntent(context))
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return manager.canScheduleExactAlarms()
    }

    /** Next moment the reminder should fire, or null when no days are selected. */
    fun nextTrigger(settings: ReminderSettings, from: Long = System.currentTimeMillis()): Long? {
        if (settings.days.isEmpty()) return null
        val base = Calendar.getInstance().apply {
            timeInMillis = from
            set(Calendar.HOUR_OF_DAY, settings.hour)
            set(Calendar.MINUTE, settings.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (offset in 0..7) {
            val candidate = base.clone() as Calendar
            candidate.add(Calendar.DAY_OF_YEAR, offset)
            if (candidate.timeInMillis > from && settings.days.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
