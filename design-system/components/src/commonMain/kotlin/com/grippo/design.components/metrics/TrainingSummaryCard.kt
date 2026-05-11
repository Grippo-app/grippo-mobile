package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.tonnage
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    training: TrainingState,
    muscleLoad: MuscleLoadSummaryState,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.scalableClick(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        SummaryHero(
            modifier = Modifier.fillMaxWidth(),
            training = training,
        )
        SummaryBreakdown(
            modifier = Modifier.fillMaxWidth(),
            training = training,
        )
        MuscleLoading(
            modifier = Modifier.fillMaxWidth(),
            summary = muscleLoad,
            mode = MuscleLoadingMode.PerGroup,
        )
    }
}

@Composable
private fun SummaryHero(
    modifier: Modifier = Modifier,
    training: TrainingState,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            modifier = Modifier.weight(weight = 1f, fill = false),
            text = AppTokens.strings.res(Res.string.tonnage),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            modifier = Modifier.weight(weight = 1f, fill = false),
            text = training.total.volume.short(),
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.semantic.notice,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SummaryBreakdown(
    modifier: Modifier = Modifier,
    training: TrainingState,
) {
    val durationLabel = AppTokens.strings.res(Res.string.duration)
    val setsLabel = AppTokens.strings.res(Res.string.sets)
    val repsLabel = AppTokens.strings.res(Res.string.reps)

    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { exercise -> exercise.iterations.size }
    }

    val repsCount = training.total.repetitions.value ?: 0

    MetricBreakdownRow(
        modifier = modifier,
        items = persistentListOf(
            MetricBreakdownItem(
                label = setsLabel,
                value = setsCount.toString(),
                dimmed = setsCount == 0,
            ),
            MetricBreakdownItem(
                label = repsLabel,
                value = repsCount.toString(),
                dimmed = repsCount == 0,
            ),
            MetricBreakdownItem(
                label = durationLabel,
                value = training.duration.display.ifBlank { "0" },
                dimmed = training.duration.display.isBlank(),
            ),
        ),
    )
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            onClick = {},
        )
    }
}
