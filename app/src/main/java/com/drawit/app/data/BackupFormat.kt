package com.drawit.app.data

import com.drawit.app.model.RewardState
import org.json.JSONArray
import org.json.JSONObject

/**
 * The manifest inside a backup zip, as plain JSON in and out.
 *
 * Format history:
 *  1. drawings + streak (the first version of the app).
 *  2. adds wallet (ink), profile (avatar) and saved references. The wallet's
 *     id and its per-source counts (earnedBy / spentBy) were added within
 *     format 2, so they may be missing too; totals alone still read.
 *
 * Every field added after format 1 is optional when reading, so any backup
 * ever written still imports.
 */
object BackupFormat {
    const val VERSION = 2

    data class Contents(
        val format: Int,
        val exportedAt: Long?,
        /** The wallet only has totals (early 2.0 builds), so its ink is one lump. */
        val walletIsLump: Boolean = false,
        val drawings: List<Drawing>,
        val streak: StreakState?,
        val wallet: WalletState?,
        val profile: Profile?,
        val references: List<Reference>
    )

    fun build(
        drawings: List<Drawing>,
        streak: StreakState,
        wallet: WalletState,
        profile: Profile,
        references: List<Reference>,
        exportedAt: Long = System.currentTimeMillis()
    ): JSONObject {
        val array = JSONArray()
        drawings.forEach { d ->
            array.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("note", d.note)
                    .put("createdAt", d.createdAt)
                    .put("fileName", d.fileName)
            )
        }
        return JSONObject()
            .put("format", VERSION)
            .put("app", "Draw it")
            .put("exportedAt", exportedAt)
            .put("drawings", array)
            .put(
                "streak",
                JSONObject()
                    .put("current", streak.current)
                    .put("best", streak.best)
                    .put("lastCountedDay", streak.lastCountedDay)
                    .put("freezes", streak.freezes)
                    .put("lastGrantWeek", streak.lastGrantWeek)
                    .put("freezesUsedTotal", streak.freezesUsedTotal)
                    .put("frozenDays", JSONArray(streak.frozenDays.toList()))
            )
            .put(
                "wallet",
                JSONObject()
                    .put("id", wallet.id)
                    .put("earnedBy", WalletRepository.countsToJson(wallet.earnedBy))
                    .put("spentBy", WalletRepository.countsToJson(wallet.spentBy))
                    .put("balance", wallet.balance)
                    .put("earned", wallet.earned)
                    .put("spent", wallet.spent)
                    .put("lastRewardDay", wallet.rewards.lastRewardDay)
                    .put("extrasDay", wallet.rewards.extrasDay)
                    .put("extrasCount", wallet.rewards.extrasCount)
                    .put("goalRewardedUpTo", wallet.rewards.goalRewardedUpTo)
                    .put("perfectWeekPaid", wallet.rewards.perfectWeekPaid)
                    .put("history", WalletRepository.historyToJson(wallet.history))
            )
            .put("profile", ProfileRepository.toJson(profile))
            .put("references", JSONArray().apply { references.forEach { put(ReferenceRepository.toJson(it)) } })
    }

    fun parse(manifest: JSONObject): Contents = Contents(
        format = manifest.optInt("format", 1),
        exportedAt = manifest.optLong("exportedAt", 0L).takeIf { it > 0L },
        walletIsLump = manifest.optJSONObject("wallet")?.has("earnedBy") == false,
        drawings = readDrawings(manifest),
        streak = readStreak(manifest),
        wallet = readWallet(manifest),
        profile = manifest.optJSONObject("profile")?.let { runCatching { ProfileRepository.fromJson(it) }.getOrNull() },
        references = manifest.optJSONArray("references")?.let { array ->
            (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let(ReferenceRepository::fromJson) }
        } ?: emptyList()
    )

    private fun readDrawings(manifest: JSONObject): List<Drawing> {
        val array = manifest.optJSONArray("drawings") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id")
            val fileName = o.optString("fileName")
            if (id.isEmpty() || fileName.isEmpty() || fileName.contains('/') || fileName == "..") return@mapNotNull null
            Drawing(
                id = id,
                title = o.optString("title", "Untitled"),
                note = o.optString("note", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                fileName = fileName
            )
        }
    }

    private fun readStreak(manifest: JSONObject): StreakState? {
        val o = manifest.optJSONObject("streak") ?: return null
        val frozen = o.optJSONArray("frozenDays")
        return StreakState(
            current = o.optInt("current", 0),
            best = o.optInt("best", 0),
            lastCountedDay = o.optLong("lastCountedDay", StreakState.NEVER),
            freezes = o.optInt("freezes", 1).coerceIn(0, StreakState.MAX_FREEZES),
            lastGrantWeek = o.optLong("lastGrantWeek", StreakState.NEVER),
            freezesUsedTotal = o.optInt("freezesUsedTotal", 0),
            frozenDays = buildSet {
                if (frozen != null) for (i in 0 until frozen.length()) add(frozen.optLong(i))
            }
        )
    }

    private fun readWallet(manifest: JSONObject): WalletState? {
        val o = manifest.optJSONObject("wallet") ?: return null
        val id = o.optString("id", "")
        // Early 2.0 backups only have totals; they count as one source of their own.
        val legacy = id.ifEmpty { "backup:" + manifest.optLong("exportedAt", 0L) }
        return WalletState(
            earnedBy = o.optJSONObject("earnedBy")?.let(WalletRepository::countsFromJson)
                ?: WalletRepository.legacyCounts(legacy, o.optInt("earned", o.optInt("balance", 0)).coerceAtLeast(0)),
            spentBy = o.optJSONObject("spentBy")?.let(WalletRepository::countsFromJson)
                ?: WalletRepository.legacyCounts(legacy, o.optInt("spent", 0).coerceAtLeast(0)),
            rewards = RewardState(
                lastRewardDay = o.optLong("lastRewardDay", -1L),
                extrasDay = o.optLong("extrasDay", -1L),
                extrasCount = o.optInt("extrasCount", 0),
                goalRewardedUpTo = o.optInt("goalRewardedUpTo", 0),
                perfectWeekPaid = o.optLong("perfectWeekPaid", -1L)
            ),
            history = o.optJSONArray("history")?.let { runCatching { WalletRepository.historyFromJson(it) }.getOrNull() } ?: emptyList(),
            initialized = true,
            id = id
        )
    }
}
