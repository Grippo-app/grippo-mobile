package com.grippo.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.metrics.MuscleLoadBreakdown
import com.grippo.core.state.muscles.metrics.MuscleLoadEntry
import com.grippo.core.state.muscles.metrics.MuscleLoadSummary
import com.grippo.core.state.trainings.stubMetrics
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.chart.DistributionPieChart
import com.grippo.design.components.chart.MetricBarChart
import com.grippo.design.components.chart.MuscleHeatmapChart
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.IntensityChip
import com.grippo.design.components.chip.IntensityChipStyle
import com.grippo.design.components.chip.RepetitionsChip
import com.grippo.design.components.chip.RepetitionsChipStyle
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.muscle.MuscleLoading
import com.grippo.design.components.spliter.ContentSpliter
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscles
import com.grippo.design.resources.provider.statistics
import com.grippo.design.resources.provider.trends
import com.grippo.design.resources.provider.value_statistics
import com.grippo.toolkit.calculation.models.DistributionBreakdown
import com.grippo.toolkit.calculation.models.DistributionSlice
import com.grippo.toolkit.calculation.models.MetricPoint
import com.grippo.toolkit.calculation.models.MetricSeries
import com.grippo.toolkit.calculation.models.MuscleLoadMatrix
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
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
                is StatisticsMode.Exercises -> AppTokens.strings.res(
                    Res.string.statistics
                )

                is StatisticsMode.Trainings -> mode.range.label()?.let {
                    AppTokens.strings.res(Res.string.value_statistics, it)
                } ?: AppTokens.strings.res(
                    Res.string.statistics
                )
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
                item(key = "summary_chips") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {
                        state
                            .totalMetrics
                            ?.volume
                            ?.takeIf { it.value != null }
                            ?.let { data ->
                                VolumeChip(
                                    modifier = Modifier.weight(1f),
                                    value = data,
                                    style = VolumeChipStyle.SHORT,
                                    size = ChipSize.Medium
                                )
                            }

                        state
                            .totalMetrics
                            ?.repetitions
                            ?.takeIf { it.value != null }
                            ?.let { data ->
                                RepetitionsChip(
                                    modifier = Modifier.weight(1f),
                                    value = data,
                                    style = RepetitionsChipStyle.SHORT,
                                    size = ChipSize.Medium
                                )
                            }

                        state
                            .totalMetrics
                            ?.intensity
                            ?.takeIf { it.value != null }
                            ?.let { data ->
                                IntensityChip(
                                    modifier = Modifier.weight(1f),
                                    value = data,
                                    style = IntensityChipStyle.SHORT,
                                    size = ChipSize.Medium
                                )
                            }
                    }
                }

                state.exerciseVolume
                    ?.takeIf { it.points.isNotEmpty() }
                    ?.let { data ->
                        item(key = "exercise_volume") {
                            MetricBarChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.7f),
                                value = data,
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
                            ?.takeIf { it.slices.isNotEmpty() }
                            ?.let { data ->
                                DistributionPieChart(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    value = data
                                )
                            }

                        state.weightTypeDistribution
                            ?.takeIf { it.slices.isNotEmpty() }
                            ?.let { data ->
                                DistributionPieChart(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    value = data
                                )
                            }

                        state.forceTypeDistribution
                            ?.takeIf { it.slices.isNotEmpty() }
                            ?.let { data ->
                                DistributionPieChart(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f),
                                    value = data
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
                    ?.perGroup
                    ?.entries
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { entries ->
                        item(key = "muscle_load") {
                            MuscleLoading(
                                entries = entries
                            )
                        }
                    }

                state.temporalHeatmap
                    ?.takeIf { it.values01.isNotEmpty() }
                    ?.let { data ->
                        item(key = "temporal_heatmap") {
                            MuscleHeatmapChart(
                                modifier = Modifier.fillMaxWidth(),
                                value = data,
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
        val mode = StatisticsMode.Trainings(
            trainings = persistentListOf(stubTraining(), stubTraining(), stubTraining()),
            range = DateTimeUtils.thisWeek()
        )

        val exerciseVolume = MetricSeries(
            points = listOf(
                MetricPoint(label = "Mon", value = 12f, color = Color(0xFF7B61FF)),
                MetricPoint(label = "Tue", value = 18f, color = Color(0xFF5AD4A3)),
                MetricPoint(label = "Wed", value = 9f, color = Color(0xFFF6B93C)),
                MetricPoint(label = "Thu", value = 22f, color = Color(0xFF4C83FF)),
                MetricPoint(label = "Fri", value = 15f, color = Color(0xFFE76F51)),
                MetricPoint(label = "Sat", value = 11f, color = Color(0xFF34AADC)),
                MetricPoint(label = "Sun", value = 6f, color = Color(0xFF8E5CF6)),
            )
        )

        val categoryDistribution = DistributionBreakdown(
            slices = listOf(
                DistributionSlice(
                    id = "category_push",
                    label = "Push",
                    value = 40f,
                    color = Color(0xFF4C83FF)
                ),
                DistributionSlice(
                    id = "category_pull",
                    label = "Pull",
                    value = 35f,
                    color = Color(0xFF5AD4A3)
                ),
                DistributionSlice(
                    id = "category_legs",
                    label = "Legs",
                    value = 25f,
                    color = Color(0xFFF6B93C)
                )
            )
        )

        val weightDistribution = DistributionBreakdown(
            slices = listOf(
                DistributionSlice(
                    id = "weight_free",
                    label = "Free weights",
                    value = 55f,
                    color = Color(0xFFE76F51)
                ),
                DistributionSlice(
                    id = "weight_machines",
                    label = "Machines",
                    value = 30f,
                    color = Color(0xFF7B61FF)
                ),
                DistributionSlice(
                    id = "weight_body",
                    label = "Bodyweight",
                    value = 15f,
                    color = Color(0xFF34AADC)
                )
            )
        )

        val forceDistribution = DistributionBreakdown(
            slices = listOf(
                DistributionSlice(
                    id = "force_push",
                    label = "Push",
                    value = 50f,
                    color = Color(0xFF5AD4A3)
                ),
                DistributionSlice(
                    id = "force_pull",
                    label = "Pull",
                    value = 30f,
                    color = Color(0xFF4C83FF)
                ),
                DistributionSlice(
                    id = "force_iso",
                    label = "Isometric",
                    value = 20f,
                    color = Color(0xFFF6B93C)
                )
            )
        )

        val muscleLoad = MuscleLoadSummary(
            perGroup = MuscleLoadBreakdown(
                entries = listOf(
                    MuscleLoadEntry(
                        label = "Chest",
                        value = 0.78f,
                        color = Color(0xFF4C83FF),
                        muscles = persistentListOf(
                            MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                            MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                        )
                    ),
                    MuscleLoadEntry(
                        label = "Back",
                        value = 0.64f,
                        color = Color(0xFFE76F51),
                        muscles = persistentListOf(
                            MuscleEnumState.LATISSIMUS_DORSI,
                            MuscleEnumState.TRAPEZIUS
                        )
                    ),
                    MuscleLoadEntry(
                        label = "Legs",
                        value = 0.52f,
                        color = Color(0xFF5AD4A3),
                        muscles = persistentListOf(
                            MuscleEnumState.QUADRICEPS,
                            MuscleEnumState.HAMSTRINGS
                        )
                    )
                )
            ),
            images = null
        )

        val heatmap = MuscleLoadMatrix(
            rows = 3,
            cols = 4,
            values01 = listOf(
                0.15f, 0.65f, 0.35f, 0.80f,
                0.30f, 0.45f, 0.55f, 0.25f,
                0.60f, 0.20f, 0.70f, 0.10f
            ),
            rowLabels = listOf("Chest", "Back", "Legs"),
            colLabels = listOf("W1", "W2", "W3", "W4")
        )

        StatisticsScreen(
            state = StatisticsState(
                mode = mode,
                totalMetrics = stubMetrics(),
                exerciseVolume = exerciseVolume,
                categoryDistribution = categoryDistribution,
                weightTypeDistribution = weightDistribution,
                forceTypeDistribution = forceDistribution,
                muscleLoad = muscleLoad,
                temporalHeatmap = heatmap
            ),
            loaders = persistentSetOf(),
            contract = StatisticsContract.Empty
        )
    }
}
