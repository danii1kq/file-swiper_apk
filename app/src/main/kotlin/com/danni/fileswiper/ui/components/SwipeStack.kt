package com.danni.fileswiper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danni.fileswiper.R
import com.danni.fileswiper.data.model.FileItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class SwipeDirection { LEFT, RIGHT, UP }

private val OffsetConverter = object : TwoWayConverter<Offset, AnimationVector2D> {
    override val convertToVector: (Offset) -> AnimationVector2D =
        { value -> AnimationVector2D(value.x, value.y) }
    override val convertFromVector: (AnimationVector2D) -> Offset =
        { vector -> Offset(vector.v1, vector.v2) }
}

@Composable
fun rememberSwipeStackState(): SwipeStackState {
    val scope = rememberCoroutineScope()
    return remember { SwipeStackState(scope) }
}

/**
 * Tinder-style card stack controller:
 * swipe right = keep, swipe left = delete, swipe up = skip to the end of the queue.
 * Thresholds are gentle so cards fly away with a light touch.
 */
class SwipeStackState internal constructor(private val scope: CoroutineScope) {

    val offset = Animatable(Offset.Zero, OffsetConverter)
    private val entrance = Animatable(0f)

    internal var onSwipeLeft: (FileItem) -> Unit = {}
    internal var onSwipeRight: (FileItem) -> Unit = {}
    internal var onSwipeUp: (FileItem) -> Unit = {}
    internal var topProvider: () -> FileItem? = { null }

    private val velocityTracker = VelocityTracker()
    private var dragging = false

    var busy = false
        private set

    var widthPx = 360f
        private set
    var heightPx = 640f
        private set

    fun metrics(width: Float, height: Float) {
        if (width > 0f) widthPx = width
        if (height > 0f) heightPx = height
    }

    fun onDragStart() {
        if (busy) return
        dragging = true
        velocityTracker.resetTracking()
    }

    fun onDrag(dx: Float, dy: Float, uptimeMillis: Long, position: Offset) {
        if (busy || !dragging) return
        velocityTracker.addPosition(uptimeMillis, position)
        scope.launch {
            offset.snapTo(offset.value + Offset(dx, dy))
        }
    }

    fun onDragEnd() {
        dragging = false
        if (busy) return
        val velocity = velocityTracker.computeVelocityOrNull()
        val horizontal = widthPx * 0.22f
        val vertical = heightPx * 0.24f
        when {
            offset.value.x > horizontal || velocity.x > 900f -> startExit(SwipeDirection.RIGHT)
            offset.value.x < -horizontal || velocity.x < -900f -> startExit(SwipeDirection.LEFT)
            offset.value.y < -vertical && abs(offset.value.y) > abs(offset.value.x) ->
                startExit(SwipeDirection.UP)
            else -> animateHome()
        }
    }

    fun trigger(direction: SwipeDirection) {
        if (busy) return
        startExit(direction)
    }

    internal fun startExit(direction: SwipeDirection) {
        if (busy) return
        busy = true
        val from = offset.value
        val target = when (direction) {
            SwipeDirection.RIGHT -> Offset(widthPx * 1.6f, from.y * 0.4f)
            SwipeDirection.LEFT -> Offset(-widthPx * 1.6f, from.y * 0.4f)
            SwipeDirection.UP -> Offset(from.x * 0.35f, -heightPx * 1.25f)
        }
        scope.launch {
            offset.animateTo(target, tween(durationMillis = 250, easing = FastOutLinearInEasing))
            busy = false
            val item = topProvider()
            if (item != null) {
                when (direction) {
                    SwipeDirection.LEFT -> onSwipeLeft(item)
                    SwipeDirection.RIGHT -> onSwipeRight(item)
                    SwipeDirection.UP -> onSwipeUp(item)
                }
            }
        }
    }

    private fun animateHome() {
        if (busy) return
        busy = true
        scope.launch {
            offset.animateTo(
                Offset.Zero,
                spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            busy = false
        }
    }

    internal fun resetForNewCard() {
        scope.launch {
            dragging = false
            offset.snapTo(Offset.Zero)
            busy = false
            entrance.snapTo(0f)
            entrance.animateTo(
                1f,
                spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    internal fun entranceValue(): Float = entrance.value
}

@Composable
fun SwipeStack(
    state: SwipeStackState,
    topItem: FileItem?,
    nextItem: FileItem?,
    stackKey: Long,
    onSwipeLeft: (FileItem) -> Unit,
    onSwipeRight: (FileItem) -> Unit,
    onSwipeUp: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
    card: @Composable (FileItem) -> Unit
) {
    state.onSwipeLeft = onSwipeLeft
    state.onSwipeRight = onSwipeRight
    state.onSwipeUp = onSwipeUp
    state.topProvider = { topItem }

    LaunchedEffect(stackKey) {
        state.resetForNewCard()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                state.metrics(it.width.toFloat(), it.height.toFloat())
            }
    ) {
        if (nextItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 0.945f
                        scaleY = 0.945f
                        translationY = 26.dp.toPx()
                        alpha = 0.92f
                    }
            ) { card(nextItem) }
        }

        if (topItem != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val value = state.offset.value
                        val entranceProgress = state.entranceValue()
                        translationX = value.x
                        translationY = value.y + (1f - entranceProgress) * 30.dp.toPx()
                        rotationZ = value.x / max(state.widthPx, 1f) * 16f
                        val scale = 0.965f + 0.035f * entranceProgress
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.5f + 0.5f * entranceProgress
                    }
                    .pointerInput(topItem.id, stackKey) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            state.onDragStart()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                val delta = change.positionChange()
                                change.consume()
                                if (change.pressed) {
                                    state.onDrag(
                                        delta.x,
                                        delta.y,
                                        change.uptimeMillis,
                                        change.position
                                    )
                                } else {
                                    state.onDragEnd()
                                    break
                                }
                            }
                        }
                    }
            ) {
                card(topItem)

                val progress = min(1f, abs(state.offset.value.x) / max(state.widthPx * 0.22f, 1f))
                SwipeStamp(
                    text = stringResource(R.string.delete_stamp),
                    color = MaterialTheme.colorScheme.error,
                    rotation = -12f,
                    alpha = if (state.offset.value.x < 0f) progress else 0f,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 22.dp, start = 22.dp)
                )
                SwipeStamp(
                    text = stringResource(R.string.keep_stamp),
                    color = MaterialTheme.colorScheme.primary,
                    rotation = 12f,
                    alpha = if (state.offset.value.x > 0f) progress else 0f,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 22.dp, end = 22.dp)
                )
            }
        }
    }
}

@Composable
private fun SwipeStamp(
    text: String,
    color: Color,
    rotation: Float,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f) return
    Box(
        modifier = modifier
            .wrapContentSize()
            .graphicsLayer {
                rotationZ = rotation
            }
            .alpha(alpha)
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
            modifier = Modifier
                .border(
                    width = 3.dp,
                    color = color,
                    shape = RoundedCornerShape(10.dp)
                )
                .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

/** Velocity helper: falls back to zero velocity when no samples were captured. */
private fun VelocityTracker.computeVelocityOrNull(): Offset = try {
    val velocity = calculateVelocity()
    Offset(velocity.x, velocity.y)
} catch (_: Exception) {
    Offset.Zero
}
