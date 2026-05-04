package com.grippo.design.components.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Sparkle
import com.grippo.design.resources.provider.onboarding_exercise_pack_subtitle
import com.grippo.design.resources.provider.onboarding_exercise_pack_title

@Composable
public fun OnboardingExercisePackCard(
    modifier: Modifier = Modifier,
) {
    val accent = AppTokens.colors.brand.color2

    Row(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.onboarding.card.radius)
            )
            .padding(
                horizontal = AppTokens.dp.onboarding.card.horizontalPadding,
                vertical = AppTokens.dp.onboarding.card.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.onboarding.card.space)
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.onboarding.iconBadge.size)
                .background(
                    color = accent.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.onboarding.iconBadge.icon),
                imageVector = AppTokens.icons.Sparkle,
                contentDescription = null,
                tint = accent,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppTokens.strings.res(Res.string.onboarding_exercise_pack_title),
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            Text(
                text = AppTokens.strings.res(Res.string.onboarding_exercise_pack_subtitle),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.tertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@AppPreview
@Composable
private fun OnboardingExercisePackCardPreview() {
    PreviewContainer {
        OnboardingExercisePackCard()
    }
}
