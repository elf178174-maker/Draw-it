package com.drawit.app.data

import java.util.Calendar

/**
 * @param days days the reminder fires on, using [Calendar.DAY_OF_WEEK] values (1 = Sunday).
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 19,
    val minute: Int = 0,
    val days: Set<Int> = ALL_DAYS
) {
    val isEveryDay: Boolean get() = days.size == ALL_DAYS.size

    companion object {
        val ALL_DAYS: Set<Int> = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        /** Monday-first ordering, which is how the week is shown in the app. */
        val WEEK_ORDER: List<Int> = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY,
            Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        fun shortLabel(day: Int): String = when (day) {
            Calendar.MONDAY -> "Mon"
            Calendar.TUESDAY -> "Tue"
            Calendar.WEDNESDAY -> "Wed"
            Calendar.THURSDAY -> "Thu"
            Calendar.FRIDAY -> "Fri"
            Calendar.SATURDAY -> "Sat"
            else -> "Sun"
        }

        fun initial(day: Int): String = shortLabel(day).take(1)
    }
}
