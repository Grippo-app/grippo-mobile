package com.grippo.design.components.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_description
import com.grippo.design.resources.provider.goal_setup_suggestion_benefit_tailored_title
import com.grippo.design.resources.provider.home_unlock_set_goal_cta
import com.grippo.design.resources.provider.home_unlock_set_goal_description
import com.grippo.design.resources.provider.home_unlock_set_goal_title
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Lock
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.onboarding_workouts_description
import com.grippo.design.resources.provider.onboarding_workouts_title

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
    public data class Custom(val tint: Color, val surface: Color) : BannerCardStyle
}

@Composable
public fun BannerCard(
    modifier: Modifier = Modifier,
    style: BannerCardStyle,
    icon: ImageVector,
    title: String,
    description: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val tokens = resolveBannerCardTokens(style, enabled)
    val dp = AppTokens.dp.bannerCard

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(dp.space),
    ) {
        Box(
            modifier = Modifier
                .size(dp.iconBackground)
                .background(color = tokens.surface, shape = RoundedCornerShape(dp.iconRadius)),
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
            modifier = Modifier
                .weight(1f)
                .heightIn(min = dp.iconBackground),
            verticalArrangement = Arrangement.spacedBy(dp.trailingSpacing, Alignment.CenterVertically),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(dp.textSpacing)) {
                Text(
                    text = title,
                    style = tokens.titleStyle,
                    color = tokens.titleColor,
                )

                if (description != null) {
                    Text(
                        text = description,
                        style = tokens.descriptionStyle,
                        color = tokens.descriptionColor,
                    )
                }
            }

            trailing?.invoke()
        }
    }
}

@AppPreview
@Composable
private fun BannerCardSemanticPreview() {
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
                    color = AppTokens.colors.text.secondary,
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
                    color = AppTokens.colors.text.secondary,
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
            style = BannerCardStyle.Error,
            icon = AppTokens.icons.Timer,
            title = "Unable to load workout",
            description = "Please try again.",
        )
    }
}

@AppPreview
@Composable
private fun BannerCardBenefitsPreview() {
    PreviewContainer {
        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.icon.secondary,
                surface = AppTokens.colors.bannerCard.mutedBackground,
            ),
            icon = AppTokens.icons.Dumbbell,
            title = AppTokens.strings.res(Res.string.onboarding_workouts_title),
            description = AppTokens.strings.res(Res.string.onboarding_workouts_description),
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.illustration.accent,
                surface = AppTokens.colors.bannerCard.illustrationBackground,
            ),
            icon = AppTokens.icons.LineUp,
            title = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title),
            description = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description),
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.illustration.accent,
                surface = AppTokens.colors.bannerCard.illustrationBackground,
            ),
            icon = AppTokens.icons.Sparkle,
            title = "A longer title that wraps without losing the benefit",
            description = "Keep the full explanation readable when a benefit needs more than two lines of supporting text, including on a narrow phone.",
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.illustration.accent,
                surface = AppTokens.colors.bannerCard.illustrationBackground,
            ),
            icon = AppTokens.icons.Lock,
            title = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_title),
            description = AppTokens.strings.res(Res.string.goal_setup_suggestion_benefit_tailored_description),
            enabled = false,
        )
    }
}

@AppPreview
@Composable
private fun BannerCardTrailingPreview() {
    PreviewContainer {
        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.text.tertiary,
                surface = AppTokens.colors.bannerCard.mutedBackground,
            ),
            icon = AppTokens.icons.Lock,
            title = AppTokens.strings.res(Res.string.home_unlock_set_goal_title),
            description = AppTokens.strings.res(Res.string.home_unlock_set_goal_description),
            trailing = {
                Button(
                    style = ButtonStyle.Secondary,
                    size = ButtonSize.Small,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.home_unlock_set_goal_cta),
                    ),
                    onClick = {},
                )
            },
        )

        BannerCard(
            modifier = Modifier.fillMaxWidth(),
            style = BannerCardStyle.Custom(
                tint = AppTokens.colors.text.tertiary,
                surface = AppTokens.colors.bannerCard.mutedBackground,
            ),
            icon = AppTokens.icons.Lock,
            title = "Performance trends",
            description = "Compare your progress after recording three workouts.",
            trailing = {
                Text(
                    modifier = Modifier
                        .background(
                            AppTokens.colors.bannerCard.mutedBackground,
                            CircleShape,
                        )
                        .padding(
                            horizontal = AppTokens.dp.contentPadding.subContent,
                            vertical = AppTokens.dp.contentPadding.text,
                        ),
                    text = "1 / 3",
                    style = AppTokens.typography.b11Semi(),
                    color = AppTokens.colors.text.tertiary,
                )
            },
        )
    }
}
