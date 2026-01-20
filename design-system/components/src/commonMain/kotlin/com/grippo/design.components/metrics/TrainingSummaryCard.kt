package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.overview
import com.grippo.design.resources.provider.total_value
import com.grippo.design.resources.provider.value_sets
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    training: TrainingState,
    onClick: () -> Unit
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
        decoration = {
            Box(
                modifier = Modifier
                    .spot(color = AppTokens.colors.brand.color3)
                    .align(Alignment.CenterEnd)
                    .size(AppTokens.dp.metrics.trainingSummary.spot)
                    .scale(2f),
            )
        }
    ) {
        val totalTxt = AppTokens.strings.res(Res.string.total_value, training.total.volume.short())

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = totalTxt,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

            Button(
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.overview),
                    endIcon = ButtonIcon.Icon(AppTokens.icons.ArrowRight)
                ),
                size = ButtonSize.Small,
                style = ButtonStyle.Secondary,
                onClick = onClick
            )
        }

        val durationText = remember(training.duration) {
            DateTimeUtils.format(training.duration)
        }
        val setsCount = remember(training.exercises) {
            training.exercises.sumOf { e -> e.iterations.sumOf { i -> i.repetitions.value ?: 0 } }
        }
        val setsTxt = AppTokens.strings.res(Res.string.value_sets, setsCount)

        Text(
            text = "$durationText · $setsTxt",
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.tertiary
        )

        MuscleLoading(
            modifier = Modifier.fillMaxWidth(),
            summary = summary,
            mode = MuscleLoadingMode.PerGroup,
            style = MuscleLoadingStyle.Collapsed
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            summary = stubMuscleLoadSummary(),
            training = stubTraining(),
            onClick = {}
        )
    }
}
