package com.danni.fileswiper.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danni.fileswiper.data.model.FileCategory
import com.danni.fileswiper.data.model.FileItem
import com.danni.fileswiper.ui.common.formatDateShort
import com.danni.fileswiper.ui.common.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class DocVisual(val label: String, val background: Color)

private fun docVisualFor(extension: String): DocVisual = when (extension) {
    "doc", "docx" -> DocVisual("W", Color(0xFF2B579A))
    "xls", "xlsx" -> DocVisual("X", Color(0xFF217346))
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
                CardHeader(item)
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
                    text = formatFileSize(LocalContext.current, item.size) + " · " +
                        formatDateShort(LocalContext.current, item.dateModified),
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
private fun CardHeader(item: FileItem) {
    when (item.category) {
        FileCategory.PHOTOS, FileCategory.VIDEOS -> {
            var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
            val context = LocalContext.current
            LaunchedEffect(item.id, item.uri) {
                bitmap = withContext(Dispatchers.IO) { loadThumbnail(context, item.uri) }
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
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
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
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
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
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
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
            progress = positionFraction.coerceIn(0f, 1f),
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

/** Compact glyph for trash rows. */
@Composable
fun FileGlyph(item: FileItem, glyphSize: Dp = 44.dp) {
    when (item.category) {
        FileCategory.PHOTOS, FileCategory.VIDEOS -> {
            var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
            val context = LocalContext.current
            LaunchedEffect(item.id, item.uri) {
                bitmap = withContext(Dispatchers.IO) { loadThumbnail(context, item.uri) }
            }
            Surface(
                shape = MaterialTheme.shapes.small,
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
        }

        FileCategory.AUDIO -> {
            Surface(
                shape = MaterialTheme.shapes.small,
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
                shape = MaterialTheme.shapes.small,
                color = visual.background,
                modifier = Modifier.size(glyphSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = visual.label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
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
