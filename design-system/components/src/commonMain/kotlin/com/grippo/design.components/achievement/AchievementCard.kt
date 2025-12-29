package com.grippo.design.components.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.achevements.AchievementState
import com.grippo.core.state.achevements.stubAchievement
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy

@Composable
public fun AchievementCard(
    modifier: Modifier = Modifier,
    value: AchievementState
) {
    val shape = RoundedCornerShape(AppTokens.dp.achievementCard.radius)

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.achievementCard.horizontalPadding,
                vertical = AppTokens.dp.achievementCard.verticalPadding
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val emblemShape = RoundedCornerShape(AppTokens.dp.achievementCard.emblem.radius)

            Icon(
                modifier = Modifier
                    .size(AppTokens.dp.achievementCard.emblem.size)
                    .clip(emblemShape)
                    .background(
                        value.color1().copy(alpha = 0.08f),
                        shape = emblemShape
                    )
                    .padding(AppTokens.dp.achievementCard.emblem.padding)
                    .size(AppTokens.dp.achievementCard.icon),
                imageVector = AppTokens.icons.Trophy,
                tint = value.color1(),
                contentDescription = null
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            val chipShape = RoundedCornerShape(AppTokens.dp.achievementCard.chip.radius)

            Text(
                modifier = Modifier
                    .clip(chipShape)
                    .background(
                        AppTokens.colors.text.primary.copy(alpha = 0.08f),
                        shape = chipShape
                    )
                    .padding(
                        horizontal = AppTokens.dp.achievementCard.chip.horizontalPadding,
                        vertical = AppTokens.dp.achievementCard.chip.verticalPadding
                    ),
                text = value.text().text(),
                style = AppTokens.typography.b11Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.value(),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
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
