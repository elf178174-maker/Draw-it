package com.drawit.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The streak, its freezes, and everything needed to redraw the last few weeks.
 *
 * Days are stored as epoch days in the device's local calendar, so the streak
 * turns over at local midnight rather than at UTC.
 */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    /** Epoch day of the most recent day that counted, or [NEVER] before the first drawing. */
    val lastCountedDay: Long = NEVER,
    val freezes: Int = 1,
    /** Epoch day of the Monday of the week freezes were last granted for. */
    val lastGrantWeek: Long = NEVER,
    val freezesUsedTotal: Int = 0,
    /** Days a freeze covered, kept so the calendar can show them. */
    val frozenDays: Set<Long> = emptySet()
) {
    val hasStreak: Boolean get() = current > 0

    fun drewOn(day: LocalDate): Boolean =
        lastCountedDay != NEVER && day.toEpochDay() <= lastCountedDay

    companion object {
        const val NEVER = -1L

        /** Freezes are capped so the streak still has to mean something. */
        const val MAX_FREEZES = 3
        const val FREEZES_PER_WEEK = 1

        /** Days worth of frozen-day history to keep; older entries are pruned. */
        const val FROZEN_HISTORY_DAYS = 120L

        val MILESTONES = listOf(3, 7, 14, 30, 50, 75, 100, 150, 200, 250, 300, 365)

        fun isMilestone(streak: Int): Boolean = streak in MILESTONES

        /** The next milestone above [streak], or null once they are all passed. */
        fun nextMilestone(streak: Int): Int? = MILESTONES.firstOrNull { it > streak }

        fun milestoneLabel(streak: Int): String = when (streak) {
            3 -> "Three days"
            7 -> "A full week"
            14 -> "Two weeks"
            30 -> "A whole month"
            50 -> "Fifty days"
            75 -> "Seventy-five"
            100 -> "One hundred days"
            150 -> "A hundred and fifty"
            200 -> "Two hundred days"
            250 -> "Two hundred and fifty"
            300 -> "Three hundred days"
            365 -> "A year of drawing"
            else -> "$streak days"
        }

        fun weekKey(date: LocalDate): Long = date.with(DayOfWeek.MONDAY).toEpochDay()

        fun initial(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "M"
            DayOfWeek.TUESDAY -> "T"
            DayOfWeek.WEDNESDAY -> "W"
            DayOfWeek.THURSDAY -> "T"
            DayOfWeek.FRIDAY -> "F"
            DayOfWeek.SATURDAY -> "S"
            DayOfWeek.SUNDAY -> "S"
        }

        fun daysBetween(from: Long, to: LocalDate): Long =
            ChronoUnit.DAYS.between(LocalDate.ofEpochDay(from), to)
    }
}

/** What changed the last time the streak was settled, so the UI can react once. */
data class StreakOutcome(
    val state: StreakState,
    val freezesSpent: Int = 0,
    val streakLost: Boolean = false,
    val extended: Boolean = false,
    val milestone: Boolean = false
)
