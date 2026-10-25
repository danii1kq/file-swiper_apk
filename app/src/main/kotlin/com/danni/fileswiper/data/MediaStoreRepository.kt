package com.danni.fileswiper.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.danni.fileswiper.data.model.DeleteOutcome
import com.danni.fileswiper.data.model.FileCategory
import com.danni.fileswiper.data.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaRepository {
    suspend fun scan(categories: Set<FileCategory>): List<FileItem>
    suspend fun delete(items: Collection<FileItem>): DeleteOutcome
}

class MediaStoreRepository(context: Context) : MediaRepository {

    private val resolver: ContentResolver = context.contentResolver

    override suspend fun scan(categories: Set<FileCategory>): List<FileItem> =
        withContext(Dispatchers.IO) {
            if (categories.isEmpty()) return@withContext emptyList()
            val result = ArrayList<FileItem>()
            if (FileCategory.PHOTOS in categories) {
                result += query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    FileCategory.PHOTOS,
                    extensions = null
                )
            }
            if (FileCategory.VIDEOS in categories) {
                result += query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    FileCategory.VIDEOS,
                    extensions = null
                )
            }
            if (FileCategory.AUDIO in categories) {
                result += query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    FileCategory.AUDIO,
                    extensions = AUDIO_EXTENSIONS
                )
            }
            if (FileCategory.DOCUMENTS in categories) {
                result += queryDocuments()
            }
            result.distinctBy { it.uri }
        }

    private fun query(
        uri: Uri,
        category: FileCategory,
        extensions: Set<String>?
    ): List<FileItem> {
        val files = ArrayList<FileItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        try {
            resolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: continue
                    if (name.isBlank()) continue
                    if (extensions != null) {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext !in extensions) continue
                    }
                    files += FileItem(
                        id = cursor.getLong(idIdx),
                        uri = ContentUris.withAppendedId(uri, cursor.getLong(idIdx)).toString(),
                        name = name,
                        size = cursor.getLong(sizeIdx),
                        dateModified = cursor.getLong(dateIdx) * 1000L,
                        category = category
                    )
                }
            }
        } catch (_: RuntimeException) {
            // SecurityException extends it; provider/cursor glitches too.
            // Scan must never crash the app.
        } catch (_: Exception) {
        }
        return files
    }

    /**
     * Documents are scanned from MediaStore.Files with the hardcoded
     * extension filter requested in the requirements:
     * .doc .docx .xls .xlsx .ppt .pptx .pdf .txt
     */
    private fun queryDocuments(): List<FileItem> {
        val files = ArrayList<FileItem>()
        val uri = MediaStore.Files.getContentUri("external")
        val extensions = DOCUMENT_EXTENSIONS
        val selection =
            extensions.joinToString(" OR ") { "lower(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ?" }
        val args = extensions.map { "%.$it" }.toTypedArray()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        try {
            resolver.query(
                uri,
                projection,
                selection,
                args,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: continue
                    if (name.isBlank()) continue
                    files += FileItem(
                        id = cursor.getLong(idIdx),
                        uri = ContentUris.withAppendedId(uri, cursor.getLong(idIdx)).toString(),
                        name = name,
                        size = cursor.getLong(sizeIdx),
                        dateModified = cursor.getLong(dateIdx) * 1000L,
                        category = FileCategory.DOCUMENTS
                    )
                }
            }
        } catch (_: RuntimeException) {
            // SecurityException extends it; provider/cursor glitches too.
            // Scan must never crash the app.
        } catch (_: Exception) {
        }
        return files
    }

    override suspend fun delete(items: Collection<FileItem>): DeleteOutcome =
        withContext(Dispatchers.IO) {
            var freed = 0L
            var failed = 0
            items.forEach { item ->
                try {
                    val rows = resolver.delete(Uri.parse(item.uri), null, null)
                    if (rows > 0) freed += item.size else failed++
                } catch (_: Exception) {
                    failed++
                }
            }
            DeleteOutcome(freedBytes = freed, failed = failed)
        }

    companion object {
        val DOCUMENT_EXTENSIONS =
            listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf", "txt")
        private val AUDIO_EXTENSIONS = setOf("mp3", "wav")
    }
}
