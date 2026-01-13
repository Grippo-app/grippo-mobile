package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.grippo.core.state.metrics.stubCategoryDistribution
import com.grippo.core.state.metrics.stubForceDistribution
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubTotal
import com.grippo.core.state.metrics.stubVolumeSeries
import com.grippo.core.state.metrics.stubWeightDistribution
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.ExerciseDistributionChart
import com.grippo.design.components.metrics.ForceTypeDistributionChart
import com.grippo.design.components.metrics.MuscleLoading
import com.grippo.design.components.metrics.MuscleLoadingMode
import com.grippo.design.components.metrics.TrainingTotalSection
import com.grippo.design.components.metrics.VolumeMetricChart
import com.grippo.design.components.metrics.WeightTypeDistributionChart
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.trends
import com.grippo.design.resources.provider.value_statistics
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun StatisticsScreen(
    state: StatisticsState,
    loaders: ImmutableSet<StatisticsLoader>,
    contract: StatisticsContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (val mode = state.mode) {
                is StatisticsMode.Exercises -> AppTokens.strings.res(Res.string.statistics)
                is StatisticsMode.Trainings -> mode.range.label()?.let {
                    AppTokens.strings.res(Res.string.value_statistics, it)
                } ?: AppTokens.strings.res(Res.string.statistics)
            },
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        when (val mode = state.mode) {
            is StatisticsMode.Exercises -> {

            }

            is StatisticsMode.Trainings -> {
                Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = mode.range.formatted(),
                    style = AppTokens.typography.b14Semi(),
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
                contentPadding = PaddingValues(
                    horizontal = AppTokens.dp.dialog.horizontalPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block)
            ) {
                state.total?.let { metrics ->
                    item(key = "total_chips") {
                        TrainingTotalSection(
                            modifier = Modifier.fillMaxWidth(),
                            state = metrics,
                        )
                    }
                }

                state.exerciseVolume
                    ?.takeIf { it.entries.isNotEmpty() }
                    ?.let { data ->
                        item(key = "exercise_volume") {
                            VolumeMetricChart(
                                modifier = Modifier.fillMaxWidth(),
                                state = data,
                            )
                        }
                    }

                item(key = "trends_spliter") {
                    ContentSpliter(
                        text = AppTokens.strings.res(Res.string.trends)
                    )
                }

                item(key = "distribution") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {
                        state.categoryDistribution
                            ?.takeIf { it.entries.isNotEmpty() }
                            ?.let { distribution ->
                                ExerciseDistributionChart(
                                    modifier = Modifier.weight(1f),
                                    state = distribution
                                )
                            }

                        state.weightTypeDistribution
                            ?.takeIf { it.entries.isNotEmpty() }
                            ?.let { distribution ->
                                WeightTypeDistributionChart(
                                    modifier = Modifier.weight(1f),
                                    state = distribution
                                )
                            }

                        state.forceTypeDistribution
                            ?.takeIf { it.entries.isNotEmpty() }
                            ?.let { distribution ->
                                ForceTypeDistributionChart(
                                    modifier = Modifier.weight(1f),
                                    state = distribution
                                )
                            }
                    }
                }

                item(key = "muscles_spliter") {
                    ContentSpliter(
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
                                mode = MuscleLoadingMode.PerGroup
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
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        StatisticsScreen(
            state = StatisticsState(
                mode = StatisticsMode.Trainings(range = DateTimeUtils.thisWeek()),
                total = stubTotal(),
                exerciseVolume = stubVolumeSeries(),
                categoryDistribution = stubCategoryDistribution(),
                weightTypeDistribution = stubWeightDistribution(),
                forceTypeDistribution = stubForceDistribution(),
                muscleLoad = stubMuscleLoadSummary(),
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty
        )
    }
}
