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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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

    return this.composed {
        val infinite = rememberInfiniteTransition(label = "bg-ambient")

        val pulse by infinite.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ambient-pulse"
        )

        Modifier.drawBehind {
            if (size.minDimension <= 0f) return@drawBehind

            val w = size.width
            val h = size.height
            val d = max(w, h)

            val ambientSoft = lerp(ambient.color, Color.White, 0.14f)

            fun glow(
                center: Offset,
                radius: Float,
                alpha: Float
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to ambientSoft.copy(alpha = 0.0f),
                            0.35f to ambientSoft.copy(alpha = alpha * pulse),
                            1.0f to Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }

            // Big overlapping circles (soft, non-flashy)
            glow(
                center = Offset(x = w * 0.75f, y = h * 0.18f),
                radius = d * 0.95f,
                alpha = 0.028f
            )
            glow(
                center = Offset(x = w * 0.20f, y = h * 0.45f),
                radius = d * 0.80f,
                alpha = 0.018f
            )
            glow(
                center = Offset(x = w * 0.80f, y = h * 0.85f),
                radius = d * 0.70f,
                alpha = 0.012f
            )

            // Bottom depth wash
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.16f)
                    ),
                    startY = 0f,
                    endY = h
                ),
                size = size
            )

            // Gentle vignette
            run {
                val center = Offset(x = w * 0.5f, y = h * 0.5f)
                val radius = d * 1.10f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.80f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.11f)
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )
            }
        }
    }
}

