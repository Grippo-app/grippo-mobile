package com.grippo.design.components.modifiers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.LayoutDirection
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

/**
 * Draws a moving highlight behind existing content without changing its layout or semantics.
 * Apply after a shape clip. For skeletons, apply after their background as well.
 * Disabled instances create no animation; system animation scaling is respected.
 */
@Composable
public fun Modifier.shimmer(
    enabled: Boolean = true,
    highlightColor: Color = AppTokens.colors.shimmer.highlight,
): Modifier {
    if (!enabled) return this
    val progress = rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "shimmer_progress",
    )
    return drawWithCache {
        val bandWidth = size.width * 0.5f
        val brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
            start = Offset.Zero,
            end = Offset(bandWidth, size.height * 0.25f),
        )
        onDrawWithContent {
            val fraction = if (layoutDirection == LayoutDirection.Ltr) progress.value
                else 1f - progress.value
            val offset = (size.width + bandWidth) * fraction - bandWidth
            clipRect {
                translate(left = offset) {
                    drawRect(brush, size = size.copy(width = bandWidth))
                }
            }
            drawContent()
        }
    }
}

@AppPreview
@Composable
private fun ShimmerPreview() {
    PreviewContainer {
        Box(
            Modifier.fillMaxWidth()
                .height(AppTokens.dp.button.medium.height)
                .clip(CircleShape)
                .background(AppTokens.colors.background.card)
                .shimmer(),
        )
    }
}
