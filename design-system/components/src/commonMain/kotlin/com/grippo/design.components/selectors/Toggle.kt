package com.grippo.design.components.selectors

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@Composable
public fun Toggle(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: () -> Unit,
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
    val click = rememberUpdatedState(onCheckedChange)
    val disabledTrack = AppTokens.colors.button.backgroundDisabled
    val disabledThumb = AppTokens.colors.button.contentDisabled

    Box(
        modifier = modifier
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                if (contentDescription != null) this.contentDescription = contentDescription
                if (!enabled) disabled()
            }
            .scalableClick(enabled = enabled, onClick = click.value, haptic = true, role = Role.Switch)
            .size(width = dp.width, height = dp.height)
            .background(color = Color.Transparent)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val background1Color = when {
                !enabled -> disabledTrack
                checked -> toggle.checkedTrack1
                else -> toggle.uncheckedTrack1
            }
            val background2Color = when {
                !enabled -> disabledTrack
                checked -> toggle.checkedTrack2
                else -> toggle.uncheckedTrack2
            }

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    0f to background1Color,
                    1f to background2Color,
                ),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(dp.radius.toPx(), dp.radius.toPx())
            )

            val thumbOffset = calculateThumbOffset(
                start = dp.horizontalPadding.toPx(),
                stop = size.width - dp.horizontalPadding.toPx(),
                fraction = thumbPosition
            )

            val thumbColor = when {
                !enabled -> disabledThumb
                checked -> toggle.checkedThumb
                else -> toggle.uncheckedThumb
            }

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
