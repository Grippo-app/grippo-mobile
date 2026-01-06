package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.MuscleLoadSummaryState
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.muscle.MuscleLoading
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.total_value
import com.grippo.design.resources.provider.value_sets
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    summary: MuscleLoadSummaryState,
    training: TrainingState
) {
    MetricSectionPanel(modifier = modifier) {
        val totalTxt = AppTokens.strings.res(Res.string.total_value, training.total.volume.short())

        Text(
            text = totalTxt,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary
        )

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
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            summary = stubMuscleLoadSummary(),
            training = stubTraining()
        )
    }
}
