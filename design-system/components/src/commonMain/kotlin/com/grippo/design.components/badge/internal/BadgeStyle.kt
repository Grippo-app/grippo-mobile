package com.grippo.design.components.badge.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.badge.BadgeStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.icons.Warning

@Stable
internal data class BadgeTokens(
    val background: Color,
    val content: Color,
    val icon: ImageVector,
)

@Composable
internal fun resolveBadgeTokens(style: BadgeStyle): BadgeTokens {
    val semantic = AppTokens.colors.semantic
    val icons = AppTokens.icons
    return when (style) {
        BadgeStyle.Error -> BadgeTokens(
            background = semantic.error.copy(alpha = 0.15f),
            content = semantic.error,
            icon = icons.Cancel,
        )

        BadgeStyle.Warning -> BadgeTokens(
            background = semantic.warning.copy(alpha = 0.15f),
            content = semantic.warning,
            icon = icons.Warning,
        )

        BadgeStyle.Success -> BadgeTokens(
            background = semantic.success.copy(alpha = 0.15f),
            content = semantic.success,
            icon = icons.Check,
        )
    }
}
