package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.volume.stubTotal
import com.grippo.core.state.metrics.volume.stubVolumeSeries
import com.grippo.design.components.chart.internal.BarChartXAxisLabels
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
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.value_statistics
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = when (val mode = state.mode) {
            is StatisticsMode.Exercises -> AppTokens.strings.res(Res.string.statistics)
            is StatisticsMode.Trainings -> mode.range.label()?.text()?.let {
                AppTokens.strings.res(Res.string.value_statistics, it)
            } ?: AppTokens.strings.res(Res.string.statistics)
        },
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    when (val mode = state.mode) {
        is StatisticsMode.Exercises -> {

        }

        is StatisticsMode.Trainings -> {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = mode.range.display,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    if (loaders.contains(StatisticsLoader.Charts)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
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

            item(key = "muscles_spliter") {
                ContentSpliter(
                    modifier = Modifier,
                    text = AppTokens.strings.res(Res.string.muscles)
                )
            }

            state.muscleLoad
                ?.takeIf { it.perGroup.entries.isNotEmpty() }
                ?.let { summary ->
                    item(key = "muscle_load") {
                        MuscleLoading(
                            modifier = Modifier.fillMaxWidth(),
                            summary = summary,
                            mode = MuscleLoadingMode.PerGroup,
                        )
                    }
                }

            item("bottom_space") {
                Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

                Spacer(modifier = Modifier.navigationBarsPadding())
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
