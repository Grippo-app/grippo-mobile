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
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.performance.stubEstimatedOneRepMax
import com.grippo.core.state.metrics.volume.VolumeSeriesEntryState
import com.grippo.core.state.metrics.volume.VolumeSeriesState
import com.grippo.core.state.metrics.volume.stubVolumeSeries
import com.grippo.core.state.trainings.stubExercises
import com.grippo.core.state.trainings.stubPendingIteration
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
import com.grippo.design.resources.provider.history
import com.grippo.exercise.example.exerciseexample.ExerciseExampleDetailsLoader
import com.grippo.exercise.example.exerciseexample.ExerciseExampleDetailsState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun ExerciseExampleDetailsAchievementsContent(
    modifier: Modifier,
    state: ExerciseExampleDetailsState,
    loaders: ImmutableSet<ExerciseExampleDetailsLoader>,
) {
    if (ExerciseExampleDetailsLoader.RecentExercises in loaders || ExerciseExampleDetailsLoader.Achievements in loaders) {
        ExerciseExampleDetailsAchievementsShimmer(modifier = modifier)
        return
    }

    Column(modifier = modifier) {
        if (state.achievements.isNotEmpty()) {
            AchievementsCard(
                modifier = Modifier.fillMaxWidth(),
                value = state.achievements,
                contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding)
            )
        }

        if (state.achievements.isNotEmpty()) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
        }

        EstimatedOneRepMaxCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
            state = state.estimatedOneRepMax,
        )

        if (state.recent.isNotEmpty()) {
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
                val chronological = state.recent.asReversed()
                val entries = series.entries.mapIndexed { index, entry ->
                    entry.copy(label = chronological[index].createdAt.short())
                }.toPersistentList()
                val data = series.copy(entries = entries)

                VolumeMetricChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                    value = data,
                    xAxisLabels = BarChartXAxisLabels.WithoutLabels
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
            }

            state.recent.forEachIndexed { index, item ->
                key(item.id) {
                    ExerciseCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                        value = item,
                        style = ExerciseCardStyle.Small,
                    )
                }

                if (index < state.recent.lastIndex) {
                    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentPreview() {
    PreviewContainer {
        val recent = stubExercises()
        val volume = stubVolumeSeries()
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = ExerciseExampleDetailsState(
                recent = recent,
                estimatedOneRepMax = stubEstimatedOneRepMax(),
                achievements = stubAchievements(),
                exerciseVolume = volume.copy(entries = volume.entries.take(recent.size).toPersistentList()),
            ),
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsContentEmptyPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = ExerciseExampleDetailsState(),
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun EstimatedMaxOnlyPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = ExerciseExampleDetailsState(estimatedOneRepMax = stubEstimatedOneRepMax()),
            loaders = persistentSetOf(),
        )
    }
}

@AppPreview
@Composable
private fun HistoryOnlyPreview() {
    val exercise = stubExercises().first().copy(
        id = "history-only",
        iterations = persistentListOf(
            stubPendingIteration().copy(
                id = "history-set",
                externalWeight = VolumeFormatState.of(80f),
                repetitions = RepetitionsFormatState.of(10),
            ),
        ),
    )
    PreviewContainer {
        ExerciseExampleDetailsAchievementsContent(
            modifier = Modifier.fillMaxWidth(),
            state = ExerciseExampleDetailsState(
                recent = persistentListOf(exercise),
                exerciseVolume = VolumeSeriesState(
                    entries = persistentListOf(VolumeSeriesEntryState(label = exercise.name, value = 800f)),
                ),
            ),
            loaders = persistentSetOf(),
        )
    }
}
