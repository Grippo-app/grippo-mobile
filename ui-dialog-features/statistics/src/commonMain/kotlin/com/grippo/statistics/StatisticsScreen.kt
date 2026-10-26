package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummaryUnknown
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.core.state.metrics.volume.stubVolumeSeries
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
import com.grippo.design.components.datetime.DialogRangeHeader
import com.grippo.design.components.frames.OverlayContainer
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.volume.TrainingTotalSection
import com.grippo.design.components.metrics.volume.VolumeMetricChart
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles
import com.grippo.statistics.components.StatisticsSkeleton
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    when (val mode = state.mode) {
        is StatisticsMode.Exercises -> {
            Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))
        }

        is StatisticsMode.Trainings -> {
            DialogRangeHeader(text = mode.range.shortStartEnd())
        }
    }

    OverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        overlay = AppTokens.colors.background.dialog,
        content = { resolvedPadding ->
            if (loaders.contains(StatisticsLoader.Charts)) {
                StatisticsSkeleton(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = resolvedPadding,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = resolvedPadding,
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
                ) {
                    state.total?.let { metrics ->
                        item(key = "total_chips") {
                            TrainingTotalSection(
                                modifier = Modifier.fillMaxWidth(),
                                value = metrics,
                            )
                        }
                    }

                    state.exerciseVolume
                        ?.takeIf { it.entries.isNotEmpty() }
                        ?.let { data ->
                            item(key = "exercise_volume") {
                                VolumeMetricChart(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = data,
                                    xAxisLabels = when (state.mode) {
                                        StatisticsMode.Exercises -> BarChartXAxisLabels.WithoutLabels
                                        is StatisticsMode.Trainings -> BarChartXAxisLabels.WithLabels
                                    }
                                )
                            }
                        }

                    state.muscleLoad
                        ?.let { summary ->
                            item(key = "muscle_load") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                                ) {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = AppTokens.strings.res(Res.string.muscles),
                                        style = AppTokens.typography.h4(),
                                        color = AppTokens.colors.text.primary,
                                    )
                                    MuscleLoading(
                                        modifier = Modifier.fillMaxWidth(),
                                        summary = summary,
                                        mode = MuscleLoadingMode.PerGroup,
                                    )
                                }
                            }
                        }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.navigationBarsPadding())
        },
    )
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 5, 0, 0),
                        to = LocalDateTime(2026, 10, 11, 23, 59),
                    ),
                ),
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
                muscleLoad = stubMuscleLoadSummary(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun StatisticsTrainingsLoadingPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 5, 0, 0),
                        to = LocalDateTime(2026, 10, 11, 23, 59),
                    ),
                ),
            ),
            loaders = persistentSetOf(StatisticsLoader.Charts),
            contract = StatisticsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StatisticsExercisesLoadingPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(mode = StatisticsMode.Exercises),
            loaders = persistentSetOf(StatisticsLoader.Charts),
            contract = StatisticsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StatisticsExercisesWithoutDataPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(mode = StatisticsMode.Exercises),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StatisticsTrainingsWithoutMuscleLoadPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 5, 0, 0),
                        to = LocalDateTime(2026, 10, 11, 23, 59),
                    ),
                ),
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StatisticsUnknownMuscleDataPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Exercises,
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
                muscleLoad = stubMuscleLoadSummaryUnknown(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun StatisticsTrainingsUnknownMuscleDataPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(
                    range = DateRangeFormatState.of(
                        from = LocalDateTime(2026, 10, 5, 0, 0),
                        to = LocalDateTime(2026, 10, 11, 23, 59),
                    ),
                ),
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
                muscleLoad = stubMuscleLoadSummaryUnknown(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty,
        )
    }
}
