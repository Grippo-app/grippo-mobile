package com.grippo.core.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFocusManager
import kotlin.math.max
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

    return this.then(
        Modifier.drawBehind {
            if (size.width <= 0f || size.height <= 0f) return@drawBehind

            val dimension = max(size.width, size.height)

            drawGradientSpot(
                color = spot.top,
                spot = GradientDefaults.topSpot,
                dimension = dimension
            )

            drawGradientSpot(
                color = spot.bottom,
                spot = GradientDefaults.bottomSpot,
                dimension = dimension
            )
        }
    )
}

private data class SpotDefinition(
    val centerFraction: Offset,
    val radiusFraction: Float
)

private fun DrawScope.drawGradientSpot(
    color: Color,
    spot: SpotDefinition,
    dimension: Float
) {
    val center = Offset(
        x = size.width * spot.centerFraction.x,
        y = size.height * spot.centerFraction.y
    )
    val radius = dimension * spot.radiusFraction

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = color.alpha * 0.22f),
                color.copy(alpha = color.alpha * 0.06f),
                Color.Transparent
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}

private object GradientDefaults {
    val topSpot: SpotDefinition = SpotDefinition(
        centerFraction = Offset(-0.05f, 0.2f),
        radiusFraction = 0.32f
    )

    val bottomSpot: SpotDefinition = SpotDefinition(
        centerFraction = Offset(1.08f, 0.86f),
        radiusFraction = 0.36f
    )
}