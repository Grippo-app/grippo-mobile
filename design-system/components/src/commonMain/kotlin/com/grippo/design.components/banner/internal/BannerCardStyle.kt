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
    val background: Color,
    val border: Color,
    val surface: Color,
    val titleStyle: TextStyle,
    val descriptionStyle: TextStyle,
)

@Composable
internal fun resolveBannerCardTokens(style: BannerCardStyle): BannerCardTokens {
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
        background = accent.copy(alpha = 0.12f),
        border = accent.copy(alpha = 0.28f),
        surface = accent.copy(alpha = 0.14f),
        titleStyle = AppTokens.typography.b14Semi(),
        descriptionStyle = AppTokens.typography.b12Med(),
    )
}
