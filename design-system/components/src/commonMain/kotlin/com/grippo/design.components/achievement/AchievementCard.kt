package com.grippo.design.components.achievement

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.achievements.AchievementState
import com.grippo.core.state.achievements.stubAchievements
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Trophy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
public fun AchievementCard(
    modifier: Modifier = Modifier,
    value: AchievementState
) {
    val shape = RoundedCornerShape(AppTokens.dp.achievementCard.radius)

    Column(
        modifier = modifier
            .width(AppTokens.dp.achievementCard.width)
            .background(AppTokens.colors.background.card, shape)
            .border(AppTokens.dp.achievementCard.borderWidth, AppTokens.colors.border.default, shape)
            .padding(
                horizontal = AppTokens.dp.achievementCard.horizontalPadding,
                vertical = AppTokens.dp.achievementCard.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
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
                    .background(
                        AppTokens.colors.background.screen,
                        shape = emblemShape
                    )
                    .padding(AppTokens.dp.achievementCard.emblem.padding),
                imageVector = AppTokens.icons.Trophy,
                tint = value.color(),
                contentDescription = null
            )

            Text(
                modifier = Modifier.weight(1f),
                text = value.text().text(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.value(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun AchievementCardPreview() {
    PreviewContainer {
        achievementCardPreviewValues().forEach { value ->
            AchievementCard(value = value)
        }
    }
}

@AppPreview
@Composable
private fun AchievementCardCarouselPreview() {
    PreviewContainer {
        AchievementsCard(value = achievementCardPreviewValues())
    }
}

private fun achievementCardPreviewValues(): ImmutableList<AchievementState> {
    return stubAchievements().map { value ->
        when (value) {
            is AchievementState.BestTonnage -> value.copy(
                exerciseExampleId = "example",
                exerciseId = "exercise",
                tonnage = VolumeFormatState.of(12_000f),
            )
            is AchievementState.BestWeight -> value.copy(
                exerciseExampleId = "example",
                exerciseId = "exercise",
                iterationId = "iteration",
                weight = VolumeFormatState.of(120f),
            )
            is AchievementState.LifetimeVolume -> value.copy(
                exerciseExampleId = "example",
                totalVolume = VolumeFormatState.of(123_456.7f),
                sessionsCount = 120,
            )
            is AchievementState.MaxRepetitions -> value.copy(
                exerciseExampleId = "example",
                exerciseId = "exercise",
                iterationId = "iteration",
                repetitions = RepetitionsFormatState.of(20),
            )
            is AchievementState.PeakIntensity -> value.copy(
                exerciseExampleId = "example",
                exerciseId = "exercise",
                intensity = IntensityFormatState.of(2.5f),
            )
        }
    }.toImmutableList()
}
