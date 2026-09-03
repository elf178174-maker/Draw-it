package com.drawit.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.drawit.app.MainActivity
import com.drawit.app.data.ReminderSettings
import java.util.Calendar

/**
 * Owns the single alarm behind the reminder. The alarm is one-shot and re-armed
 * each time it fires, which keeps day-of-week schedules exact across
 * daylight-saving changes.
 *
 * Delivery uses [AlarmManager.setAlarmClock] wherever it is allowed. That is the
 * only alarm type Doze will not defer: everything weaker gets parked while the
 * phone is idle and then dumped on you the moment the app next opens, which is
 * not a reminder. The cost is the alarm icon in the status bar, which is a fair
 * trade for the reminder actually arriving.
 */
object ReminderScheduler {

    private const val REQUEST_CODE = 4201
    private const val SHOW_REQUEST_CODE = 4202

    fun apply(context: Context, settings: ReminderSettings) {
        if (settings.enabled) schedule(context, settings) else cancel(context)
    }

    fun schedule(context: Context, settings: ReminderSettings) {
        val triggerAt = nextTrigger(settings) ?: return
        armAt(context, triggerAt)
    }

    /** Arms the reminder alarm for an absolute time. */
    fun armAt(context: Context, triggerAt: Long) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pending = pendingIntent(context)

        if (canScheduleExact(context)) {
            // Best case: a real alarm-clock alarm, which Doze cannot touch.
            val armed = runCatching {
                manager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent(context)),
                    pending
                )
            }.isSuccess
            if (armed) return

            val exact = runCatching {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }.isSuccess
            if (exact) return
        }

        // Last resort. Fires late while the phone is idle, but never not at all.
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        manager.cancel(pendingIntent(context))
    }

    /**
     * True when the system will let us set an exact alarm. Thanks to the
     * USE_EXACT_ALARM permission this is granted on install for API 33+; on
     * Android 12 it is a setting the user has to turn on.
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return manager.canScheduleExactAlarms()
    }

    /**
     * Whether the app is exempt from battery optimisation. Without this, some
     * manufacturers stop delivering alarms to a backgrounded app entirely,
     * whatever the alarm type.
     */
    fun isBatteryExempt(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
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

    /** Where the system sends you if you tap the alarm in the status bar. */
    private fun showIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        SHOW_REQUEST_CODE,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
