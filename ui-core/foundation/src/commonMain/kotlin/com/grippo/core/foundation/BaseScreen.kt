package com.grippo.core.foundation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.max
import androidx.compose.ui.graphics.Color as ComposeColor

@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(
        val value: ComposeColor,
        val ambient: Ambient? = null
    ) : ScreenBackground

    @Immutable
    public data class Ambient(
        val color: ComposeColor,
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
            .ambient(background.ambient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusManager.clearFocus(force = true) }
            ),
        content = content
    )
}

@Composable
private fun Modifier.ambient(ambient: ScreenBackground.Ambient?): Modifier {
    if (ambient == null) return this

    // Very low-amplitude alpha breathing, not position drift.
    val infinite = rememberInfiniteTransition(label = "bg-breath")
    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg-breath-alpha"
    )

    return this.then(
        Modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            val w = size.width
            val h = size.height
            val dimension = max(w, h)

            // 1) main ambient glow
            run {
                val center = Offset(x = w * 0.5f, y = h * 0.33f)
                val radius = dimension * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to ambient.color.copy(alpha = 0f),
                            0.4f to ambient.color.copy(alpha = 0.08f * pulse),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }

            // 2) vertical depth wash (bottom darkening)
            run {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        ),
                        startY = h * 0.4f,
                        endY = h
                    ),
                    size = size
                )
            }

            // 3) subtle edge vignette
            run {
                val vignetteRadius = dimension * 0.95f
                val vignetteCenter = Offset(x = w * 0.5f, y = h * 0.5f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.7f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.12f)
                        ),
                        center = vignetteCenter,
                        radius = vignetteRadius
                    ),
                    center = vignetteCenter,
                    radius = vignetteRadius
                )
            }
        }
    )
}
