package com.drawit.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * The player's profile: their name, which avatar pieces they own and what is
 * equipped. Item ids are plain strings so this layer doesn't need the art.
 */
data class Profile(
    val name: String = "",
    val owned: Set<String> = emptySet(),
    /** Slot name to item id. */
    val equipped: Map<String, String> = emptyMap(),
    /** Ids the player hasn't looked at in the shop yet, for "new" badges. */
    val unseen: Set<String> = emptySet(),
    val createdAt: Long = 0L
)

class ProfileRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(read())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    @Synchronized
    fun update(transform: (Profile) -> Profile) {
        val next = transform(_profile.value)
        write(next)
        _profile.value = next
    }

    fun restore(profile: Profile) = update { profile }

    private fun read(): Profile {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return Profile(createdAt = System.currentTimeMillis())
        return runCatching { fromJson(JSONObject(raw)) }.getOrDefault(Profile(createdAt = System.currentTimeMillis()))
    }

    private fun write(profile: Profile) {
        prefs.edit().putString(KEY_PROFILE, toJson(profile).toString()).apply()
    }

    companion object {
        private const val KEY_PROFILE = "profile_json"

        fun toJson(p: Profile): JSONObject = JSONObject()
            .put("name", p.name)
            .put("owned", JSONArray(p.owned.sorted()))
            .put("equipped", JSONObject().apply { p.equipped.forEach { (k, v) -> put(k, v) } })
            .put("unseen", JSONArray(p.unseen.sorted()))
            .put("createdAt", p.createdAt)

        fun fromJson(o: JSONObject): Profile {
            val owned = o.optJSONArray("owned")
            val unseen = o.optJSONArray("unseen")
            val equipped = o.optJSONObject("equipped")
            return Profile(
                name = o.optString("name", ""),
                owned = buildSet { if (owned != null) for (i in 0 until owned.length()) add(owned.optString(i)) }.filter { it.isNotEmpty() }.toSet(),
                equipped = buildMap {
                    if (equipped != null) equipped.keys().forEach { k -> equipped.optString(k).takeIf { it.isNotEmpty() }?.let { put(k, it) } }
                },
                unseen = buildSet { if (unseen != null) for (i in 0 until unseen.length()) add(unseen.optString(i)) }.filter { it.isNotEmpty() }.toSet(),
                createdAt = o.optLong("createdAt", 0L)
            )
        }
    }
}
