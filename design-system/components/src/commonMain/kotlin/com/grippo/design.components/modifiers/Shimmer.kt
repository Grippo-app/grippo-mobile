package com.grippo.design.components.modifiers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.persistentListOf

public fun Modifier.shimmerAnimation(
    visible: Boolean,
    widthOfShadowBrush: Int = 500,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1400,
    radius: Dp = 8.dp,
): Modifier {
    return if (visible) {
        composed {
            val color1 = AppTokens.colors.skeleton.background
            val color2 = AppTokens.colors.skeleton.shimmer

            val shimmerColors = persistentListOf(
                color1,
                color2,
                color1,
            )

            val transition = rememberInfiniteTransition(label = "")
            val translateAnimation = transition.animateFloat(
                initialValue = 0f,
                targetValue = (durationMillis + widthOfShadowBrush).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = durationMillis,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "Shimmer loading animation",
            )

            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateAnimation.value - widthOfShadowBrush, 0f),
                end = Offset(translateAnimation.value, angleOfAxisY),
            )

            val radiusPx = with(LocalDensity.current) { radius.toPx() }

            this.drawWithContent {
                drawContent()
                drawRoundRect(
                    brush = brush,
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    blendMode = BlendMode.SrcOver,
                )
            }
        }
    } else {
        this
    }
}
