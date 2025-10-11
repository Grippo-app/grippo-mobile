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
                background1 = colors.backgroundPrimaryDisabled,
                background2 = colors.backgroundPrimaryDisabled,
                content = colors.contentPrimaryDisabled,
                border = Color.Transparent,
                icon = colors.contentPrimaryDisabled
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background1 = colors.backgroundSecondaryDisabled,
                background2 = colors.backgroundSecondaryDisabled,
                content = colors.contentSecondaryDisabled,
                border = Color.Transparent,
                icon = colors.contentSecondaryDisabled
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                background1 = colors.backgroundTertiaryDisabled,
                background2 = colors.backgroundTertiaryDisabled,
                content = colors.contentTertiaryDisabled,
                border = Color.Transparent,
                icon = colors.contentTertiaryDisabled
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                background1 = Color.Transparent,
                background2 = Color.Transparent,
                content = colors.contentTransparentDisabled,
                border = Color.Transparent,
                icon = colors.contentTransparentDisabled
            )

            ButtonStyle.Error -> ButtonColorTokens(
                background1 = AppTokens.colors.button.backgroundPrimaryDisabled,
                background2 = AppTokens.colors.button.backgroundPrimaryDisabled,
                content = AppTokens.colors.button.contentPrimaryDisabled,
                border = Color.Transparent,
                icon = AppTokens.colors.button.contentPrimaryDisabled
            )

            ButtonStyle.Warning -> ButtonColorTokens(
                background1 = AppTokens.colors.button.backgroundPrimaryDisabled,
                background2 = AppTokens.colors.button.backgroundPrimaryDisabled,
                content = AppTokens.colors.button.contentPrimaryDisabled,
                border = Color.Transparent,
                icon = AppTokens.colors.button.contentPrimaryDisabled
            )

            ButtonStyle.Magic -> ButtonColorTokens(
                background1 = AppTokens.colors.button.backgroundPrimaryDisabled,
                background2 = AppTokens.colors.button.backgroundPrimaryDisabled,
                content = AppTokens.colors.button.contentPrimaryDisabled,
                border = Color.Transparent,
                icon = AppTokens.colors.button.contentPrimaryDisabled
            )
        }

        else -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                background1 = colors.backgroundPrimary1,
                background2 = colors.backgroundPrimary2,
                content = colors.textPrimary,
                border = Color.Transparent,
                icon = colors.iconPrimary,
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                background1 = colors.backgroundSecondary1,
                background2 = colors.backgroundSecondary2,
                content = colors.textSecondary,
                border = colors.borderSecondary,
                icon = colors.iconSecondary,
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                background1 = colors.backgroundTertiary1,
                background2 = colors.backgroundTertiary2,
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

            ButtonStyle.Warning -> ButtonColorTokens(
                background1 = AppTokens.colors.semantic.warning,
                background2 = AppTokens.colors.semantic.warning,
                icon = AppTokens.colors.button.iconPrimary,
                content = AppTokens.colors.button.textPrimary,
                border = AppTokens.colors.semantic.warning,
            )

            ButtonStyle.Magic -> ButtonColorTokens(
                background1 = AppTokens.colors.aiSuggestion.background1,
                background2 = AppTokens.colors.aiSuggestion.background2,
                icon = AppTokens.colors.aiSuggestion.content,
                content = AppTokens.colors.aiSuggestion.content,
                border = AppTokens.colors.aiSuggestion.border,
            )
        }
    }
}