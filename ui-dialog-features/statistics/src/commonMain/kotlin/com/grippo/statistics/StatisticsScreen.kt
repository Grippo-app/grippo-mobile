package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.MuscleLoadSummaryState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.core.state.metrics.volume.stubVolumeSeries
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoading
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingMode
import com.grippo.design.components.metrics.volume.TrainingTotalSection
import com.grippo.design.components.metrics.volume.VolumeMetricChart
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.available_training_details_notice
import com.grippo.design.resources.provider.muscles
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    when (val mode = state.mode) {
        is StatisticsMode.Exercises -> {

        }

        is StatisticsMode.Trainings -> {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
                text = mode.range.short(),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))
        }
    }

    if (state.mode is StatisticsMode.Trainings) {
        ContentDivider()

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))
    }

    if (loaders.contains(StatisticsLoader.Charts)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = AppTokens.dp.dialog.horizontalPadding,
                end = AppTokens.dp.dialog.horizontalPadding,
                bottom = AppTokens.dp.dialog.bottom,
            ),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
        ) {
            if (state.total != null ||
                state.exerciseVolume?.entries?.isNotEmpty() == true ||
                state.muscleLoad != null
            ) {
                item(key = "available_details_notice") {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = AppTokens.strings.res(Res.string.available_training_details_notice),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }

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
                    item(key = "muscles_spliter") {
                        ContentSpliter(
                            text = AppTokens.strings.res(Res.string.muscles)
                        )
                    }

                    item(key = "muscle_load") {
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

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(range = DateRangeFormatState.of(DateRangeKind.Weekly)),
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
                muscleLoad = unknownMuscleLoadPreview(),
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
                muscleLoad = unknownMuscleLoadPreview(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty,
        )
    }
}

private fun unknownMuscleLoadPreview(): MuscleLoadSummaryState {
    val summary = stubMuscleLoadSummary()
    val emptyBreakdown = summary.perGroup.copy(entries = persistentListOf())

    return summary.copy(
        meta = summary.meta.copy(
            dominantGroup = null,
            unassignedSets = summary.meta.totalSets,
        ),
        perGroup = emptyBreakdown,
        perMuscle = emptyBreakdown,
        volumePerGroup = emptyBreakdown,
        volumePerMuscle = emptyBreakdown,
        dominance = summary.dominance.copy(top1SharePercent = 0f, top2SharePercent = 0f),
        groupDominance = summary.groupDominance.copy(top1SharePercent = 0f, top2SharePercent = 0f),
    )
}
