package com.drawit.app.data

import com.drawit.app.model.DrawingReward
import com.drawit.app.model.Economy
import com.drawit.app.model.RewardKind
import com.drawit.app.model.RewardRules
import com.drawit.app.ui.art.AvatarCatalog
import com.drawit.app.ui.art.AvatarItem
import com.drawit.app.ui.art.AvatarSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Everything that happened when a drawing was saved, for the celebration. */
data class Celebration(
    val outcome: StreakOutcome,
    val streakBefore: Int,
    val reward: DrawingReward,
    val balanceBefore: Int,
    val balanceAfter: Int,
    /** Avatar pieces unlocked by this drawing's streak. */
    val unlocked: List<AvatarItem>,
    val isRecord: Boolean
) {
    val extended: Boolean get() = outcome.extended
}

/** The one-off gift shown the first time the new version opens. */
data class WelcomeGift(val welcome: Int, val backPay: Int, val drawings: Int, val unlocked: List<AvatarItem>) {
    val total: Int get() = welcome + backPay
}

sealed interface PurchaseResult {
    data class Bought(val item: AvatarItem, val balance: Int) : PurchaseResult
    data object NotEnoughInk : PurchaseResult
    data object AlreadyOwned : PurchaseResult
    data object NotForSale : PurchaseResult
}

/**
 * Ties a saved drawing to everything that grows from it: the streak, ink,
 * goals, the evolving flame and streak-reward avatar pieces.
 */
class ProgressManager(
    private val streak: StreakRepository,
    private val wallet: WalletRepository,
    private val profile: ProfileRepository
) {

    // Kept on disk until seen: the migration can run in a process started by
    // the update or boot receiver, long before the app is opened.
    private val _welcome = MutableStateFlow(readPendingWelcome())
    val welcome: StateFlow<WelcomeGift?> = _welcome.asStateFlow()

    fun clearWelcome() {
        _welcome.value = null
        wallet.savePendingWelcome(null)
    }

    private fun readPendingWelcome(): WelcomeGift? {
        val raw = wallet.pendingWelcome() ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val ids = o.optJSONArray("unlocked")
            WelcomeGift(
                welcome = o.optInt("welcome"),
                backPay = o.optInt("backPay"),
                drawings = o.optInt("drawings"),
                unlocked = if (ids == null) emptyList() else (0 until ids.length()).mapNotNull { AvatarCatalog.get(ids.optString(it)) }
            )
        }.getOrNull()
    }

    private fun WelcomeGift.toJson(): String = JSONObject()
        .put("welcome", welcome)
        .put("backPay", backPay)
        .put("drawings", drawings)
        .put("unlocked", JSONArray().apply { unlocked.forEach { put(it.id) } })
        .toString()

    /**
     * True while the wallet holds nothing but the welcome gift. Only then may
     * a backup replace it: otherwise newer ink and purchases would be lost, or
     * the same drawings paid for again.
     */
    fun walletIsPristine(): Boolean {
        val w = wallet.state.value
        return w.spent == 0 && w.earned <= Economy.WELCOME_GIFT
    }

    /** Counts a drawing that was just saved and pays out whatever it earned. */
    @Synchronized
    fun recordDrawing(drawings: List<Drawing>, today: LocalDate = LocalDate.now()): Celebration {
        val settled = streak.settle(today).state
        val before = settled.current
        val outcome = streak.recordDrawing(today)
        val after = outcome.state.current
        val reward = RewardRules.forDrawing(
            today = today,
            streakBefore = before,
            streakAfter = after,
            extended = outcome.extended,
            drawnDays = drawnDays(drawings) + today.toEpochDay(),
            state = wallet.state.value.rewards
        )
        val balanceBefore = wallet.state.value.balance
        reward.lines.forEach { line -> wallet.credit(line.amount, line.kind, line.label) }
        wallet.updateRewards(reward.state)
        val unlocked = if (outcome.extended) unlockStreakRewards() else emptyList()
        return Celebration(
            outcome = outcome,
            streakBefore = before,
            reward = reward,
            balanceBefore = balanceBefore,
            balanceAfter = wallet.state.value.balance,
            unlocked = unlocked,
            // Strictly beyond the old best: matching it isn't a new record.
            isRecord = outcome.extended && after > 1 && after > settled.best
        )
    }

    /**
     * Runs once after upgrading (or on a fresh install): a welcome gift plus
     * back-pay for every drawing already in the album, and any streak rewards
     * the best streak has already earned.
     */
    @Synchronized
    fun migrateIfNeeded(drawings: List<Drawing>) {
        if (wallet.state.value.initialized) return
        val state = streak.state.value
        val backPay = Economy.retroactive(drawings.map { dayOf(it.createdAt) }, state.best)
        wallet.credit(Economy.WELCOME_GIFT, RewardKind.Welcome, "Welcome gift")
        if (backPay > 0) wallet.credit(backPay, RewardKind.Retro, "Your ${drawings.size} past drawings")
        wallet.updateRewards(
            wallet.state.value.rewards.copy(
                goalRewardedUpTo = Economy.previousGoal(state.current),
                lastRewardDay = if (state.drewOn(LocalDate.now())) LocalDate.now().toEpochDay() else -1L
            )
        )
        ensureStarterLook(since = drawings.map { it.createdAt }.filter { it > 0L }.minOrNull())
        val unlocked = unlockStreakRewards()
        val gift = WelcomeGift(Economy.WELCOME_GIFT, backPay, drawings.size, unlocked)
        wallet.savePendingWelcome(gift.toJson())
        wallet.markInitialized()
        _welcome.value = gift
    }

    /**
     * Restores ink from a backup onto a fresh phone. Old backups have no
     * wallet, so their drawings are back-paid instead. Returns the balance.
     */
    @Synchronized
    fun restoreWallet(saved: WalletState?, drawings: List<Drawing>, savedStreak: StreakState?): Int {
        if (saved != null) {
            wallet.restore(saved)
            return saved.balance
        }
        val best = savedStreak?.best ?: 0
        val current = savedStreak?.current ?: 0
        val backPay = Economy.retroactive(drawings.map { dayOf(it.createdAt) }, best)
        if (backPay > 0) wallet.credit(backPay, RewardKind.Retro, "Restored ${drawings.size} drawings")
        wallet.updateRewards(wallet.state.value.rewards.copy(goalRewardedUpTo = Economy.previousGoal(current)))
        wallet.markInitialized()
        return backPay
    }

    /** Drops ids this version doesn't know, so an odd backup can't break the avatar. */
    fun sanitizeProfile(p: Profile): Profile {
        val known = AvatarCatalog.items.map { it.id }.toSet()
        val owned = p.owned.filter { it in known }.toSet() + AvatarCatalog.starterIds
        val equipped = p.equipped.filter { (slot, id) ->
            id in owned && AvatarCatalog.get(id)?.slot?.name == slot
        }
        return p.copy(owned = owned, equipped = equipped, unseen = p.unseen.filter { it in known }.toSet())
    }

    fun mergeOwned(ids: Set<String>) {
        val known = AvatarCatalog.items.map { it.id }.toSet()
        val incoming = ids.filter { it in known }
        if (incoming.isEmpty()) return
        profile.update { it.copy(owned = it.owned + incoming) }
    }

    /** Grants every streak-reward piece the best streak has reached. Returns the new ones. */
    @Synchronized
    fun unlockStreakRewards(): List<AvatarItem> {
        val best = streak.state.value.best
        val owned = profile.profile.value.owned
        val fresh = AvatarCatalog.rewardsUpTo(best).filter { it.id !in owned }
        if (fresh.isNotEmpty()) {
            profile.update { p -> p.copy(owned = p.owned + fresh.map { it.id }, unseen = p.unseen + fresh.map { it.id }) }
        }
        return fresh
    }

    fun owns(item: AvatarItem): Boolean = item.isStarter || item.id in profile.profile.value.owned

    @Synchronized
    fun buy(item: AvatarItem): PurchaseResult {
        if (owns(item)) return PurchaseResult.AlreadyOwned
        if (item.isStreakReward || item.price <= 0) return PurchaseResult.NotForSale
        if (!wallet.debit(item.price, RewardKind.Purchase, item.name)) return PurchaseResult.NotEnoughInk
        profile.update { it.copy(owned = it.owned + item.id) }
        return PurchaseResult.Bought(item, wallet.state.value.balance)
    }

    fun equip(item: AvatarItem) {
        if (!owns(item)) return
        profile.update { it.copy(equipped = it.equipped + (item.slot.name to item.id), unseen = it.unseen - item.id) }
    }

    fun unequip(slot: AvatarSlot) {
        if (!slot.removable) return
        profile.update { it.copy(equipped = it.equipped - slot.name) }
    }

    fun markSeen(ids: Collection<String>) {
        if (ids.none { it in profile.profile.value.unseen }) return
        profile.update { it.copy(unseen = it.unseen - ids.toSet()) }
    }

    fun rename(name: String) {
        profile.update { it.copy(name = name.trim().take(24)) }
    }

    /** Buys a streak freeze if there's room for one and enough ink. */
    @Synchronized
    fun buyFreeze(): Boolean {
        if (!streak.canAddFreeze()) return false
        if (!wallet.debit(Economy.FREEZE_PRICE, RewardKind.Freeze, "Streak freeze")) return false
        if (!streak.addFreeze()) {
            // Raced with the weekly grant filling the last slot: give the ink back.
            wallet.credit(Economy.FREEZE_PRICE, RewardKind.Refund, "Freeze refund")
            return false
        }
        return true
    }

    /** Dresses a brand-new profile; [since] is the first drawing, so "drawing since" goes back that far. */
    private fun ensureStarterLook(since: Long? = null) {
        val now = System.currentTimeMillis()
        profile.update { p ->
            if (p.equipped.isNotEmpty()) p
            else p.copy(equipped = AvatarCatalog.defaultLook.mapKeys { it.key.name }, createdAt = minOf(since ?: now, now))
        }
    }

    companion object {
        fun dayOf(millis: Long): Long =
            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        fun drawnDays(list: List<Drawing>): Set<Long> = list.map { dayOf(it.createdAt) }.toSet()
    }
}
