@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.danni.fileswiper.ui.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danni.fileswiper.R
import com.danni.fileswiper.data.model.AudioPlayState
import com.danni.fileswiper.ui.components.FileCard
import com.danni.fileswiper.ui.components.SwipeDirection
import com.danni.fileswiper.ui.components.SwipeStack
import com.danni.fileswiper.ui.components.rememberSwipeStackState
import com.danni.fileswiper.ui.common.BadgePill
import com.danni.fileswiper.ui.common.formatFileSize
import com.danni.fileswiper.viewmodel.SwipeViewModel
import java.util.Locale
import kotlin.math.max

@Composable
fun SwipeScreen(
    onFinished: () -> Unit,
    onFinishRequested: () -> Unit,
    viewModel: SwipeViewModel = viewModel()
) {
    val context = LocalContext.current
    val queue by viewModel.queue.collectAsState()
    val trash by viewModel.trash.collectAsState()
    val isMonthly by viewModel.isMonthly.collectAsState()
    val monthLabel by viewModel.monthLabel.collectAsState()
    val generation by viewModel.generation.collectAsState()
    val sessionDone by viewModel.sessionDone.collectAsState()
    val audioState by viewModel.audioState.collectAsState()
    val trashSize = trash.sumOf { it.size }
    var showFinishDialog by remember { mutableStateOf(false) }

    val stack = rememberSwipeStackState()
    val topItem = queue.firstOrNull()
    val nextItem = queue.getOrNull(1)

    LaunchedEffect(sessionDone) {
        if (sessionDone) onFinished()
    }

    // Process death mid-session (e.g. the user went to system settings to grant
    // storage access) can restore this screen with an empty, unfinished queue.
    // Escape gracefully to the trash/home instead of showing a dead screen.
    LaunchedEffect(Unit) {
        if (viewModel.isDeadEnd()) onFinishRequested()
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showFinishDialog = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.finish_dialog_title),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.swipe_remaining, queue.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (isMonthly && monthLabel != null) {
                    Text(
                        text = monthLabel.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BadgePill(
                text = formatFileSize(context, trashSize),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            SwipeStack(
                state = stack,
                topItem = topItem,
                nextItem = nextItem,
                stackKey = generation,
                onSwipeLeft = { viewModel.onSwipeLeft(it) },
                onSwipeRight = { viewModel.onSwipeRight(it) },
                onSwipeUp = { viewModel.onSkip() },
                card = { item ->
                    val playing = audioState.fileId == item.id && audioState.isPlaying
                    val fraction = if (audioState.fileId == item.id && audioState.durationMs > 0) {
                        audioState.positionMs.toFloat() / audioState.durationMs.toFloat()
                    } else {
                        0f
                    }
                    FileCard(
                        item = item,
                        playing = playing,
                        positionFraction = fraction,
                        timeText = formatTime(audioState, item.id),
                        onPlayToggle = { viewModel.toggleAudio(item) }
                    )
                }
            )
        }

        ActionButtonsRow(
            onDeleteClick = { stack.trigger(SwipeDirection.LEFT) },
            onSkipClick = { stack.trigger(SwipeDirection.UP) },
            onKeepClick = { stack.trigger(SwipeDirection.RIGHT) }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.finish_dialog_title)) },
            text = { Text(stringResource(R.string.finish_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.stopAudio()
                        showFinishDialog = false
                        onFinishRequested()
                    }
                ) { Text(stringResource(R.string.go_trash)) }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionButtonsRow(
    onDeleteClick: () -> Unit,
    onSkipClick: () -> Unit,
    onKeepClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundAction(
            icon = Icons.Rounded.Delete,
            tint = MaterialTheme.colorScheme.onError,
            container = MaterialTheme.colorScheme.error,
            label = stringResource(R.string.delete_btn),
            onClick = onDeleteClick,
            size = 70.dp
        )
        RoundAction(
            icon = Icons.Rounded.KeyboardArrowUp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceVariant,
            label = stringResource(R.string.skip_btn),
            onClick = onSkipClick,
            size = 56.dp
        )
        RoundAction(
            icon = Icons.Rounded.Favorite,
            tint = MaterialTheme.colorScheme.onPrimary,
            container = MaterialTheme.colorScheme.primary,
            label = stringResource(R.string.keep_btn),
            onClick = onKeepClick,
            size = 70.dp
        )
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    container: Color,
    label: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(container)
                .clickable(onClick = onClick)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.align(Alignment.Center).size(size / 2)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(state: AudioPlayState, fileId: Long): String {
    if (state.fileId != fileId || state.durationMs <= 0) return "0:00"
    val pos = state.positionMs / 1000
    val duration = state.durationMs / 1000
    return String.format(Locale.US, "%d:%02d / %d:%02d", pos / 60, pos % 60, duration / 60, duration % 60)
}
