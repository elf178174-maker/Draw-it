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

/** What importing a backup did to the wallet. */
data class InkImport(val amount: Int, val alreadyAdded: Boolean)

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
        reward.lines.forEach { line ->
            // A day's drawing ink and a perfect week are paid once however many
            // phones they happen on; goals belong to this wallet.
            val source = when (line.kind) {
                RewardKind.Daily, RewardKind.StreakBonus, RewardKind.Extra -> WalletRepository.daySource(today.toEpochDay())
                RewardKind.PerfectWeek -> WalletRepository.weekSource(Economy.weekKey(today))
                else -> null
            }
            wallet.credit(line.amount, line.kind, line.label, source)
        }
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
        // Both are paid "once" per source, so this is safe even after an import.
        val welcome = wallet.creditOnce(mapOf(WalletRepository.WELCOME_SOURCE to Economy.WELCOME_GIFT), RewardKind.Welcome, "Welcome gift")
        val backPay = wallet.creditOnce(
            retroSources(drawings.map { dayOf(it.createdAt) }, state.best),
            RewardKind.Retro, "Your ${drawings.size} past drawings"
        )
        wallet.updateRewards(
            wallet.state.value.rewards.copy(
                goalRewardedUpTo = Economy.previousGoal(state.current),
                lastRewardDay = if (state.drewOn(LocalDate.now())) LocalDate.now().toEpochDay() else -1L
            )
        )
        ensureStarterLook(since = drawings.map { it.createdAt }.filter { it > 0L }.minOrNull())
        val unlocked = unlockStreakRewards()
        val gift = WelcomeGift(welcome, backPay, drawings.size, unlocked)
        wallet.savePendingWelcome(gift.toJson())
        wallet.markInitialized()
        _welcome.value = gift
    }

    /**
     * Brings a backup's ink onto this phone. A backup's wallet is merged
     * source by source, so ink this phone already has (from an earlier import,
     * or a backup of a backup) is never counted twice. Old backups have no
     * wallet, so their drawing days are back-paid, again once per day.
     * Returns the change in balance.
     */
    @Synchronized
    fun importInk(contents: BackupFormat.Contents, alreadyHad: Boolean = false): InkImport {
        val saved = contents.wallet?.let { if (contents.walletIsLump) splitBackupLump(it, contents, alreadyHad) else it }
        val delta = if (saved != null) {
            wallet.mergeFrom(saved)
        } else {
            wallet.creditOnce(
                retroSources(contents.drawings.map { dayOf(it.createdAt) }, contents.streak?.best ?: 0),
                RewardKind.Retro, "Restored ${contents.drawings.size} drawings"
            )
        }
        return InkImport(delta, alreadyAdded = delta == 0 && (saved == null || saved.balance > 0))
    }

    /**
     * A backup from the first 2.0 builds has one lump of ink. Re-file it by
     * source the same way the phone's own wallet was, so merging recognises
     * the welcome gift and days it already has. What's left belongs to the
     * wallet that made it. Without an id, it counts as this phone's own when
     * this phone already had drawings from it ([alreadyHad]), which is how its
     * own early backups look; otherwise it stays separate.
     */
    private fun splitBackupLump(saved: WalletState, contents: BackupFormat.Contents, alreadyHad: Boolean): WalletState {
        val lumpKey = saved.earnedBy.keys.singleOrNull() ?: return saved
        val owner = saved.id.ifEmpty { if (alreadyHad) wallet.state.value.id else lumpKey }
        val pieces = legacyPieces(contents.drawings.map { dayOf(it.createdAt) }, contents.streak?.best ?: 0)
        val (moved, rest) = WalletRepository.splitLump(saved.earnedBy[lumpKey] ?: 0, pieces)
        val spent = saved.spentBy.values.sum()
        return saved.copy(
            earnedBy = moved + (if (rest > 0) mapOf(owner to rest) else emptyMap()),
            spentBy = if (spent > 0) mapOf(owner to spent) else emptyMap()
        )
    }

    /** Re-files a wallet written by the first 2.0 builds by source, once. */
    @Synchronized
    fun splitLegacyWallet(drawings: List<Drawing>) {
        if (!wallet.hasLegacyLump()) return
        wallet.splitLegacy(legacyPieces(drawings.map { dayOf(it.createdAt) }, streak.state.value.best))
    }

    /** True while the avatar is still the starter look with no name, so a backup's look may replace it. */
    fun profileIsUntouched(): Boolean {
        val p = profile.profile.value
        val starter = AvatarCatalog.defaultLook.mapKeys { it.key.name }
        return p.name.isBlank() && (p.equipped.isEmpty() || p.equipped == starter)
    }

    /**
     * Takes the backup's outfit and name, but only onto an avatar nobody has
     * dressed yet. Owned pieces are merged separately. Returns true if it did.
     */
    @Synchronized
    fun adoptLookIfUntouched(incoming: Profile): Boolean {
        if (!profileIsUntouched()) return false
        val clean = sanitizeProfile(incoming)
        val current = profile.profile.value
        if (clean.name.isBlank() && (clean.equipped.isEmpty() || clean.equipped == current.equipped)) return false
        profile.update { p -> p.copy(name = clean.name.take(24), equipped = clean.equipped, owned = p.owned + clean.owned) }
        return true
    }

    /**
     * After a streak arrives from a backup, marks the goals it has already
     * passed as paid, and today's ink too if the backup already drew today.
     */
    @Synchronized
    fun alignRewardsWithStreak(today: LocalDate = LocalDate.now()) {
        val s = streak.state.value
        val r = wallet.state.value.rewards
        val day = today.toEpochDay()
        val updated = r.copy(
            goalRewardedUpTo = maxOf(r.goalRewardedUpTo, Economy.previousGoal(s.current)),
            lastRewardDay = if (s.lastCountedDay == day) maxOf(r.lastRewardDay, day) else r.lastRewardDay
        )
        if (updated != r) wallet.updateRewards(updated)
    }

    /** Moves the profile's "drawing since" back to the earliest drawing (or [since]), if that is earlier. */
    fun noteFirstDrawing(drawings: List<Drawing>, since: Long? = null) {
        val first = (drawings.map { it.createdAt } + listOfNotNull(since)).filter { it > 0L }.minOrNull() ?: return
        profile.update { p -> if (p.createdAt == 0L || first < p.createdAt) p.copy(createdAt = first) else p }
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

        /**
         * Back-pay for drawings made before ink existed, split by source so it
         * can only ever be paid once: each day's drawing ink, and each goal the
         * best streak reached. Adds up to [Economy.retroactive].
         */
        /**
         * What a one-lump wallet was paid for, in the order to share the lump
         * out: the welcome gift, then goals, then days newest first. If the
         * lump falls short, it falls short on the oldest days, which are the
         * ones an early import added without paying for.
         */
        fun legacyPieces(drawingDays: List<Long>, best: Int): Map<String, Int> {
            val all = retroSources(drawingDays, best)
            val goals = all.filterKeys { it.startsWith("goal:") }
            val days = all.filterKeys { it.startsWith("day:") }.entries
                .sortedByDescending { it.key.removePrefix("day:").toLongOrNull() ?: 0L }
                .associate { it.key to it.value }
            return linkedMapOf(WalletRepository.WELCOME_SOURCE to Economy.WELCOME_GIFT) + goals + days
        }

        fun retroSources(drawingDays: List<Long>, best: Int): Map<String, Int> {
            val days = drawingDays.groupingBy { it }.eachCount().entries.associate { (day, n) ->
                WalletRepository.daySource(day) to
                    Economy.DAILY_BASE + minOf(n - 1, Economy.MAX_EXTRAS_PER_DAY) * Economy.EXTRA_DRAWING
            }
            val goals = Economy.GOALS.filter { it <= best }.associate { WalletRepository.goalSource(it) to Economy.goalReward(it) }
            return days + goals
        }
    }
}
