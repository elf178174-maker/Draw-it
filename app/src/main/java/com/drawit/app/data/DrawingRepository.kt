package com.drawit.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Stores drawings as JPEGs under files/drawings plus a small JSON index.
 * Everything stays on the device; nothing is uploaded anywhere.
 */
class DrawingRepository(private val context: Context) {

    private val dir: File get() = File(context.filesDir, "drawings").apply { mkdirs() }
    private val indexFile: File get() = File(dir, "index.json")
    private val writeLock = Mutex()

    private val _drawings = MutableStateFlow<List<Drawing>>(emptyList())
    val drawings: StateFlow<List<Drawing>> = _drawings.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        _drawings.value = readIndex()
    }

    fun fileFor(drawing: Drawing): File = File(dir, drawing.fileName)

    /** Copies [source] into app storage, downscaled and rotation-corrected. */
    suspend fun add(source: Uri, title: String, note: String, createdAt: Long): Drawing? =
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val target = File(dir, "$id.jpg")
            val ok = importImage(source, target)
            if (!ok) return@withContext null

            val drawing = Drawing(
                id = id,
                title = title.trim().ifEmpty { "Untitled" },
                note = note.trim(),
                createdAt = createdAt,
                fileName = target.name
            )
            writeLock.withLock {
                val updated = (readIndex() + drawing).sortedByDescending { it.createdAt }
                writeIndex(updated)
                _drawings.value = updated
            }
            drawing
        }

    suspend fun update(id: String, title: String, note: String) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            val updated = readIndex().map {
                if (it.id == id) it.copy(title = title.trim().ifEmpty { "Untitled" }, note = note.trim())
                else it
            }.sortedByDescending { it.createdAt }
            writeIndex(updated)
            _drawings.value = updated
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        writeLock.withLock {
            val current = readIndex()
            current.firstOrNull { it.id == id }?.let { runCatching { File(dir, it.fileName).delete() } }
            val updated = current.filterNot { it.id == id }
            writeIndex(updated)
            _drawings.value = updated
        }
    }

    private fun readIndex(): List<Drawing> {
        if (!indexFile.exists()) return emptyList()
        return runCatching {
            val array = JSONArray(indexFile.readText())
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val fileName = o.optString("fileName")
                if (fileName.isEmpty() || !File(dir, fileName).exists()) return@mapNotNull null
                Drawing(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.optString("title", "Untitled"),
                    note = o.optString("note", ""),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    fileName = fileName
                )
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    private fun writeIndex(list: List<Drawing>) {
        val array = JSONArray()
        list.forEach { d ->
            array.put(
                JSONObject()
                    .put("id", d.id)
                    .put("title", d.title)
                    .put("note", d.note)
                    .put("createdAt", d.createdAt)
                    .put("fileName", d.fileName)
            )
        }
        // Write to a temp file first so an interrupted write can never corrupt the index.
        val tmp = File(dir, "index.json.tmp")
        tmp.writeText(array.toString())
        if (indexFile.exists()) indexFile.delete()
        tmp.renameTo(indexFile)
    }

    private fun importImage(source: Uri, target: File): Boolean = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // Bounds-only decoding always returns a null bitmap, so check the stream instead.
        val boundsStream = context.contentResolver.openInputStream(source) ?: return false
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return false

        // Decode at most one power-of-two step above the size we keep.
        var sample = 1
        while (longest / sample > MAX_EDGE) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return false

        val rotation = context.contentResolver.openInputStream(source)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val scale = MAX_EDGE.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale < 1f || rotation != 0f) {
            val matrix = Matrix()
            if (scale < 1f) matrix.postScale(scale, scale)
            if (rotation != 0f) matrix.postRotate(rotation)
            val scaled = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        bitmap.recycle()
        true
    }.getOrDefault(false)

    private companion object {
        const val MAX_EDGE = 2048
    }
}
