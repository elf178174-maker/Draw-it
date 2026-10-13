package com.drawit.app.data

import android.content.Context
import com.drawit.app.model.RewardKind
import com.drawit.app.model.RewardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** One entry in the ink history shown on the profile. */
data class InkTransaction(val kind: RewardKind, val amount: Int, val label: String, val at: Long)

data class WalletState(
    val balance: Int = 0,
    val earned: Int = 0,
    val spent: Int = 0,
    val rewards: RewardState = RewardState(),
    /** Most recent first, capped at [WalletRepository.HISTORY_LIMIT]. */
    val history: List<InkTransaction> = emptyList(),
    /** False until the one-off welcome / back-pay has run. */
    val initialized: Boolean = false
)

/**
 * Ink, the currency. Earned by drawing, spent in the shop. Lives in the same
 * SharedPreferences file as the streak so a backup can carry both.
 */
class WalletRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    @Synchronized
    fun credit(amount: Int, kind: RewardKind, label: String, rewards: RewardState? = null) {
        if (amount <= 0 && rewards == null) return
        val s = _state.value
        val updated = s.copy(
            balance = s.balance + amount.coerceAtLeast(0),
            earned = s.earned + amount.coerceAtLeast(0),
            rewards = rewards ?: s.rewards,
            history = if (amount > 0) prepend(s.history, InkTransaction(kind, amount, label, now())) else s.history
        )
        commit(updated)
    }

    /** Spends [amount] if there is enough. Returns false and changes nothing otherwise. */
    @Synchronized
    fun debit(amount: Int, kind: RewardKind, label: String): Boolean {
        val s = _state.value
        if (amount <= 0 || s.balance < amount) return false
        commit(
            s.copy(
                balance = s.balance - amount,
                spent = s.spent + amount,
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

    /** Replaces everything, used when restoring a backup onto a fresh phone. */
    @Synchronized
    fun restore(state: WalletState) = commit(state.copy(initialized = true))

    private fun prepend(list: List<InkTransaction>, t: InkTransaction) = (listOf(t) + list).take(HISTORY_LIMIT)

    private fun now() = System.currentTimeMillis()

    private fun commit(state: WalletState) {
        prefs.edit()
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
            .apply()
        _state.value = state
    }

    private fun read() = WalletState(
        balance = prefs.getInt(KEY_BALANCE, 0),
        earned = prefs.getInt(KEY_EARNED, 0),
        spent = prefs.getInt(KEY_SPENT, 0),
        rewards = RewardState(
            lastRewardDay = prefs.getLong(KEY_LAST_REWARD, -1L),
            extrasDay = prefs.getLong(KEY_EXTRAS_DAY, -1L),
            extrasCount = prefs.getInt(KEY_EXTRAS_COUNT, 0),
            goalRewardedUpTo = prefs.getInt(KEY_GOAL_UP_TO, 0),
            perfectWeekPaid = prefs.getLong(KEY_PERFECT_WEEK, -1L)
        ),
        history = runCatching { historyFromJson(JSONArray(prefs.getString(KEY_HISTORY, "[]"))) }.getOrDefault(emptyList()),
        initialized = prefs.getBoolean(KEY_INITIALIZED, false)
    )

    companion object {
        const val HISTORY_LIMIT = 40

        private const val KEY_BALANCE = "wallet_balance"
        private const val KEY_EARNED = "wallet_earned"
        private const val KEY_SPENT = "wallet_spent"
        private const val KEY_LAST_REWARD = "wallet_last_reward_day"
        private const val KEY_EXTRAS_DAY = "wallet_extras_day"
        private const val KEY_EXTRAS_COUNT = "wallet_extras_count"
        private const val KEY_GOAL_UP_TO = "wallet_goal_up_to"
        private const val KEY_PERFECT_WEEK = "wallet_perfect_week"
        private const val KEY_HISTORY = "wallet_history"
        private const val KEY_INITIALIZED = "wallet_initialized"
        private const val KEY_WELCOME = "wallet_welcome_pending"

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
