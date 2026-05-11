package com.grippo.design.components.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.banner.internal.resolveBannerCardTokens
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.Trophy

@Immutable
public sealed interface BannerCardStyle {
    @Immutable
    public data object Notice : BannerCardStyle

    @Immutable
    public data object Info : BannerCardStyle

    @Immutable
    public data object Success : BannerCardStyle

    @Immutable
    public data object Warning : BannerCardStyle

    @Immutable
    public data object Error : BannerCardStyle

    @Immutable
    public data class Custom(val tint: Color) : BannerCardStyle
}

@Composable
public fun BannerCard(
    modifier: Modifier = Modifier,
    style: BannerCardStyle,
    icon: ImageVector,
    title: String,
    description: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = resolveBannerCardTokens(style)
    val dp = AppTokens.dp.bannerCard

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dp.space),
    ) {
        Box(
            modifier = Modifier
                .size(dp.iconBackground)
                .background(color = tokens.surface, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(dp.icon),
                imageVector = icon,
                tint = tokens.accent,
                contentDescription = null,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dp.textSpacing),
        ) {
            Text(
                text = title,
                style = tokens.titleStyle,
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (description != null) {
                Text(
                    text = description,
                    style = tokens.descriptionStyle,
                    color = AppTokens.colors.text.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailing != null) {
            trailing.invoke()
        }
    }
}

@AppPreview
@Composable
private fun BannerCardPreview() {
    PreviewContainer {
        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Notice,
            icon = AppTokens.icons.Sparkle,
            title = "Suggested preset",
            description = "A balanced session generated for you.",
            trailing = {
                Text(
                    text = "4 exercises",
                    style = AppTokens.typography.b11Semi(),
                    color = AppTokens.colors.semantic.notice,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Info,
            icon = AppTokens.icons.Timer,
            title = "Recent workout",
            description = "Yesterday, 18:42",
            trailing = {
                Text(
                    text = "12 exercises",
                    style = AppTokens.typography.b11Semi(),
                    color = AppTokens.colors.semantic.info,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Success,
            icon = AppTokens.icons.Trophy,
            title = "Goal reached",
            description = "Three sessions this week — keep going.",
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Warning,
            icon = AppTokens.icons.Timer,
            title = "Title only",
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(AppTokens.colors.brand.color2),
            icon = AppTokens.icons.LineUp,
            title = "Custom tint",
            description = "Pass any Color via Custom — useful for brand or context palettes.",
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(AppTokens.colors.context.goal),
            icon = AppTokens.icons.Sparkle,
            title = "My test message with title",
            description = "Super long description. Super long description. Super long description. Super long description. Super long description.",
        )
    }
}
