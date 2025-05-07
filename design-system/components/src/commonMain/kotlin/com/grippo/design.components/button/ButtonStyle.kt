package com.grippo.design.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens

internal data class ButtonColorTokens(
    val background: Color,
    val content: Color,
    val border: Color,
    val icon: Color
)

@Composable
internal fun resolveButtonColors(
    style: ButtonStyle,
    state: ButtonState,
): ButtonColorTokens {
    val colors = AppTokens.colors.button
    val borders = AppTokens.colors.border

    return when (state) {
        ButtonState.Disabled -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                background = colors.backgroundPrimaryDisabled,
                content = colors.contentPrimaryDisabled,
                border = Color.Transparent,
                icon = colors.contentPrimaryDisabled
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background = colors.backgroundSecondaryDisabled,
                content = colors.contentSecondaryDisabled,
                border = borders.disabledSecondary,
                icon = colors.contentSecondaryDisabled
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background = Color.Transparent,
                content = colors.contentTransparentDisabled,
                border = Color.Transparent,
                icon = colors.contentTransparentDisabled
            )
        }

        else -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                background = colors.backgroundPrimary,
                content = colors.contentPrimary,
                border = Color.Transparent,
                icon = colors.contentPrimary,
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background = colors.backgroundSecondary,
                content = colors.contentSecondary,
                border = borders.defaultSecondary,
                icon = colors.contentSecondary,
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background = Color.Transparent,
                content = colors.contentSecondary,
                border = Color.Transparent,
                icon = colors.contentSecondary,
            )
        }
    }
}