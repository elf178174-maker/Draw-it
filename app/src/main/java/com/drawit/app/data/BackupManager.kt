package com.drawit.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and writes a single .zip holding the album and the streak, so an album
 * can move to a new phone.
 *
 * Layout:
 *   manifest.json   drawings, streak, and format version
 *   photos/<name>   one JPEG per drawing
 */
class BackupManager(
    private val context: Context,
    private val drawings: DrawingRepository,
    private val streak: StreakRepository
) {

    data class ExportResult(val drawings: Int, val bytes: Long)

    data class ImportResult(
        val added: Int,
        val duplicates: Int,
        val streakRestored: Boolean
    )

    fun suggestedFileName(today: LocalDate = LocalDate.now()): String = "draw-it-backup-$today.zip"

    suspend fun export(target: Uri): Result<ExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = drawings.snapshot()
            val manifest = buildManifest(entries)
            var bytes = 0L

            val stream = context.contentResolver.openOutputStream(target)
                ?: error("Could not open the file you chose.")

            stream.use { raw ->
                ZipOutputStream(raw.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST))
                    val manifestBytes = manifest.toString(2).toByteArray()
                    zip.write(manifestBytes)
                    zip.closeEntry()
                    bytes += manifestBytes.size

                    entries.forEach { drawing ->
                        val file = drawings.fileFor(drawing)
                        if (!file.exists()) return@forEach
                        zip.putNextEntry(ZipEntry("$PHOTOS/${drawing.fileName}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        bytes += file.length()
                    }
                }
            }
            ExportResult(drawings = entries.size, bytes = bytes)
        }
    }

    suspend fun import(source: Uri): Result<ImportResult> = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "import-${System.currentTimeMillis()}")
        try {
            runCatching {
                staging.mkdirs()
                val photoDir = File(staging, PHOTOS).apply { mkdirs() }
                var manifest: JSONObject? = null

                val stream = context.contentResolver.openInputStream(source)
                    ?: error("Could not open that file.")

                stream.use { raw ->
                    ZipInputStream(raw.buffered()).use { zip ->
                        while (true) {
                            val entry = zip.nextEntry ?: break
                            val name = entry.name
                            when {
                                entry.isDirectory -> Unit
                                name == MANIFEST ->
                                    manifest = JSONObject(zip.readBytes().decodeToString())
                                name.startsWith("$PHOTOS/") -> {
                                    val leaf = name.substringAfter("$PHOTOS/")
                                    // Refuse anything trying to escape the staging folder.
                                    if (leaf.isNotEmpty() && !leaf.contains('/') && leaf != "..") {
                                        File(photoDir, leaf).outputStream().use { zip.copyTo(it) }
                                    }
                                }
                            }
                            zip.closeEntry()
                        }
                    }
                }

                val parsed = manifest ?: error("That zip is not a Draw it backup.")
                val incoming = readDrawings(parsed)
                if (incoming.isEmpty()) error("That backup has no drawings in it.")

                val hadNothing = drawings.snapshot().isEmpty()
                val (added, duplicates) = drawings.merge(incoming) { drawing ->
                    File(photoDir, drawing.fileName).takeIf { it.exists() }
                }

                // Only adopt the backup's streak on a phone with nothing to lose.
                val restored = if (hadNothing && added > 0) {
                    readStreak(parsed)?.let { streak.restore(it); true } ?: false
                } else {
                    false
                }

                ImportResult(added = added, duplicates = duplicates, streakRestored = restored)
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    // -- manifest ---------------------------------------------------------

    private fun buildManifest(entries: List<Drawing>): JSONObject {
        val array = JSONArray()
        entries.forEach { d ->
            array.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("note", d.note)
                    .put("createdAt", d.createdAt)
                    .put("fileName", d.fileName)
            )
        }
        val s = streak.state.value
        return JSONObject()
            .put("format", FORMAT_VERSION)
            .put("app", "Draw it")
            .put("exportedAt", System.currentTimeMillis())
            .put("drawings", array)
            .put(
                "streak",
                JSONObject()
                    .put("current", s.current)
                    .put("best", s.best)
                    .put("lastCountedDay", s.lastCountedDay)
                    .put("freezes", s.freezes)
                    .put("lastGrantWeek", s.lastGrantWeek)
                    .put("freezesUsedTotal", s.freezesUsedTotal)
                    .put("frozenDays", JSONArray(s.frozenDays.toList()))
            )
    }

    private fun readDrawings(manifest: JSONObject): List<Drawing> {
        val array = manifest.optJSONArray("drawings") ?: return emptyList()
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val id = o.optString("id")
            val fileName = o.optString("fileName")
            if (id.isEmpty() || fileName.isEmpty() || fileName.contains('/')) return@mapNotNull null
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

    private companion object {
        const val FORMAT_VERSION = 1
        const val MANIFEST = "manifest.json"
        const val PHOTOS = "photos"
    }
}
