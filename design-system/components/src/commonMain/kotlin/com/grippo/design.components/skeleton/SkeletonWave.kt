package com.grippo.design.components.skeleton

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
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
            withFrameMillis { frameTime ->
                val elapsed = frameTime % (WAVE_SWEEP_MILLIS + WAVE_PAUSE_MILLIS)
                value = if (elapsed < WAVE_SWEEP_MILLIS) {
                    EaseInOutSine.transform(elapsed / WAVE_SWEEP_MILLIS.toFloat())
                } else {
                    null
                }
            }
        }
    }
    val placement = remember { WavePlacement() }

    return onGloballyPositioned { coordinates ->
        placement.origin = coordinates.positionInRoot()
        placement.rootSize = coordinates.findRootCoordinates().size
    }.drawWithCache {
        val band = waveWidth.toPx()
        val tilt = WAVE_TILT_DEGREES * PI.toFloat() / 180f
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val direction = Offset(x = if (isLtr) cos(tilt) else -cos(tilt), y = sin(tilt))
        val brush = Brush.linearGradient(
            *waveStops(highlight),
            start = Offset.Zero,
            end = direction * band,
        )
        onDrawWithContent {
            drawContent()
            val fraction = progress.value ?: return@onDrawWithContent
            val root = placement.rootSize
            val travel = root.width * cos(tilt) + root.height * sin(tilt) + band
            val rootStart = Offset(x = if (isLtr) 0f else root.width.toFloat(), y = 0f)
            val anchor = rootStart + direction * (fraction * travel - band) - placement.origin
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

private class WavePlacement {
    var origin: Offset = Offset.Zero
    var rootSize: IntSize = IntSize.Zero
}
