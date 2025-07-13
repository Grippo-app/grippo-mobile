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
                border = Color.Transparent,
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
                content = colors.textPrimary,
                border = Color.Transparent,
                icon = colors.iconPrimary,
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background = colors.backgroundSecondary,
                content = colors.textSecondary,
                border = colors.borderSecondary,
                icon = colors.iconSecondary,
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background = Color.Transparent,
                content = colors.textTransparent,
                border = Color.Transparent,
                icon = colors.iconTransparent,
            )
        }
    }
}