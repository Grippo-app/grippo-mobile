package com.grippo.design.components.selectors

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun Radio(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean = true,
    onSelectedChange: () -> Unit,
) {
    val colors = AppTokens.colors.radio
    val dp = AppTokens.dp.radio

    val density = LocalDensity.current
    val innerCircleRadiusPx = with(density) { dp.innerCircleRadius.toPx() }

    // Animate inner circle radius
    val animatedInnerRadius by animateFloatAsState(
        targetValue = if (selected) innerCircleRadiusPx else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "RadioInnerCircleAnimation"
    )

    // Animate outer color
    val targetOuterColor = when {
        enabled -> if (selected) colors.selectedTrack else colors.unselectedTrack
        else -> colors.disabledUnselectedTrack
    }
    val animatedOuterColor by animateColorAsState(
        targetValue = targetOuterColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "RadioOuterColorAnimation"
    )

    // Animate inner color
    val targetInnerColor = if (enabled) colors.selectedThumb else colors.disabledSelectedThumb
    val animatedInnerColor by animateColorAsState(
        targetValue = targetInnerColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "RadioInnerColorAnimation"
    )

    Canvas(
        modifier = modifier
            .semantics { this.selected = selected }
            .scalableClick(onClick = onSelectedChange)
            .size(dp.size)
    ) {
        val center = Offset(x = size.width / 2, y = size.height / 2)

        // Outer circle (animated color)
        drawCircle(
            color = animatedOuterColor,
            radius = size.minDimension / 2,
            center = center,
            style = Stroke(width = dp.borderWidth.toPx())
        )

        // Inner circle (animated radius + color)
        if (animatedInnerRadius > 0f) {
            drawCircle(
                color = animatedInnerColor,
                radius = animatedInnerRadius,
                center = center
            )
        }
    }
}

@AppPreview
@Composable
private fun RadioPreview() {
    PreviewContainer {
        val noop: () -> Unit = {}
        Radio(
            selected = true,
            enabled = true,
            onSelectedChange = noop
        )

        Radio(
            selected = false,
            enabled = true,
            onSelectedChange = noop
        )

        Radio(
            selected = true,
            enabled = false,
            onSelectedChange = noop
        )

        Radio(
            selected = false,
            enabled = false,
            onSelectedChange = noop
        )
    }
}