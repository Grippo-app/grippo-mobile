package com.grippo.design.components.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.achievementCard.radius)
            ).width(
                intrinsicSize = IntrinsicSize.Max
            ).padding(
                horizontal = AppTokens.dp.achievementCard.horizontalPadding,
                vertical = AppTokens.dp.achievementCard.verticalPadding
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.achievementCard.icon),
            imageVector = AppTokens.icons.Trophy,
            tint = AppTokens.colors.semantic.warning,
            contentDescription = null
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

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
            text = "5.000kg",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary
        )
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