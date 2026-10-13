package com.drawit.app.data

import android.content.Context
import com.drawit.app.model.RewardKind
import com.drawit.app.model.RewardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One entry in the ink history shown on the profile. */
data class InkTransaction(val kind: RewardKind, val amount: Int, val label: String, val at: Long)

/**
 * The wallet. Ink is counted per source rather than as one total, so wallets
 * from different phones and backups can be merged without counting anything
 * twice: each source only ever grows, and a merge keeps the larger count of
 * each. Sources are
 *  - this wallet's [id], for goals, refunds and everything it spends;
 *  - `day:<epochDay>` for a day's drawing ink, however many phones paid it;
 *  - `week:<monday>` for a perfect week;
 *  - `welcome` and `goal:<n>` for the one-off welcome gift and back-paid goals.
 */
data class WalletState(
    val earnedBy: Map<String, Int> = emptyMap(),
    val spentBy: Map<String, Int> = emptyMap(),
    val rewards: RewardState = RewardState(),
    /** Most recent first, capped at [WalletRepository.HISTORY_LIMIT]. */
    val history: List<InkTransaction> = emptyList(),
    /** False until the one-off welcome / back-pay has run. */
    val initialized: Boolean = false,
    /** Stable id for this wallet: the source its own earnings and spending go under. */
    val id: String = ""
) {
    val earned: Int get() = earnedBy.values.sum()
    val spent: Int get() = spentBy.values.sum()
    val balance: Int get() = (earned - spent).coerceAtLeast(0)

    companion object {
        /** Everything either wallet has seen, each source counted once. Keeps [local]'s id. */
        fun merge(local: WalletState, incoming: WalletState): WalletState = local.copy(
            earnedBy = maxEach(local.earnedBy, incoming.earnedBy),
            spentBy = maxEach(local.spentBy, incoming.spentBy),
            rewards = mergeRewards(local.rewards, incoming.rewards),
            history = (local.history + incoming.history).distinct()
                .sortedByDescending { it.at }
                .take(WalletRepository.HISTORY_LIMIT),
            // Still this phone's call: its one-off welcome is safe to run after.
            initialized = local.initialized
        )

        private fun maxEach(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> =
            (a.keys + b.keys).associateWith { maxOf(a[it] ?: 0, b[it] ?: 0) }

        /**
         * Goals stay this phone's: they belong to the streak run, which the
         * import lines up separately once the streaks are merged.
         */
        private fun mergeRewards(a: RewardState, b: RewardState): RewardState {
            val extras = when {
                b.extrasDay > a.extrasDay -> b.extrasDay to b.extrasCount
                b.extrasDay == a.extrasDay -> a.extrasDay to maxOf(a.extrasCount, b.extrasCount)
                else -> a.extrasDay to a.extrasCount
            }
            return a.copy(
                lastRewardDay = maxOf(a.lastRewardDay, b.lastRewardDay),
                extrasDay = extras.first,
                extrasCount = extras.second,
                perfectWeekPaid = maxOf(a.perfectWeekPaid, b.perfectWeekPaid)
            )
        }
    }
}

/**
 * Ink, the currency. Earned by drawing, spent in the shop. Lives in the same
 * SharedPreferences file as the streak so a backup can carry both.
 */
class WalletRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    /** Adds [amount] to [source], or to this wallet's own count when no source is given. */
    @Synchronized
    fun credit(amount: Int, kind: RewardKind, label: String, source: String? = null) {
        if (amount <= 0) return
        val s = _state.value
        val key = source ?: s.id
        commit(
            s.copy(
                earnedBy = s.earnedBy + (key to (s.earnedBy[key] ?: 0) + amount),
                history = prepend(s.history, InkTransaction(kind, amount, label, now()))
            )
        )
    }

    /**
     * Raises each source to at least its amount, for one-off payments that
     * must never be paid twice (the welcome gift, back-pay for past days).
     * Returns how much ink that actually added; one history line covers it.
     */
    @Synchronized
    fun creditOnce(amounts: Map<String, Int>, kind: RewardKind, label: String): Int {
        val s = _state.value
        var added = 0
        val earned = s.earnedBy.toMutableMap()
        amounts.forEach { (key, amount) ->
            val have = earned[key] ?: 0
            if (amount > have) {
                added += amount - have
                earned[key] = amount
            }
        }
        if (added > 0) {
            commit(s.copy(earnedBy = earned, history = prepend(s.history, InkTransaction(kind, added, label, now()))))
        }
        return added
    }

    /** Spends [amount] if there is enough. Returns false and changes nothing otherwise. */
    @Synchronized
    fun debit(amount: Int, kind: RewardKind, label: String): Boolean {
        val s = _state.value
        if (amount <= 0 || s.balance < amount) return false
        commit(
            s.copy(
                spentBy = s.spentBy + (s.id to (s.spentBy[s.id] ?: 0) + amount),
                history = prepend(s.history, InkTransaction(kind, -amount, label, now()))
            )
        )
        return true
    }

    @Synchronized
    fun updateRewards(rewards: RewardState) = commit(_state.value.copy(rewards = rewards))

    @Synchronized
    fun markInitialized() = commit(_state.value.copy(initialized = true))

    /** The welcome gift still waiting to be shown, as JSON; kept until it has been seen. */
    fun pendingWelcome(): String? = prefs.getString(KEY_WELCOME, null)

    fun savePendingWelcome(json: String?) {
        prefs.edit().apply { if (json == null) remove(KEY_WELCOME) else putString(KEY_WELCOME, json) }.apply()
    }

    /**
     * True while this wallet is still one lump of ink from the first 2.0
     * builds, before ink was counted per source. [splitLegacy] fixes that.
     */
    fun hasLegacyLump(): Boolean = prefs.getBoolean(KEY_LEGACY, false)

    /**
     * Re-files a legacy lump under the sources it was really paid for, so
     * later merges can recognise it. [pieces] come out of this wallet's own
     * count; whatever doesn't fit stays its own.
     */
    @Synchronized
    fun splitLegacy(pieces: Map<String, Int>) {
        val s = _state.value
        val (moved, rest) = splitLump(s.earnedBy[s.id] ?: 0, pieces)
        val earned = s.earnedBy - s.id + moved + (if (rest > 0) mapOf(s.id to rest) else emptyMap())
        prefs.edit().remove(KEY_LEGACY).apply()
        commit(s.copy(earnedBy = maxEach(s.earnedBy - s.id, earned)))
    }

    /** Folds a wallet from a backup into this one. Returns the change in balance. */
    @Synchronized
    fun mergeFrom(incoming: WalletState): Int {
        val s = _state.value
        val merged = WalletState.merge(s, incoming)
        commit(merged)
        return merged.balance - s.balance
    }

    private fun maxEach(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> =
        (a.keys + b.keys).associateWith { maxOf(a[it] ?: 0, b[it] ?: 0) }

    private fun prepend(list: List<InkTransaction>, t: InkTransaction) = (listOf(t) + list).take(HISTORY_LIMIT)

    private fun now() = System.currentTimeMillis()

    private fun commit(state: WalletState) {
        prefs.edit()
            .putString(KEY_EARNED_BY, countsToJson(state.earnedBy).toString())
            .putString(KEY_SPENT_BY, countsToJson(state.spentBy).toString())
            // Plain totals too, for anyone reading the prefs file by hand.
            .putInt(KEY_BALANCE, state.balance)
            .putInt(KEY_EARNED, state.earned)
            .putInt(KEY_SPENT, state.spent)
            .putLong(KEY_LAST_REWARD, state.rewards.lastRewardDay)
            .putLong(KEY_EXTRAS_DAY, state.rewards.extrasDay)
            .putInt(KEY_EXTRAS_COUNT, state.rewards.extrasCount)
            .putInt(KEY_GOAL_UP_TO, state.rewards.goalRewardedUpTo)
            .putLong(KEY_PERFECT_WEEK, state.rewards.perfectWeekPaid)
            .putString(KEY_HISTORY, historyToJson(state.history).toString())
            .putBoolean(KEY_INITIALIZED, state.initialized)
            .putString(KEY_ID, state.id)
            .apply()
        _state.value = state
    }

    private fun read(): WalletState {
        val id = prefs.getString(KEY_ID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_ID, it).apply() }
        val earnedBy = prefs.getString(KEY_EARNED_BY, null)?.let { runCatching { countsFromJson(JSONObject(it)) }.getOrNull() }
        if (earnedBy == null && prefs.getInt(KEY_EARNED, 0) > 0) {
            // Written by a build that kept one total: flag it until it has been split.
            prefs.edit().putBoolean(KEY_LEGACY, true).apply()
        }
        val spentBy = prefs.getString(KEY_SPENT_BY, null)?.let { runCatching { countsFromJson(JSONObject(it)) }.getOrNull() }
        return WalletState(
            // Wallets from before ink was counted per source keep their totals as their own.
            earnedBy = earnedBy ?: legacyCounts(id, prefs.getInt(KEY_EARNED, 0)),
            spentBy = spentBy ?: legacyCounts(id, prefs.getInt(KEY_SPENT, 0)),
            rewards = RewardState(
                lastRewardDay = prefs.getLong(KEY_LAST_REWARD, -1L),
                extrasDay = prefs.getLong(KEY_EXTRAS_DAY, -1L),
                extrasCount = prefs.getInt(KEY_EXTRAS_COUNT, 0),
                goalRewardedUpTo = prefs.getInt(KEY_GOAL_UP_TO, 0),
                perfectWeekPaid = prefs.getLong(KEY_PERFECT_WEEK, -1L)
            ),
            history = runCatching { historyFromJson(JSONArray(prefs.getString(KEY_HISTORY, "[]"))) }.getOrDefault(emptyList()),
            initialized = prefs.getBoolean(KEY_INITIALIZED, false),
            id = id
        )
    }

    companion object {
        const val HISTORY_LIMIT = 40

        const val WELCOME_SOURCE = "welcome"
        fun daySource(epochDay: Long) = "day:$epochDay"
        fun weekSource(monday: Long) = "week:$monday"
        fun goalSource(goal: Int) = "goal:$goal"

        private const val KEY_BALANCE = "wallet_balance"
        private const val KEY_EARNED = "wallet_earned"
        private const val KEY_SPENT = "wallet_spent"
        private const val KEY_EARNED_BY = "wallet_earned_by"
        private const val KEY_SPENT_BY = "wallet_spent_by"
        private const val KEY_LAST_REWARD = "wallet_last_reward_day"
        private const val KEY_EXTRAS_DAY = "wallet_extras_day"
        private const val KEY_EXTRAS_COUNT = "wallet_extras_count"
        private const val KEY_GOAL_UP_TO = "wallet_goal_up_to"
        private const val KEY_PERFECT_WEEK = "wallet_perfect_week"
        private const val KEY_HISTORY = "wallet_history"
        private const val KEY_INITIALIZED = "wallet_initialized"
        private const val KEY_WELCOME = "wallet_welcome_pending"
        private const val KEY_ID = "wallet_id"
        private const val KEY_LEGACY = "wallet_legacy_lump"

        /**
         * Shares [lump] out among [pieces] in order, each up to its amount.
         * Returns what each piece got and what was left over.
         */
        fun splitLump(lump: Int, pieces: Map<String, Int>): Pair<Map<String, Int>, Int> {
            var left = lump.coerceAtLeast(0)
            val moved = LinkedHashMap<String, Int>()
            pieces.forEach { (key, amount) ->
                val take = minOf(left, amount.coerceAtLeast(0))
                if (take > 0) {
                    moved[key] = take
                    left -= take
                }
            }
            return moved to left
        }

        fun legacyCounts(source: String, total: Int): Map<String, Int> =
            if (total > 0) mapOf(source to total) else emptyMap()

        fun countsToJson(counts: Map<String, Int>): JSONObject = JSONObject().apply {
            counts.forEach { (k, v) -> if (v > 0) put(k, v) }
        }

        fun countsFromJson(o: JSONObject): Map<String, Int> = buildMap {
            val keys = o.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = o.optInt(k, 0)
                if (k.isNotEmpty() && v > 0) put(k, v)
            }
        }

        fun historyToJson(list: List<InkTransaction>): JSONArray = JSONArray().apply {
            list.forEach {
                put(JSONObject().put("kind", it.kind.name).put("amount", it.amount).put("label", it.label).put("at", it.at))
            }
        }

        fun historyFromJson(array: JSONArray): List<InkTransaction> = (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val kind = runCatching { RewardKind.valueOf(o.optString("kind")) }.getOrNull() ?: return@mapNotNull null
            InkTransaction(kind, o.optInt("amount"), o.optString("label"), o.optLong("at"))
        }
    }
}
