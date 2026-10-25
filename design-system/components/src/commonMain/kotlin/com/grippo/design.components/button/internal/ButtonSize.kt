package com.grippo.design.components.button.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.core.AppTokens

@Immutable
internal data class ButtonMetrics(
    val height: Dp,
    val radius: Dp,
    val horizontalPadding: Dp,
    val space: Dp,
    val spaceTransparent: Dp,
    val icon: Dp,
)

@Composable
internal fun resolveButtonSize(size: ButtonSize): ButtonMetrics {
    return when (size) {
        ButtonSize.Medium -> ButtonMetrics(
            height = AppTokens.dp.button.medium.height,
            radius = AppTokens.dp.button.medium.radius,
            horizontalPadding = AppTokens.dp.button.medium.horizontalPadding,
            space = AppTokens.dp.button.medium.space,
            spaceTransparent = AppTokens.dp.button.medium.spaceTransparent,
            icon = AppTokens.dp.button.medium.icon,
        )

        ButtonSize.Small -> ButtonMetrics(
            height = AppTokens.dp.button.small.height,
            radius = AppTokens.dp.button.small.radius,
            horizontalPadding = AppTokens.dp.button.small.horizontalPadding,
            space = AppTokens.dp.button.small.space,
            spaceTransparent = AppTokens.dp.button.small.spaceTransparent,
            icon = AppTokens.dp.button.small.icon,
        )
    }
}

@Composable
internal fun defaultButtonTextStyle(size: ButtonSize): TextStyle {
    return when (size) {
        ButtonSize.Small -> AppTokens.typography.b12Semi()
        ButtonSize.Medium -> AppTokens.typography.b14Bold()
    }
}

// Mirrors Button's text layout, rounding each padding to pixels separately as the button does; change both together.
@Composable
internal fun rememberTextButtonSize(text: String, size: ButtonSize): DpSize {
    val metrics = resolveButtonSize(size)
    val textStyle = defaultButtonTextStyle(size)
    val verticalPadding = AppTokens.dp.button.verticalPadding
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    return remember(text, metrics, textStyle, verticalPadding, textMeasurer, density) {
        val label = textMeasurer.measure(text = text, style = textStyle, maxLines = 1)
        with(density) {
            val width = label.size.width + metrics.horizontalPadding.roundToPx() * 2
            val height = maxOf(metrics.height.roundToPx(), label.size.height + verticalPadding.roundToPx() * 2)
            DpSize(width = width.toDp(), height = height.toDp())
        }
    }
}
