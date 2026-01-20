package com.grippo.design.components.metrics.streak

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingStreakState
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.highlight_active_days
import com.grippo.design.resources.provider.training_streak_confidence_title
import com.grippo.design.resources.provider.training_streak_confidence_value
import kotlin.math.roundToInt

@Composable
public fun TrainingStreakInsightsCard(
    value: TrainingStreakState,
    modifier: Modifier = Modifier,
) {
    val confidencePercent = (value.featured.confidence * 100).roundToInt()

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_streak_confidence_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary
        )

        Text(
            text = AppTokens.strings.res(Res.string.highlight_active_days, value.totalActiveDays),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary
        )

        Text(
            text = AppTokens.strings.res(
                Res.string.training_streak_confidence_value,
                confidencePercent
            ),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakInsightsCardPreview() {
    PreviewContainer {
        TrainingStreakInsightsCard(
            value = stubTrainingStreaks().random()
        )
    }
}
