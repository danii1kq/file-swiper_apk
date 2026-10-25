package com.danni.fileswiper.data.model

enum class FileCategory { PHOTOS, VIDEOS, AUDIO, DOCUMENTS }

enum class CleanMode { MONTHLY, QUICK }

data class FileItem(
    val id: Long,
    val uri: String,
    val name: String,
    val size: Long,
    val dateModified: Long,
    val category: FileCategory
)

val FileItem.extension: String
    get() = name.substringAfterLast('.', "").lowercase()

data class MonthBucket(
    val id: String,
    val label: String,
    val startMillis: Long,
    val count: Int,
    val totalSize: Long,
    val isCleared: Boolean
)

data class AudioPlayState(
    val fileId: Long = 0L,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0
)

data class DeleteOutcome(
    val freedBytes: Long,
    val failed: Int
)

data class SessionRequest(
    val files: List<FileItem>,
    val isMonthly: Boolean,
    val monthKey: String?,
    val monthLabel: String?
)
