package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.grippo.core.state.achievements.stubAchievements
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.PercentageFormatState
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.core.state.metrics.volume.VolumeSeriesEntryState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExerciseHistory
import com.grippo.design.components.achievement.AchievementsCard
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
import com.grippo.design.components.metrics.performance.EstimatedOneRepMaxCard
import com.grippo.design.components.metrics.volume.VolumeMetricChart
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_volume_delta
import com.grippo.design.resources.provider.history
import com.grippo.exercise.example.exerciseexample.ExerciseExampleDetailsState
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseExampleDetailsAchievementsContent(
    modifier: Modifier,
    state: ExerciseExampleDetailsState,
) {
    val achievements = state.achievements
    val recent = state.recent.orEmpty()

    Column(modifier = modifier) {
        if (!achievements.isNullOrEmpty()) {
            AchievementsCard(
                modifier = Modifier.fillMaxWidth(),
                value = achievements,
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
        }

        EstimatedOneRepMaxCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            state = state.estimatedOneRepMax,
        )

        if (recent.isNotEmpty()) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                text = AppTokens.strings.res(Res.string.history),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            state.exerciseVolume?.let { series ->
                val chronological = recent.asReversed()
                val entries = series.entries.mapIndexed { index, entry ->
                    entry.copy(label = rememberHistoryDateLabel(chronological[index].createdAt))
                }.toPersistentList()
                val data = series.copy(entries = entries)

                VolumeMetricChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    value = data,
                    xAxisLabels = BarChartXAxisLabels.WithLabels
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
            }

            recent.forEachIndexed { index, item ->
                val previous = recent.getOrNull(index + 1)
                val delta = previous?.let(item::volumeDeltaPercent)
                val trailing: (@Composable () -> Unit)? = if (delta != null) {
                    { ExerciseHistoryVolumeDelta(delta = delta) }
                } else {
                    null
                }

                key(item.id) {
                    ExerciseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        value = item,
                        style = ExerciseCardStyle.Small(trailing = trailing),
                    )
                }

                if (index < recent.lastIndex) {
                    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
                }
            }
        }
    }
}

@Composable
private fun ExerciseHistoryVolumeDelta(
    modifier: Modifier = Modifier,
    delta: Double,
) {
    val percent = PercentageFormatState.shortDelta(delta)

    Text(
        modifier = modifier,
        text = AppTokens.strings.res(Res.string.exercise_history_volume_delta, percent),
        style = AppTokens.typography.b12Semi(),
        color = AppTokens.colors.text.secondary,
    )
}

@Composable
private fun rememberHistoryDateLabel(value: DateTimeFormatState): String {
    val date = value.value ?: return value.display
    return DateCompose.rememberFormat(value = date, format = DateFormat.DateOnly.DateDdMmm)
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = historyPreviewState(stubExerciseHistory().take(2).toPersistentList()).copy(
                estimatedOneRepMax = stubEstimatedOneRepMax(),
                achievements = stubAchievements(),
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentHistoryPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = historyPreviewState(stubExerciseHistory()),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentSingleEntryPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = historyPreviewState(stubExerciseHistory().take(1).toPersistentList()),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentZeroTonnagePreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = historyPreviewState(stubExerciseHistory().drop(2).toPersistentList()),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentUnavailablePreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = ExerciseExampleDetailsState(),
        )
    }
}

private fun historyPreviewState(entries: PersistentList<ExerciseState>): ExerciseExampleDetailsState {
    val series = entries.asReversed().map { entry ->
        VolumeSeriesEntryState(label = entry.createdAt.display, value = entry.total.volume.value ?: 0f)
    }.toPersistentList()

    return ExerciseExampleDetailsState(
        recent = entries,
        exerciseVolume = VolumeSeriesState(entries = series),
    )
}
