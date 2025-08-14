package com.grippo.design.components.selectors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Canvas(
        modifier = modifier
            .semantics {
                this.selected = selected
            }
            .scalableClick(onClick = onSelectedChange)
            .size(dp.size)
    ) {
        val center = Offset(x = size.width / 2, y = size.height / 2)

        val outerColor = when {
            enabled -> if (selected) colors.selectedTrack else colors.unselectedTrack
            else -> colors.disabledUnselectedTrack
        }

        drawCircle(
            color = outerColor,
            radius = size.minDimension / 2,
            center = center,
            style = Stroke(width = dp.borderWidth.toPx())
        )

        if (selected) {
            val innerColor = if (enabled) colors.selectedThumb else colors.disabledSelectedThumb

            drawCircle(
                color = innerColor,
                radius = dp.innerCircleRadius.toPx(),
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