package com.drawit.app.model

/**
 * The streak flame evolves as the streak grows. Each tier is a new look for
 * the flame everywhere it appears, and crossing into one is celebrated.
 */
enum class FlameTier(val minStreak: Int, val title: String, val tagline: String) {
    Ember(0, "Ember", "Draw today to light it."),
    Spark(1, "Spark", "Every streak starts with one."),
    Blaze(7, "Blaze", "A full week. It's catching."),
    Inferno(14, "Inferno", "Two weeks of heat."),
    Azure(28, "Azure Flame", "So hot it burns blue."),
    Cosmic(50, "Cosmic Flame", "Fifty days. Out of this world."),
    Solar(100, "Solar Flame", "A hundred days. You're a star."),
    Phoenix(200, "Phoenix", "Two hundred days. Legendary."),
    Eternal(365, "Eternal Flame", "A whole year. It never goes out.");

    val next: FlameTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forStreak(streak: Int): FlameTier =
            entries.last { streak >= it.minStreak }

        /** The tier newly reached by moving from [before] to [after], if any. */
        fun evolvedBetween(before: Int, after: Int): FlameTier? {
            val a = forStreak(before)
            val b = forStreak(after)
            return if (b.ordinal > a.ordinal && b != Spark) b else null
        }
    }
}
