package com.grippo.design.components.skeleton

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.grippo.design.core.AppTokens
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WAVE_SWEEP_MILLIS = 1700L
private const val WAVE_PAUSE_MILLIS = 500L
private const val WAVE_TILT_DEGREES = 15f
private const val WAVE_PROFILE_STEPS = 8

@Composable
internal fun Modifier.skeletonWave(enabled: Boolean): Modifier {
    if (!enabled) return this
    val highlight = AppTokens.colors.skeleton.highlight
    val waveWidth = AppTokens.dp.skeleton.waveWidth
    val progress = produceState<Float?>(initialValue = null) {
        while (true) {
            val motionScale = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
            if (motionScale > 0f) {
                withInfiniteAnimationFrameMillis { frameTime ->
                    val elapsed = (frameTime / motionScale).toLong() % (WAVE_SWEEP_MILLIS + WAVE_PAUSE_MILLIS)
                    value = if (elapsed < WAVE_SWEEP_MILLIS) {
                        EaseInOutSine.transform(elapsed / WAVE_SWEEP_MILLIS.toFloat())
                    } else {
                        null
                    }
                }
            } else {
                value = null
                snapshotFlow { coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f }.first { it > 0f }
            }
        }
    }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }

    return onGloballyPositioned { coordinates ->
        origin = coordinates.positionInRoot()
        rootSize = coordinates.findRootCoordinates().size
    }.drawWithCache {
        val band = waveWidth.toPx()
        val tilt = WAVE_TILT_DEGREES * PI.toFloat() / 180f
        val cosTilt = cos(tilt)
        val sinTilt = sin(tilt)
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val direction = Offset(x = if (isLtr) cosTilt else -cosTilt, y = sinTilt)
        val brush = Brush.linearGradient(
            *waveStops(highlight),
            start = Offset.Zero,
            end = direction * band,
        )
        onDrawWithContent {
            drawContent()
            val fraction = progress.value ?: return@onDrawWithContent
            val travel = rootSize.width * cosTilt + rootSize.height * sinTilt + band
            val rootStart = Offset(x = if (isLtr) 0f else rootSize.width.toFloat(), y = 0f)
            val anchor = rootStart + direction * (fraction * travel - band) - origin
            translate(left = anchor.x, top = anchor.y) {
                drawRect(brush = brush, topLeft = -anchor, size = size)
            }
        }
    }
}

private fun waveStops(highlight: Color): Array<Pair<Float, Color>> {
    return Array(WAVE_PROFILE_STEPS + 1) { step ->
        val position = step / WAVE_PROFILE_STEPS.toFloat()
        val intensity = (1f - cos(2f * PI.toFloat() * position)) / 2f
        position to highlight.copy(alpha = highlight.alpha * intensity)
    }
}
