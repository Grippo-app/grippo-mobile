package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.tonnage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    training: TrainingState,
    muscleLoad: MuscleLoadSummaryState,
    onClick: () -> Unit,
) {
    MetricSectionPanel(
        modifier = modifier.scalableClick(onClick = onClick),
        style = MetricSectionPanelStyle.Large,
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
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Text(
                text = AppTokens.strings.res(Res.string.tonnage),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )

            Text(
                text = training.total.volume.short(),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )
        }

        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
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
    val duration = training.duration.short()

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
                value = duration.ifBlank { "0" },
                dimmed = duration.isBlank(),
            ),
        ),
    )
}

private fun trainingSummaryPreviewTraining(): TrainingState {
    val training = stubTraining()
    val exercises = training.exercises.mapIndexed { index, exercise ->
        exercise.copy(
            id = "summary-exercise-$index",
            iterations = persistentListOf(
                exercise.iterations.first().copy(id = "summary-set-$index"),
            ),
        )
    }.toPersistentList()

    return training.copy(
        id = "summary-training",
        duration = DurationFormatState.of(75.minutes),
        exercises = exercises,
        total = training.total.copy(
            volume = VolumeFormatState.of(8_640f),
            repetitions = RepetitionsFormatState.of(96),
        ),
    )
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = trainingSummaryPreviewTraining(),
            muscleLoad = stubMuscleLoadSummary(),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardEmptyPreview() {
    val training = trainingSummaryPreviewTraining()
    val muscleLoad = stubMuscleLoadSummary()

    PreviewContainer {
        TrainingSummaryCard(
            training = training.copy(
                duration = DurationFormatState.Empty(),
                exercises = persistentListOf(),
                total = training.total.copy(
                    volume = VolumeFormatState.Empty(),
                    repetitions = RepetitionsFormatState.Empty(),
                ),
            ),
            muscleLoad = muscleLoad.copy(
                perGroup = muscleLoad.perGroup.copy(entries = persistentListOf()),
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardSingleGroupPreview() {
    val muscleLoad = stubMuscleLoadSummary()

    PreviewContainer {
        TrainingSummaryCard(
            training = trainingSummaryPreviewTraining(),
            muscleLoad = muscleLoad.copy(
                perGroup = muscleLoad.perGroup.copy(
                    entries = persistentListOf(muscleLoad.perGroup.entries.first().copy(value = 100f)),
                ),
            ),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardLongValuesPreview() {
    val training = trainingSummaryPreviewTraining()

    PreviewContainer {
        TrainingSummaryCard(
            training = training.copy(
                duration = DurationFormatState.of(23.hours + 59.minutes),
                total = training.total.copy(
                    volume = VolumeFormatState.of(12_345_678f),
                    repetitions = RepetitionsFormatState.of(123_456),
                ),
            ),
            muscleLoad = stubMuscleLoadSummary(),
            onClick = {},
        )
    }
}
