package com.grippo.design.components.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.grippo.design.core.AppTokens

@Immutable
internal data class ButtonMetrics(
    val height: Dp,
    val horizontalPadding: Dp,
    val space: Dp,
    val spaceTransparent: Dp,
    val icon: Dp,
)

@Composable
internal fun resolveButtonSize(
    size: ButtonSize,
): ButtonMetrics {
    return when (size) {
        ButtonSize.Medium -> ButtonMetrics(
            height = AppTokens.dp.button.medium.height,
            horizontalPadding = AppTokens.dp.button.medium.horizontalPadding,
            space = AppTokens.dp.button.medium.space,
            spaceTransparent = AppTokens.dp.button.medium.spaceTransparent,
            icon = AppTokens.dp.button.medium.icon
        )

        ButtonSize.Small -> ButtonMetrics(
            height = AppTokens.dp.button.small.height,
            horizontalPadding = AppTokens.dp.button.small.horizontalPadding,
            space = AppTokens.dp.button.small.space,
            spaceTransparent = AppTokens.dp.button.small.spaceTransparent,
            icon = AppTokens.dp.button.small.icon,
        )
    }
}
