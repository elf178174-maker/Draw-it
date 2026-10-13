package com.drawit.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A pin saved from the inspiration feed as a drawing reference. */
data class Reference(
    val id: String,
    val imageUrl: String,
    val title: String,
    val link: String,
    val width: Int,
    val height: Int,
    val savedAt: Long
)

/**
 * Saved references, plus the one picked as "today's reference" for the home
 * screen. Only links are stored; the images come from Pinterest's servers.
 */
class ReferenceRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("drawit_prefs", Context.MODE_PRIVATE)

    private val _saved = MutableStateFlow(read())
    val saved: StateFlow<List<Reference>> = _saved.asStateFlow()

    private val _today = MutableStateFlow(readToday())
    val today: StateFlow<Reference?> = _today.asStateFlow()

    @Synchronized
    fun toggle(ref: Reference) {
        val current = _saved.value
        val next = if (current.any { it.id == ref.id }) current.filterNot { it.id == ref.id }
        else listOf(ref.copy(savedAt = System.currentTimeMillis())) + current
        commit(next)
    }

    fun isSaved(id: String) = _saved.value.any { it.id == id }

    @Synchronized
    fun merge(incoming: List<Reference>) {
        val known = _saved.value.map { it.id }.toSet()
        val added = incoming.filterNot { it.id in known }
        if (added.isNotEmpty()) commit((_saved.value + added).sortedByDescending { it.savedAt })
    }

    /** Picks the reference to draw from today, or clears it with null. */
    fun setToday(ref: Reference?) {
        prefs.edit().putString(KEY_TODAY, ref?.let { toJson(it).put("day", java.time.LocalDate.now().toEpochDay()).toString() }).apply()
        _today.value = ref
    }

    private fun commit(list: List<Reference>) {
        prefs.edit().putString(KEY_SAVED, JSONArray().apply { list.forEach { put(toJson(it)) } }.toString()).apply()
        _saved.value = list
    }

    private fun read(): List<Reference> = runCatching {
        val array = JSONArray(prefs.getString(KEY_SAVED, "[]"))
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::fromJson) }
    }.getOrDefault(emptyList())

    /** Today's pick expires at midnight. */
    private fun readToday(): Reference? = runCatching {
        val o = JSONObject(prefs.getString(KEY_TODAY, null) ?: return null)
        if (o.optLong("day") != java.time.LocalDate.now().toEpochDay()) return null
        fromJson(o)
    }.getOrNull()

    companion object {
        private const val KEY_SAVED = "references_json"
        private const val KEY_TODAY = "reference_today_json"

        fun toJson(r: Reference): JSONObject = JSONObject()
            .put("id", r.id).put("image", r.imageUrl).put("title", r.title).put("link", r.link)
            .put("w", r.width).put("h", r.height).put("savedAt", r.savedAt)

        fun fromJson(o: JSONObject): Reference? {
            val id = o.optString("id")
            val image = o.optString("image")
            if (id.isEmpty() || !image.startsWith("https://")) return null
            return Reference(
                id = id, imageUrl = image, title = o.optString("title"), link = o.optString("link"),
                width = o.optInt("w", 236), height = o.optInt("h", 300), savedAt = o.optLong("savedAt")
            )
        }
    }
}
