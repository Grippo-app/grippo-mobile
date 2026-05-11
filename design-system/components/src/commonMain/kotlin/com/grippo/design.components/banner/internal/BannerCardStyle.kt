package com.grippo.design.components.banner.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.core.AppTokens

@Stable
internal data class BannerCardTokens(
    val accent: Color,
    val surface: Color,
    val titleColor: Color,
    val descriptionColor: Color,
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle,
)

@Composable
internal fun resolveBannerCardTokens(
    style: BannerCardStyle,
    enabled: Boolean,
): BannerCardTokens {
    val titleStyle = AppTokens.typography.b14Semi()
    val descriptionStyle = AppTokens.typography.b12Med()

    if (!enabled) {
        val disabledIcon = AppTokens.colors.icon.disabled
        val disabledText = AppTokens.colors.text.disabled
        return BannerCardTokens(
            accent = disabledIcon,
            surface = disabledIcon.copy(alpha = 0.14f),
            titleColor = disabledText,
            descriptionColor = disabledText,
            titleStyle = titleStyle,
            descriptionStyle = descriptionStyle,
        )
    }

    val semantic = AppTokens.colors.semantic
    val accent = when (style) {
        BannerCardStyle.Notice -> semantic.notice
        BannerCardStyle.Info -> semantic.info
        BannerCardStyle.Success -> semantic.success
        BannerCardStyle.Warning -> semantic.warning
        BannerCardStyle.Error -> semantic.error
        is BannerCardStyle.Custom -> style.tint
    }

    return BannerCardTokens(
        accent = accent,
        surface = accent.copy(alpha = 0.14f),
        titleColor = AppTokens.colors.text.primary,
        descriptionColor = AppTokens.colors.text.secondary,
        titleStyle = titleStyle,
        descriptionStyle = descriptionStyle,
    )
}
