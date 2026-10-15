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
    val descriptionStyle = AppTokens.typography.b13Med()

    if (!enabled) {
        val disabledIcon = AppTokens.colors.icon.disabled
        val disabledText = AppTokens.colors.text.disabled
        return BannerCardTokens(
            accent = disabledIcon,
            surface = AppTokens.colors.bannerCard.disabledBackground,
            titleColor = disabledText,
            descriptionColor = disabledText,
            titleStyle = titleStyle,
            descriptionStyle = descriptionStyle,
        )
    }

    val semantic = AppTokens.colors.semantic
    val (accent, surface) = when (style) {
        BannerCardStyle.Notice -> semantic.notice to semantic.noticeBackground
        BannerCardStyle.Info -> semantic.info to semantic.infoBackground
        BannerCardStyle.Success -> semantic.success to semantic.successBackground
        BannerCardStyle.Warning -> semantic.warning to semantic.warningBackground
        BannerCardStyle.Error -> semantic.error to semantic.errorBackground
        is BannerCardStyle.Custom -> style.tint to style.surface
    }

    return BannerCardTokens(
        accent = accent,
        surface = surface,
        titleColor = AppTokens.colors.text.primary,
        descriptionColor = AppTokens.colors.text.secondary,
        titleStyle = titleStyle,
        descriptionStyle = descriptionStyle,
    )
}
