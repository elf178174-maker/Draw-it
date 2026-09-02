package com.drawit.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Owns the streak. Two things drive it:
 *
 *  - [settle] is called whenever the app looks at the streak. It grants the
 *    week's freezes and pays for any days missed since the last drawing,
 *    spending freezes if there are enough and resetting the streak if not.
 *  - [recordDrawing] is called when a drawing is saved and moves the streak on
 *    by one, at most once per day.
 */
class StreakRepository(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<StreakState> = _state.asStateFlow()

    /** Set when freezes were spent or the streak was lost, until the UI clears it. */
    private val _pendingNotice = MutableStateFlow<StreakOutcome?>(null)
    val pendingNotice: StateFlow<StreakOutcome?> = _pendingNotice.asStateFlow()

    fun clearNotice() {
        _pendingNotice.value = null
    }

    /** Brings the streak up to date with today. Safe to call as often as you like. */
    fun settle(today: LocalDate = LocalDate.now()): StreakOutcome {
        val outcome = settled(_state.value, today)
        if (outcome.state != _state.value) {
            write(outcome.state)
            _state.value = outcome.state
        }
        if (outcome.freezesSpent > 0 || outcome.streakLost) {
            _pendingNotice.value = outcome
        }
        return outcome
    }

    /**
     * Counts a drawing made on [today]. Returns what happened so the caller can
     * celebrate; a second drawing on the same day changes nothing.
     */
    fun recordDrawing(today: LocalDate = LocalDate.now()): StreakOutcome {
        val settled = settled(_state.value, today).state
        if (settled.lastCountedDay == today.toEpochDay()) {
            // Already counted today — keep any freeze grant, but do not extend.
            if (settled != _state.value) {
                write(settled)
                _state.value = settled
            }
            return StreakOutcome(state = settled)
        }

        val current = settled.current + 1
        val updated = settled.copy(
            current = current,
            best = maxOf(settled.best, current),
            lastCountedDay = today.toEpochDay()
        )
        write(updated)
        _state.value = updated
        return StreakOutcome(
            state = updated,
            extended = true,
            milestone = StreakState.isMilestone(current)
        )
    }

    /** Replaces the whole streak, used when restoring a backup onto a fresh phone. */
    fun restore(state: StreakState) {
        write(state)
        _state.value = state
    }

    fun reload() {
        _state.value = read()
    }

    // -- the actual rules -------------------------------------------------

    private fun settled(state: StreakState, today: LocalDate): StreakOutcome {
        val granted = grantFreezes(state, today)

        if (granted.lastCountedDay == StreakState.NEVER) {
            return StreakOutcome(state = granted)
        }

        val gap = StreakState.daysBetween(granted.lastCountedDay, today)
        // Drew today, drew yesterday, or the clock moved backwards: nothing owed.
        if (gap <= 1L) return StreakOutcome(state = granted)

        val missed = (gap - 1).toInt()
        if (missed <= granted.freezes && granted.current > 0) {
            val covered = (1..missed).map {
                LocalDate.ofEpochDay(granted.lastCountedDay).plusDays(it.toLong()).toEpochDay()
            }
            val state2 = granted.copy(
                freezes = granted.freezes - missed,
                freezesUsedTotal = granted.freezesUsedTotal + missed,
                frozenDays = prune(granted.frozenDays + covered, today),
                // The streak is now held through yesterday without being extended.
                lastCountedDay = today.minusDays(1).toEpochDay()
            )
            return StreakOutcome(state = state2, freezesSpent = missed)
        }

        val lost = granted.current > 0
        return StreakOutcome(
            state = granted.copy(current = 0, frozenDays = prune(granted.frozenDays, today)),
            streakLost = lost
        )
    }

    /** One freeze for each whole week gone by, up to the cap. */
    private fun grantFreezes(state: StreakState, today: LocalDate): StreakState {
        val thisWeek = StreakState.weekKey(today)
        if (state.lastGrantWeek == StreakState.NEVER) {
            return state.copy(lastGrantWeek = thisWeek)
        }
        if (thisWeek <= state.lastGrantWeek) return state

        val weeks = ((thisWeek - state.lastGrantWeek) / 7).toInt().coerceAtLeast(1)
        val earned = weeks * StreakState.FREEZES_PER_WEEK
        return state.copy(
            freezes = (state.freezes + earned).coerceAtMost(StreakState.MAX_FREEZES),
            lastGrantWeek = thisWeek
        )
    }

    private fun prune(days: Set<Long>, today: LocalDate): Set<Long> {
        val cutoff = today.toEpochDay() - StreakState.FROZEN_HISTORY_DAYS
        return days.filter { it >= cutoff }.toSet()
    }

    // -- persistence ------------------------------------------------------

    private fun read() = StreakState(
        current = prefs.getInt(KEY_CURRENT, 0),
        best = prefs.getInt(KEY_BEST, 0),
        lastCountedDay = prefs.getLong(KEY_LAST_DAY, StreakState.NEVER),
        freezes = prefs.getInt(KEY_FREEZES, 1).coerceIn(0, StreakState.MAX_FREEZES),
        lastGrantWeek = prefs.getLong(KEY_GRANT_WEEK, StreakState.NEVER),
        freezesUsedTotal = prefs.getInt(KEY_FREEZES_USED, 0),
        frozenDays = prefs.getStringSet(KEY_FROZEN, null)
            ?.mapNotNull { it.toLongOrNull() }?.toSet()
            ?: emptySet()
    )

    private fun write(state: StreakState) {
        prefs.edit()
            .putInt(KEY_CURRENT, state.current)
            .putInt(KEY_BEST, state.best)
            .putLong(KEY_LAST_DAY, state.lastCountedDay)
            .putInt(KEY_FREEZES, state.freezes)
            .putLong(KEY_GRANT_WEEK, state.lastGrantWeek)
            .putInt(KEY_FREEZES_USED, state.freezesUsedTotal)
            .putStringSet(KEY_FROZEN, state.frozenDays.map { it.toString() }.toSet())
            .apply()
    }

    private companion object {
        const val KEY_CURRENT = "streak_current"
        const val KEY_BEST = "streak_best"
        const val KEY_LAST_DAY = "streak_last_day"
        const val KEY_FREEZES = "streak_freezes"
        const val KEY_GRANT_WEEK = "streak_grant_week"
        const val KEY_FREEZES_USED = "streak_freezes_used"
        const val KEY_FROZEN = "streak_frozen_days"
    }
}
