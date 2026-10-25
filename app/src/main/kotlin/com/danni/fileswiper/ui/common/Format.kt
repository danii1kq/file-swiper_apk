package com.danni.fileswiper.ui.common

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Locale-aware human sizes: Б/КБ/МБ/ГБ (or B/KB/MB/GB). */
fun formatFileSize(context: Context, bytes: Long): String {
    val locale = context.resources.configuration.locales[0]
    val one = context.getString(com.danni.fileswiper.R.string.unit_b)
    val kilo = context.getString(com.danni.fileswiper.R.string.unit_kb)
    val mega = context.getString(com.danni.fileswiper.R.string.unit_mb)
    val giga = context.getString(com.danni.fileswiper.R.string.unit_gb)
    return when {
        bytes < 0 -> "—"
        bytes < 1024 -> "${bytes} $one"
        bytes < 1048576L -> formatFraction(locale, bytes / 1024.0) + " " + kilo
        bytes < 1073741824L -> formatFraction(locale, bytes / 1048576.0) + " " + mega
        else -> formatFraction(locale, bytes / 1073741824L.toDouble()) + " " + giga
    }
}

fun formatFileSizeCompact(bytes: Long): String = when {
    bytes < 0 -> "?"
    bytes < 1024L -> "$bytes B"
    bytes < 1048576L -> String.format(java.util.Locale.US, "%.0f KB", bytes / 1024.0)
    bytes < 1073741824L -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576.0)
    else -> String.format(java.util.Locale.US, "%.2f GB", bytes / 1073741824.0)
}

private fun formatFraction(locale: Locale, value: Double): String {
    val pattern = if (value >= 100.0) "%.0f" else "%.1f"
    return String.format(locale, pattern, value)
}

/** Short file date, e.g. "12 May 2025". */
fun formatDateShort(context: Context, millis: Long): String {
    val locale = context.resources.configuration.locales[0]
    return SimpleDateFormat("d MMM yyyy", locale).format(Date(millis))
}
