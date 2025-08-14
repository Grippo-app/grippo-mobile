package com.grippo.design.components.selectors

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun Toggle(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggle = AppTokens.colors.toggle
    val dp = AppTokens.dp.toggle
    val thumbPosition by animateFloatAsState(targetValue = if (checked) 1f else 0f, label = "ToggleThumb")

    Box(
        modifier = modifier
            .scalableClick(onClick = onCheckedChange)
            .size(width = dp.width, height = dp.height)
            .background(color = Color.Transparent)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val backgroundColor = if (checked) toggle.checkedTrack else toggle.uncheckedTrack

            drawRoundRect(
                color = backgroundColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(dp.radius.toPx(), dp.radius.toPx())
            )

            val thumbOffset = calculateThumbOffset(
                start = dp.horizontalPadding.toPx(),
                stop = size.width - dp.horizontalPadding.toPx(),
                fraction = thumbPosition
            )

            val thumbColor = if (checked) toggle.checkedThumb else toggle.uncheckedThumb

            drawCircle(
                color = thumbColor,
                radius = dp.thumbRadius.toPx(),
                center = Offset(x = thumbOffset, y = size.height / 2)
            )
        }
    }
}

@Composable
public fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Toggle(
        checked = checked,
        onCheckedChange = { onCheckedChange(!checked) },
        modifier = modifier,
    )
}

private fun calculateThumbOffset(
    start: Float,
    stop: Float,
    fraction: Float
): Float = start + (stop - start) * fraction

@AppPreview
@Composable
private fun TogglePreview() {
    PreviewContainer {
        val noop: () -> Unit = {}
        Toggle(
            checked = true,
            onCheckedChange = noop
        )

        Toggle(
            checked = false,
            onCheckedChange = noop
        )
    }
}