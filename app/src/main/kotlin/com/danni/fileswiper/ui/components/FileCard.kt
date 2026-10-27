package com.danni.fileswiper.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danni.fileswiper.data.model.FileCategory
import com.danni.fileswiper.data.model.FileItem
import com.danni.fileswiper.data.model.extension
import com.danni.fileswiper.ui.common.formatDateShort
import com.danni.fileswiper.ui.common.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DocVisual(val label: String, val background: Color)

private fun docVisualFor(extension: String): DocVisual = when (extension) {
    "doc", "docx" -> DocVisual("W", Color(0xFF185ABD))
    "xls", "xlsx" -> DocVisual("X", Color(0xFF107C41))
    "ppt", "pptx" -> DocVisual("P", Color(0xFFD24726))
    "pdf" -> DocVisual("PDF", Color(0xFFD93025))
    "txt" -> DocVisual("TXT", Color(0xFF5F6368))
    else -> DocVisual("?", Color(0xFF757575))
}

/** Big Tinder-style card for a media/file item. */
@Composable
fun FileCard(
    item: FileItem,
    playing: Boolean,
    positionFraction: Float,
    timeText: String,
    onPlayToggle: () -> Unit
) {
    val context = LocalContext.current
    val onOpenFile = { openInViewer(context, item) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.44f)
            ) {
                CardHeader(item, onOpenFile)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatFileSize(context, item.size) + " · " +
                        formatDateShort(context, item.dateModified),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.category == FileCategory.AUDIO) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AudioBar(
                        playing = playing,
                        positionFraction = positionFraction,
                        timeText = timeText,
                        onPlayToggle = onPlayToggle
                    )
                }
            }
        }
    }
}

@Composable
private fun CardHeader(item: FileItem, onOpenFile: () -> Unit) {
    when (item.category) {
        FileCategory.PHOTOS, FileCategory.VIDEOS -> {
            var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
            val context = LocalContext.current
            LaunchedEffect(item.id, item.uri) {
                bitmap = withContext(Dispatchers.IO) { loadPreview(context, item) }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(item.uri) {
                        // Long press opens the file in its native viewer/player.
                        detectTapGestures(onLongPress = { onOpenFile() })
                    }
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        // Native aspect ratio: the whole frame is visible, uncropped.
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CardFallbackIcon(
                        icon = if (item.category == FileCategory.VIDEOS) {
                            Icons.Rounded.Videocam
                        } else {
                            Icons.Rounded.Image
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                if (item.category == FileCategory.VIDEOS) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(onClick = onOpenFile)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringOrEmpty(false),
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        FileCategory.AUDIO -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .pointerInput(item.uri) {
                        detectTapGestures(onLongPress = { onOpenFile() })
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                )
            }
        }

        FileCategory.DOCUMENTS -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(item.uri) {
                        detectTapGestures(onLongPress = { onOpenFile() })
                    }
            ) {
                val visual = docVisualFor(item.extension)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(96.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(visual.background)
                ) {
                    Text(
                        text = visual.label,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardFallbackIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(56.dp)
    )
}

@Composable
private fun AudioBar(
    playing: Boolean,
    positionFraction: Float,
    timeText: String,
    onPlayToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onPlayToggle)
        ) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringOrEmpty(playing),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        LinearProgressIndicator(
            progress = { positionFraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun stringOrEmpty(playing: Boolean): String =
    if (playing) "Pause" else "Play"

/** Compact glyph for trash rows: circular type badge, as in the Stitch design. */
@Composable
fun FileGlyph(item: FileItem, glyphSize: Dp = 44.dp) {
    when (item.category) {
        FileCategory.PHOTOS, FileCategory.VIDEOS -> {
            var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
            val context = LocalContext.current
            LaunchedEffect(item.id, item.uri) {
                bitmap = withContext(Dispatchers.IO) { loadThumbnail(context, item.uri) }
            }
            Box(modifier = Modifier.size(glyphSize)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(glyphSize)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (item.category == FileCategory.VIDEOS) {
                                    Icons.Rounded.Videocam
                                } else {
                                    Icons.Rounded.Image
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Text(
                    text = item.extension.uppercase().ifBlank { "FILE" },
                    style = TextStyle(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }

        FileCategory.AUDIO -> {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(glyphSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        FileCategory.DOCUMENTS -> {
            val visual = docVisualFor(item.extension)
            Surface(
                shape = CircleShape,
                color = visual.background,
                modifier = Modifier.size(glyphSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = visual.label,
                        color = Color.White,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun loadThumbnail(context: Context, uriRaw: String): Bitmap? {
    val uri = Uri.parse(uriRaw)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, android.util.Size(512, 512), null)
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val sample = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / 640)
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Native-resolution preview for the swipe card: the real image (not the small
 * MediaStore thumbnail) or a full-size video frame; on failure any fallback.
 */
private fun loadPreview(context: Context, item: FileItem): Bitmap? = try {
    if (item.category == FileCategory.VIDEOS) {
        loadVideoFrame(context, Uri.parse(item.uri)) ?: loadThumbnail(context, item.uri)
    } else {
        loadFullImage(context, Uri.parse(item.uri)) ?: loadThumbnail(context, item.uri)
    }
} catch (_: Exception) {
    null
}

/** Full image decoded from the actual file, downsampled only past 2560px to keep memory sane. */
private fun loadFullImage(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (maxDim > 2560 * sample) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}

/** Native-resolution frame from the start of the video. */
private fun loadVideoFrame(context: Context, uri: Uri): Bitmap? {
    var retriever: MediaMetadataRetriever? = null
    try {
        retriever = MediaMetadataRetriever().apply { setDataSource(context, uri) }
        return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (_: Exception) {
        return null
    } finally {
        try {
            retriever?.release()
        } catch (_: Exception) {
        }
    }
}

/** Opens the item in the system viewer/player (long press on the card header). */
private fun openInViewer(context: Context, item: FileItem) {
    try {
        val uri = Uri.parse(item.uri)
        val mime = context.contentResolver.getType(uri) ?: fallbackMime(item)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    } catch (_: Exception) {
        // No viewer registered for this type; open silently does nothing.
    }
}

private fun fallbackMime(item: FileItem): String = when (item.category) {
    FileCategory.PHOTOS -> "image/*"
    FileCategory.VIDEOS -> "video/*"
    FileCategory.AUDIO -> "audio/*"
    FileCategory.DOCUMENTS -> when (item.extension) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt" -> "text/plain"
        else -> "*/*"
    }
}
