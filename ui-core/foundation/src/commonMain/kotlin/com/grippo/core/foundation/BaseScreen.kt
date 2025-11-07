package com.grippo.core.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import androidx.compose.ui.graphics.Color as ComposeColor

@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(
        val value: ComposeColor,
        val spot: Spot? = null
    ) : ScreenBackground

    @Immutable
    public data class Spot(
        val top: ComposeColor,
        val bottom: ComposeColor,
    )
}

@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current


    Column(
        modifier = Modifier
            .background(background.value)
            .spots(background.spot)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus(force = true) }
            ),
        content = content
    )
}

@Composable
private fun Modifier.spots(spot: ScreenBackground.Spot?): Modifier {
    if (spot == null) return this

    // Subtle perpetual motion
    val infinite = rememberInfiniteTransition(label = "bg-spots")
    val phase1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(16000, easing = LinearEasing)),
        label = "phase1"
    )
    val phase2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(22000, easing = LinearEasing)),
        label = "phase2"
    )

    // Max drift relative to the shortest side (kept tiny to avoid nausea)
    val drift = 0.035f

    return this.then(
        Modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            val w = size.width
            val h = size.height
            val dimension = max(w, h)

            // Animated centers (base + drift)
            val topCenter = GradientDefaults.topSpot.centerFraction +
                    Offset(x = sin(phase1) * drift, y = cos(phase1 * 0.8f) * drift)
            val bottomCenter = GradientDefaults.bottomSpot.centerFraction +
                    Offset(x = cos(phase2) * drift, y = sin(phase2 * 0.7f) * drift)

            // Spots — soft, multi-stop, large radii to remove “cheap” edges
            drawSoftSpot(
                color = spot.top,
                centerFraction = topCenter,
                radiusFraction = GradientDefaults.topSpot.radiusFraction
            )
            drawSoftSpot(
                color = spot.bottom,
                centerFraction = bottomCenter,
                radiusFraction = GradientDefaults.bottomSpot.radiusFraction
            )

            // Diagonal wash for “brand depth”
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        spot.top.copy(alpha = 0.06f),
                        Color.Transparent,
                        spot.bottom.copy(alpha = 0.08f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                size = size
            )

            // Vignette to bind layers together
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.18f)
                    ),
                    center = Offset(w * 0.5f, h * 0.55f),
                    radius = dimension * 0.9f
                ),
                center = Offset(w * 0.5f, h * 0.55f),
                radius = dimension * 0.9f
            )
        }
    )
}

private data class SpotDefinition(
    val centerFraction: Offset,
    val radiusFraction: Float
)

// Multi-stop radial with gentle falloff to avoid banding
private fun DrawScope.drawSoftSpot(
    color: Color,
    centerFraction: Offset,
    radiusFraction: Float
) {
    val dimension = max(size.width, size.height)
    val center = Offset(size.width * centerFraction.x, size.height * centerFraction.y)
    val radius = dimension * radiusFraction

    // 4 stops: quick inner bloom, long soft tail, fully transparent edge
    val stops = arrayOf(
        0.0f to color.copy(alpha = color.alpha * 0.20f),
        0.35f to color.copy(alpha = color.alpha * 0.10f),
        0.75f to color.copy(alpha = color.alpha * 0.04f),
        1.0f to Color.Transparent
    )

    drawCircle(
        brush = Brush.radialGradient(
            colorStops = stops,
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}

private object GradientDefaults {
    // Slightly off-canvas to avoid hard outlines on corners
    val topSpot: SpotDefinition = SpotDefinition(
        centerFraction = Offset(-0.08f, 0.18f),
        radiusFraction = 0.42f
    )
    val bottomSpot: SpotDefinition = SpotDefinition(
        centerFraction = Offset(1.06f, 0.88f),
        radiusFraction = 0.48f
    )
}