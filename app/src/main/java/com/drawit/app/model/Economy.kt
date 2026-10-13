package com.drawit.app.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Everything about how ink is earned, in one place so the numbers stay
 * honest. Pure functions only: the repositories decide when to call them.
 */
object Economy {
    /** Given once, the first time the new version opens. */
    const val WELCOME_GIFT = 100

    /** The first drawing of each day. */
    const val DAILY_BASE = 20

    /** Further drawings on the same day, up to [MAX_EXTRAS_PER_DAY]. */
    const val EXTRA_DRAWING = 5
    const val MAX_EXTRAS_PER_DAY = 3

    /** Every day Monday to Sunday drawn, without a freeze. */
    const val PERFECT_WEEK = 50

    /** A streak freeze in the shop. */
    const val FREEZE_PRICE = 150

    /** Streak goals, in order. Reaching each one pays out once per run. */
    val GOALS = listOf(3, 7, 14, 28, 50, 100, 150, 200, 365, 500, 730, 1000)

    fun goalReward(goal: Int): Int = when (goal) {
        3 -> 25
        7 -> 50
        14 -> 100
        28 -> 200
        50 -> 300
        100 -> 500
        150 -> 600
        200 -> 800
        365 -> 1500
        500 -> 2000
        730 -> 3000
        1000 -> 5000
        else -> 0
    }

    fun goalLabel(goal: Int): String = when (goal) {
        3 -> "Warm-up"
        7 -> "One week"
        14 -> "Two weeks"
        28 -> "Four weeks"
        50 -> "Fifty days"
        100 -> "One hundred"
        150 -> "One fifty"
        200 -> "Two hundred"
        365 -> "One whole year"
        500 -> "Five hundred"
        730 -> "Two years"
        1000 -> "One thousand"
        else -> "$goal days"
    }

    /** The goal being worked towards at [streak], or null past the last one. */
    fun nextGoal(streak: Int): Int? = GOALS.firstOrNull { it > streak }

    /** The last goal at or below [streak], or 0. */
    fun previousGoal(streak: Int): Int = GOALS.lastOrNull { it <= streak } ?: 0

    /** Bonus on top of the daily reward, growing as the flame evolves. */
    fun streakBonus(streak: Int): Int = FlameTier.forStreak(streak).ordinal * 2

    /**
     * What the album was worth before ink existed: the daily reward for every
     * day drawn, extras for the busy days, and every goal the best streak
     * passed. Paid once when upgrading, or when restoring an old backup.
     */
    fun retroactive(drawingDays: List<Long>, best: Int): Int {
        val perDay = drawingDays.groupingBy { it }.eachCount()
        val daily = perDay.size * DAILY_BASE
        val extras = perDay.values.sumOf { minOf(it - 1, MAX_EXTRAS_PER_DAY) } * EXTRA_DRAWING
        val goals = GOALS.filter { it <= best }.sumOf { goalReward(it) }
        return daily + extras + goals
    }

    fun weekKey(date: LocalDate): Long = date.with(DayOfWeek.MONDAY).toEpochDay()
}

enum class RewardKind { Daily, StreakBonus, Extra, Goal, PerfectWeek, Welcome, Retro, Purchase, Freeze, Refund }

data class RewardLine(val kind: RewardKind, val amount: Int, val label: String)

/** The state of the run the rewards care about. */
data class RewardState(
    val lastRewardDay: Long = -1L,
    val extrasDay: Long = -1L,
    val extrasCount: Int = 0,
    /** Highest goal already paid in the current streak run. */
    val goalRewardedUpTo: Int = 0,
    /** Week key of the last perfect week paid. */
    val perfectWeekPaid: Long = -1L
)

data class DrawingReward(
    val lines: List<RewardLine>,
    val state: RewardState,
    /** The goal reached by this drawing, if any. */
    val goalReached: Int? = null,
    /** The flame tier this drawing evolved into, if any. */
    val evolvedTo: FlameTier? = null,
    val perfectWeek: Boolean = false
) {
    val total: Int get() = lines.sumOf { it.amount }
}

object RewardRules {

    /**
     * Works out what one saved drawing earns.
     *
     * [streakBefore] is the settled streak just before the drawing counted and
     * [streakAfter] the streak after it; [extended] says whether this drawing
     * moved the streak on (the first of the day). [drawnDays] are the local
     * epoch days with a drawing, including today's.
     */
    fun forDrawing(
        today: LocalDate,
        streakBefore: Int,
        streakAfter: Int,
        extended: Boolean,
        drawnDays: Set<Long>,
        state: RewardState
    ): DrawingReward {
        val day = today.toEpochDay()
        val lines = ArrayList<RewardLine>()
        var next = state

        // A broken run starts goals over.
        if (streakAfter < next.goalRewardedUpTo) next = next.copy(goalRewardedUpTo = 0)

        val firstToday = extended && next.lastRewardDay != day
        if (firstToday) {
            lines += RewardLine(RewardKind.Daily, Economy.DAILY_BASE, "Today's drawing")
            val bonus = Economy.streakBonus(streakAfter)
            if (bonus > 0) lines += RewardLine(RewardKind.StreakBonus, bonus, "${FlameTier.forStreak(streakAfter).title} bonus")
            next = next.copy(lastRewardDay = day)
        } else {
            val count = if (next.extrasDay == day) next.extrasCount else 0
            if (count < Economy.MAX_EXTRAS_PER_DAY) {
                lines += RewardLine(RewardKind.Extra, Economy.EXTRA_DRAWING, "Extra drawing")
                next = next.copy(extrasDay = day, extrasCount = count + 1)
            }
        }

        var goalReached: Int? = null
        if (extended) {
            Economy.GOALS.filter { it > next.goalRewardedUpTo && it <= streakAfter }.forEach { goal ->
                lines += RewardLine(RewardKind.Goal, Economy.goalReward(goal), "${goal}-day goal")
                goalReached = goal
                next = next.copy(goalRewardedUpTo = goal)
            }
        }

        var perfect = false
        val week = Economy.weekKey(today)
        if (extended && today.dayOfWeek == DayOfWeek.SUNDAY && next.perfectWeekPaid != week) {
            val all = (0..6).all { (week + it) in drawnDays }
            if (all) {
                lines += RewardLine(RewardKind.PerfectWeek, Economy.PERFECT_WEEK, "Perfect week")
                next = next.copy(perfectWeekPaid = week)
                perfect = true
            }
        }

        val evolved = if (extended) FlameTier.evolvedBetween(streakBefore, streakAfter) else null
        return DrawingReward(lines, next, goalReached, evolved, perfect)
    }
}
