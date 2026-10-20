package com.grippo.design.components.metrics.engagement.streak

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.engagement.TrainingStreakState
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_streak_insights_title

@Composable
public fun TrainingStreakInsightsCard(
    modifier: Modifier = Modifier,
    value: TrainingStreakState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_streak_insights_title),
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            text = value.featured.calculationDescription(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakInsightsCardDailyPreview() {
    PreviewContainer {
        TrainingStreakInsightsCard(
            value = stubTrainingStreaks()[0],
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakInsightsCardWeeklyPreview() {
    PreviewContainer {
        TrainingStreakInsightsCard(value = stubTrainingStreaks()[1])
    }
}

@AppPreview
@Composable
private fun TrainingStreakInsightsCardCyclePreview() {
    PreviewContainer {
        TrainingStreakInsightsCard(value = stubTrainingStreaks()[2])
    }
}
