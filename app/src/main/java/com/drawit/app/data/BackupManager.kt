package com.drawit.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and writes a single .zip holding the album, the streak, ink, the
 * avatar and saved references, so everything can move to a new phone.
 *
 * Layout:
 *   manifest.json   see [BackupFormat]
 *   photos/<name>   one JPEG per drawing
 */
class BackupManager(
    private val context: Context,
    private val drawings: DrawingRepository,
    private val streak: StreakRepository,
    private val wallet: WalletRepository,
    private val profile: ProfileRepository,
    private val references: ReferenceRepository,
    private val progress: ProgressManager
) {

    data class ExportResult(val drawings: Int, val bytes: Long)

    data class ImportResult(
        val added: Int,
        val duplicates: Int,
        val streakRestored: Boolean,
        /** Ink restored from the backup, or back-paid for an old backup. */
        val inkRestored: Int,
        val avatarRestored: Boolean
    )

    fun suggestedFileName(today: LocalDate = LocalDate.now()): String = "draw-it-backup-$today.zip"

    suspend fun export(target: Uri): Result<ExportResult> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = drawings.snapshot()
            val manifest = BackupFormat.build(
                drawings = entries,
                streak = streak.state.value,
                wallet = wallet.state.value,
                profile = profile.profile.value,
                references = references.saved.value
            )
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
                                    if (leaf.isNotEmpty() && !leaf.contains('/') && !leaf.contains('\\') && leaf != "..") {
                                        File(photoDir, leaf).outputStream().use { zip.copyTo(it) }
                                    }
                                }
                            }
                            zip.closeEntry()
                        }
                    }
                }

                val parsed = BackupFormat.parse(manifest ?: error("That zip is not a Draw it backup."))
                if (parsed.drawings.isEmpty()) error("That backup has no drawings in it.")

                val hadNothing = drawings.snapshot().isEmpty()
                val (added, duplicates) = drawings.merge(parsed.drawings) { drawing ->
                    File(photoDir, drawing.fileName).takeIf { it.exists() }
                }

                // Saved references and owned avatar pieces never hurt to merge.
                references.merge(parsed.references)

                // Only adopt the backup's streak, ink and avatar on a phone with
                // nothing to lose; otherwise ink could be duplicated by importing
                // the same backup twice.
                val fresh = hadNothing && added > 0
                var streakRestored = false
                var ink = 0
                var avatarRestored = false
                if (fresh) {
                    parsed.streak?.let { streak.restore(it); streakRestored = true }
                    ink = progress.restoreWallet(parsed.wallet, parsed.drawings, parsed.streak)
                    parsed.profile?.let { profile.restore(progress.sanitizeProfile(it)); avatarRestored = true }
                    progress.unlockStreakRewards()
                } else {
                    parsed.profile?.let { incoming -> progress.mergeOwned(incoming.owned) }
                }

                ImportResult(
                    added = added,
                    duplicates = duplicates,
                    streakRestored = streakRestored,
                    inkRestored = ink,
                    avatarRestored = avatarRestored
                )
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private companion object {
        const val MANIFEST = "manifest.json"
        const val PHOTOS = "photos"
    }
}
