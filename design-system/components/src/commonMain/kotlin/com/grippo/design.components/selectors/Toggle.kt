package com.grippo.design.components.selectors

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

/** Without [onCheckedChange], renders a passive indicator for an interactive parent. */
@Composable
public fun Toggle(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val toggle = AppTokens.colors.toggle
    val dp = AppTokens.dp.toggle
    val thumbPosition by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ToggleThumb"
    )
    val trackStart = when {
        !enabled -> toggle.disabledTrack
        checked -> toggle.checkedTrack1
        else -> toggle.uncheckedTrack1
    }
    val trackEnd = when {
        !enabled -> toggle.disabledTrack
        checked -> toggle.checkedTrack2
        else -> toggle.uncheckedTrack2
    }
    val trackBrush = remember(trackStart, trackEnd) {
        Brush.horizontalGradient(listOf(trackStart, trackEnd))
    }
    val thumbColor = when {
        !enabled -> toggle.disabledThumb
        checked -> toggle.checkedThumb
        else -> toggle.uncheckedThumb
    }
    val interactionModifier = if (onCheckedChange != null) {
        Modifier
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (contentDescription != null) this.contentDescription = contentDescription
                if (!enabled) disabled()
            }
            .scalableClick(
                enabled = enabled,
                onClick = onCheckedChange,
                haptic = true,
                role = Role.Switch,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(interactionModifier)
            .size(width = dp.width, height = dp.height)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = trackBrush,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(dp.radius.toPx(), dp.radius.toPx())
            )

            val thumbOffset = calculateThumbOffset(
                start = dp.horizontalPadding.toPx(),
                stop = size.width - dp.horizontalPadding.toPx(),
                fraction = thumbPosition
            )

            drawCircle(
                color = thumbColor,
                radius = dp.thumbRadius.toPx(),
                center = Offset(x = thumbOffset, y = size.height / 2)
            )
        }
    }
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
        Toggle(
            checked = true,
            onCheckedChange = {}
        )

        Toggle(
            checked = false,
            onCheckedChange = {}
        )

        Toggle(checked = false, enabled = false, onCheckedChange = {})
        Toggle(checked = true, enabled = false, onCheckedChange = {})
    }
}

@AppPreview
@Composable
private fun ToggleIndicatorPreview() {
    PreviewContainer {
        Toggle(checked = false)
        Toggle(checked = true)
        Toggle(checked = false, enabled = false)
        Toggle(checked = true, enabled = false)
    }
}
