package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTrainingSummary
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.internal.MetricBreakdownItem
import com.grippo.design.components.metrics.internal.MetricBreakdownRow
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.reps
import com.grippo.design.resources.provider.available_sets_label
import com.grippo.design.resources.provider.tonnage
import com.grippo.design.resources.provider.training_completed_session_title
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
public fun TrainingSummaryCard(
    modifier: Modifier = Modifier,
    training: TrainingState,
    muscleLoad: MuscleLoadSummaryState?,
    onClick: () -> Unit,
) {
    MetricSectionPanel(
        modifier = modifier.scalableClick(onClick = onClick),
        style = MetricSectionPanelStyle.Large,
    ) {
        MetricOverviewHeader(
            modifier = Modifier.fillMaxWidth(),
            title = AppTokens.strings.res(Res.string.training_completed_session_title),
        )
        SummaryBreakdown(
            modifier = Modifier.fillMaxWidth(),
            training = training,
        )
        if (muscleLoad != null) {
            ContentDivider()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                MuscleLoading(
                    modifier = Modifier.fillMaxWidth(),
                    summary = muscleLoad,
                    mode = MuscleLoadingMode.PerGroup,
                )
            }
        }
    }
}

@Composable
private fun SummaryBreakdown(
    modifier: Modifier = Modifier,
    training: TrainingState,
) {
    val durationLabel = AppTokens.strings.res(Res.string.duration)
    val setsLabel = AppTokens.strings.res(Res.string.available_sets_label)
    val tonnageLabel = AppTokens.strings.res(Res.string.tonnage)
    val repsLabel = AppTokens.strings.res(Res.string.reps)
    val duration = training.duration.short()

    val setsCount = remember(training.exercises) {
        training.exercises.sumOf { exercise -> exercise.iterations.size }
    }

    val repsCount = training.total.repetitions.value

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        MetricBreakdownRow(
            items = persistentListOf(
                MetricBreakdownItem(
                    label = durationLabel,
                    value = duration.ifBlank { "—" },
                    dimmed = duration.isBlank(),
                ),
                MetricBreakdownItem(
                    label = setsLabel,
                    value = setsCount.toString(),
                    dimmed = setsCount == 0,
                ),
            ),
        )

        MetricBreakdownRow(
            items = persistentListOf(
                MetricBreakdownItem(
                    label = tonnageLabel,
                    value = training.total.volume.short(),
                    dimmed = training.total.volume.value == null,
                ),
                MetricBreakdownItem(
                    label = repsLabel,
                    value = repsCount?.toString() ?: "—",
                    dimmed = repsCount == null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTrainingSummary(),
            muscleLoad = stubMuscleLoadSummary(),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardEmptyPreview() {
    val training = stubTrainingSummary()
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
            training = stubTrainingSummary(),
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
private fun TrainingSummaryCardWithoutMuscleLoadPreview() {
    PreviewContainer {
        TrainingSummaryCard(
            training = stubTrainingSummary(),
            muscleLoad = null,
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardLongValuesPreview() {
    val training = stubTrainingSummary()

    PreviewContainer {
        TrainingSummaryCard(
            training = training.copy(
                duration = DurationFormatState.of(23.hours + 59.minutes),
                total = training.total.copy(
                    volume = VolumeFormatState.ofTotal(12_345_678f),
                    repetitions = RepetitionsFormatState.ofTotal(123_456),
                ),
            ),
            muscleLoad = stubMuscleLoadSummary(),
            onClick = {},
        )
    }
}

@AppPreview
@Composable
private fun TrainingSummaryCardRecordedZeroPreview() {
    val training = stubTrainingSummary()
    PreviewContainer {
        TrainingSummaryCard(
            training = training.copy(
                duration = DurationFormatState.ofTotal(kotlin.time.Duration.ZERO),
                exercises = persistentListOf(),
                total = training.total.copy(
                    volume = VolumeFormatState.ofTotal(0f),
                    repetitions = RepetitionsFormatState.ofTotal(0),
                ),
            ),
            muscleLoad = null,
            onClick = {},
        )
    }
}
