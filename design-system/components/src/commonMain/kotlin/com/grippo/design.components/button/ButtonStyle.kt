package com.grippo.design.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    icon: ImageVector? = null,
    iconTint: Color? = null,
): ButtonColorTokens {
    val colors = AppTokens.colors.button
    val borders = AppTokens.colors.border
    val icons = AppTokens.colors.icon
    val fallbackIconTint = iconTint ?: icons.default

    return when (state) {
        ButtonState.Disabled -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                colors.backgroundPrimaryDisabled,
                colors.contentPrimaryDisabled,
                Color.Transparent,
                fallbackIconTint
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                colors.backgroundSecondaryDisabled,
                colors.contentSecondaryDisabled,
                borders.disabledSecondary,
                fallbackIconTint
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                colors.backgroundTertiaryDisabled,
                colors.contentTertiaryDisabled,
                borders.disabledTertiary,
                fallbackIconTint
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                Color.Transparent,
                colors.contentTransparentDisabled,
                Color.Transparent,
                fallbackIconTint
            )
        }

        else -> when (style) {
            ButtonStyle.Primary -> ButtonColorTokens(
                colors.backgroundPrimary,
                colors.contentPrimary,
                Color.Transparent,
                fallbackIconTint
            )

            ButtonStyle.Secondary -> ButtonColorTokens(
                colors.backgroundSecondary,
                colors.contentSecondary,
                borders.defaultSecondary,
                fallbackIconTint
            )

            ButtonStyle.Tertiary -> ButtonColorTokens(
                colors.backgroundTertiary,
                colors.contentTertiary,
                borders.defaultTertiary,
                fallbackIconTint
            )

            ButtonStyle.Transparent -> ButtonColorTokens(
                Color.Transparent,
                colors.contentSecondary,
                Color.Transparent,
                fallbackIconTint
            )
        }
    }
}