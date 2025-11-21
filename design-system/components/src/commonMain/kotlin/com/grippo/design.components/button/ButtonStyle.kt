package com.grippo.design.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.grippo.design.core.AppTokens

public data class ButtonColorTokens(
    val background1: Color,
    val background2: Color,
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
                background1 = AppTokens.colors.button.backgroundDisabled,
                background2 = AppTokens.colors.button.backgroundDisabled,
                content = AppTokens.colors.button.contentDisabled,
                border = Color.Transparent,
                icon = AppTokens.colors.button.contentDisabled
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background1 = colors.backgroundDisabled,
                background2 = colors.backgroundDisabled,
                content = colors.contentDisabled,
                border = Color.Transparent,
                icon = colors.contentDisabled
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                background1 = colors.backgroundDisabled,
                background2 = colors.backgroundDisabled,
                content = colors.contentDisabled,
                border = Color.Transparent,
                icon = colors.contentDisabled
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background1 = Color.Transparent,
                background2 = Color.Transparent,
                content = colors.contentDisabled,
                border = Color.Transparent,
                icon = colors.contentDisabled
            )

            ButtonStyle.Error -> ButtonColorTokens(
                background1 = AppTokens.colors.button.backgroundDisabled,
                background2 = AppTokens.colors.button.backgroundDisabled,
                content = AppTokens.colors.button.contentDisabled,
                border = Color.Transparent,
                icon = AppTokens.colors.button.contentDisabled
            )
        }

        else -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                background1 = AppTokens.colors.button.backgroundPrimary1,
                background2 = AppTokens.colors.button.backgroundPrimary2,
                icon = AppTokens.colors.button.iconPrimary,
                content = AppTokens.colors.button.textPrimary,
                border = AppTokens.colors.button.borderPrimary,
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background1 = colors.backgroundSecondary1,
                background2 = colors.backgroundSecondary2,
                content = colors.textSecondary,
                border = colors.borderSecondary,
                icon = colors.iconSecondary,
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                background1 = AppTokens.colors.background.card,
                background2 = AppTokens.colors.background.card,
                content = colors.textTertiary,
                border = colors.borderTertiary,
                icon = colors.iconTertiary,
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background1 = Color.Transparent,
                background2 = Color.Transparent,
                content = colors.textTransparent,
                border = Color.Transparent,
                icon = colors.iconTransparent,
            )

            ButtonStyle.Error -> ButtonColorTokens(
                background1 = AppTokens.colors.semantic.error,
                background2 = AppTokens.colors.semantic.error,
                icon = AppTokens.colors.button.iconPrimary,
                content = AppTokens.colors.button.textPrimary,
                border = AppTokens.colors.semantic.error,
            )
        }
    }
}
