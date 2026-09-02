package com.drawit.app.ui

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** Formats an hour/minute pair using the device's 12- or 24-hour preference. */
fun formatTime(context: Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return formatTime(context, calendar.timeInMillis)
}

fun formatTime(context: Context, millis: Long): String =
    DateFormat.getTimeFormat(context).format(Date(millis))

fun formatDate(millis: Long): String =
    java.text.SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatShortDate(millis: Long): String =
    java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

fun formatDayAndTime(context: Context, millis: Long): String =
    "${formatShortDate(millis)} · ${formatTime(context, millis)}"

/** "in 3h 12m", "in 4 days" — a soft countdown rather than an exact clock. */
fun formatCountdown(target: Long, now: Long = System.currentTimeMillis()): String {
    val delta = target - now
    if (delta <= 0) return "any moment now"
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta) % 24
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta) % 60
    return when {
        days >= 2 -> "in $days days"
        days == 1L -> "in 1 day ${hours}h"
        hours >= 1 -> "in ${hours}h ${minutes}m"
        minutes >= 1 -> "in ${minutes}m"
        else -> "in under a minute"
    }
}

fun relativeDayLabel(target: Long, now: Long = System.currentTimeMillis()): String {
    val today = startOfDay(now)
    val day = startOfDay(target)
    return when (day) {
        today -> "Today"
        today + TimeUnit.DAYS.toMillis(1) -> "Tomorrow"
        else -> java.text.SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(target))
    }
}

fun greeting(now: Long = System.currentTimeMillis()): String {
    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..21 -> "Good evening"
        else -> "Still up"
    }
}

private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
