package com.grippo.design.components.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.achevements.AchievementState
import com.grippo.core.state.achevements.stubAchievement
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy

@Composable
public fun AchievementCard(
    modifier: Modifier = Modifier,
    value: AchievementState
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(AppTokens.dp.achievementCard.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.achievementCard.radius)
            )
    ) {
        Icon(
            modifier = Modifier
                .spot(color = value.color1())
                .align(Alignment.CenterEnd)
                .offset(x = (AppTokens.dp.achievementCard.icon / 2))
                .size(AppTokens.dp.achievementCard.icon)
                .scale(1.6f)
                .alpha(0.2f),
            imageVector = AppTokens.icons.Trophy,
            tint = value.color1(),
            contentDescription = null,
        )

        Column(
            modifier = Modifier.padding(
                horizontal = AppTokens.dp.achievementCard.horizontalPadding,
                vertical = AppTokens.dp.achievementCard.verticalPadding
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.text().text(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.value(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = AppTokens.typography.h6(),
                color = AppTokens.colors.text.primary
            )
        }
    }
}

@AppPreview
@Composable
private fun AchievementCardPreview() {
    PreviewContainer {
        AchievementCard(
            value = stubAchievement()
        )
    }
}