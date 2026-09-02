package com.drawit.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.drawit.app.MainActivity
import com.drawit.app.R
import com.drawit.app.data.StreakRepository
import java.time.LocalDate

object Notifier {

    const val CHANNEL_ID = "drawing_reminders"
    private const val NOTIFICATION_ID = 7301

    private val lines = listOf(
        "Time to draw" to "Twenty minutes and a page is all it takes.",
        "Your page is waiting" to "Draw something small. It still counts.",
        "Time to draw" to "Start with one line and see where it goes.",
        "Pencil time" to "No pressure, no perfect. Just draw.",
        "Your page is waiting" to "Today's drawing is the only one that matters."
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** A nudge that knows what is at stake. */
    private fun streakLine(state: com.drawit.app.data.StreakState): Pair<String, String> {
        val drewToday = state.drewOn(LocalDate.now())
        return when {
            drewToday ->
                "Already done today" to "Your streak is safe. Draw again if you feel like it."
            state.current >= 2 && state.freezes == 0 ->
                "Your ${state.current}-day streak is on the line" to
                    "No freezes left. Draw today or it goes back to zero."
            state.current >= 2 ->
                "Keep your ${state.current}-day streak" to
                    "One drawing keeps it alive. You have ${state.freezes} " +
                    (if (state.freezes == 1) "freeze" else "freezes") + " spare, but why spend one?"
            state.current == 1 ->
                "Day two?" to "You drew yesterday. Do it again and a streak starts."
            else -> lines.random()
        }
    }

    fun show(context: Context, preview: Boolean = false) {
        ensureChannel(context)
        if (!hasPermission(context)) return

        val (title, body) = if (preview) {
            "Time to draw" to "This is what your reminder will look like."
        } else {
            // Read-only: settling the streak is the app's job, not the alarm's.
            streakLine(StreakRepository(context).state.value)
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addDrawing = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_ADD_DRAWING)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFFF5001.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .addAction(R.drawable.ic_camera, "Add today's drawing", addDrawing)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }
}
