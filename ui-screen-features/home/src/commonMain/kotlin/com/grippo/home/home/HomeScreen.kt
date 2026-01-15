package com.grippo.home.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.stubDigest
import com.grippo.core.state.metrics.stubExerciseSpotlight
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.metrics.DigestCard
import com.grippo.design.components.metrics.ExerciseSpotlightCard
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.metrics.MuscleLoadingCard
import com.grippo.design.components.metrics.PerformanceTrendCard
import com.grippo.design.components.metrics.TrainingStreakCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dashboard
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.start_workout
import com.grippo.home.home.components.EmptyHomeContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.hours

@Composable
internal fun HomeScreen(
    state: HomeState,
    loaders: ImmutableSet<HomeLoader>,
    contract: HomeContract
) = BaseComposeScreen(
    ScreenBackground.Color(
        value = AppTokens.colors.background.screen,
    )
) {
    val isEmptyState = state.lastTraining == null &&
            state.streak == null &&
            state.digest == null

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent,
        title = AppTokens.strings.res(Res.string.dashboard),
        trailing = {
            Button(
                modifier = Modifier.padding(end = AppTokens.dp.contentPadding.subContent),
                content = ButtonContent.Icon(icon = ButtonIcon.Icon(AppTokens.icons.User)),
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = contract::onOpenProfile
            )
        },
    )

    if (isEmptyState && loaders.contains(HomeLoader.Trainings)) {
        Loader(modifier = Modifier.fillMaxWidth().weight(1f))
        return@BaseComposeScreen
    }

    if (isEmptyState && loaders.contains(HomeLoader.Trainings).not()) {
        EmptyHomeContent(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onStartTraining = contract::onStartTraining
        )
        return@BaseComposeScreen
    }

    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.contentPadding.content
    )

    val densityMetric = remember(state.performance) {
        state.performance.firstOrNull { it.type == PerformanceMetricTypeState.Density }
    }

    val volumeMetric = remember(state.performance) {
        state.performance.firstOrNull { it.type == PerformanceMetricTypeState.Volume }
    }

    val repetitionsMetric = remember(state.performance) {
        state.performance.firstOrNull { it.type == PerformanceMetricTypeState.Repetitions }
    }

    val intensityMetric = remember(state.performance) {
        state.performance.firstOrNull { it.type == PerformanceMetricTypeState.Intensity }
    }

    BottomOverlayContainer(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = basePadding,
        overlay = AppTokens.colors.background.screen,
        content = { containerModifier, resolvedPadding ->
            LazyVerticalGrid(
                modifier = containerModifier
                    .fillMaxWidth()
                    .weight(1f),
                columns = GridCells.Fixed(2),
                contentPadding = resolvedPadding,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
            ) {
                if (state.lastTraining != null) {
                    item(key = "last_training", span = { GridItemSpan(2) }) {
                        LastTrainingCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.lastTraining,
                            onClick = contract::onOpenTrainings
                        )
                    }
                }

                item(key = "highlights_header", span = { GridItemSpan(2) }) {
                    HighlightsHeader(
                        modifier = Modifier.fillMaxWidth(),
                        range = state.range,
                        onPeriodChange = contract::onOpenPeriodPicker
                    )
                }

                if (state.spotlight != null) {
                    item(key = "exercise_spotlight", span = { GridItemSpan(2) }) {
                        val onExampleClickProvider =
                            remember(state.spotlight.exercise.value.id) {
                                { contract.onOpenExample(state.spotlight.exercise.value.id) }
                            }

                        ExerciseSpotlightCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scalableClick(onClick = onExampleClickProvider),
                            value = state.spotlight,
                        )
                    }
                }

                item(key = "muscle_loading_and_training_streak", span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(intrinsicSize = IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
                    ) {
                        if (state.muscleLoad != null) {
                            MuscleLoadingCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .scalableClick(onClick = contract::onOpenMuscleLoading),
                                summary = state.muscleLoad
                            )
                        }
                        if (state.streak != null) {
                            TrainingStreakCard(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .scalableClick(onClick = contract::onOpenTrainingStreak),
                                value = state.streak
                            )
                        }
                    }
                }

                if (densityMetric != null) {
                    item(key = "performance_density") {
                        val onPerformanceMetricClickProvider =
                            remember(densityMetric.type) {
                                { contract.onPerformanceMetricClick(densityMetric.type) }
                            }
                        PerformanceTrendCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = densityMetric
                        )
                    }
                } else {
                    item(key = "performance_density_spacer") {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (volumeMetric != null) {
                    item(key = "performance_volume") {
                        val onPerformanceMetricClickProvider =
                            remember(volumeMetric.type) {
                                { contract.onPerformanceMetricClick(volumeMetric.type) }
                            }
                        PerformanceTrendCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = volumeMetric
                        )
                    }
                } else {
                    item(key = "performance_volume_spacer") {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (repetitionsMetric != null) {
                    item(key = "performance_repetitions") {
                        val onPerformanceMetricClickProvider =
                            remember(repetitionsMetric.type) {
                                { contract.onPerformanceMetricClick(repetitionsMetric.type) }
                            }
                        PerformanceTrendCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = repetitionsMetric
                        )
                    }
                } else {
                    item(key = "performance_repetitions_spacer") {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (intensityMetric != null) {
                    item(key = "performance_intensity") {
                        val onPerformanceMetricClickProvider =
                            remember(intensityMetric.type) {
                                { contract.onPerformanceMetricClick(intensityMetric.type) }
                            }
                        PerformanceTrendCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = intensityMetric
                        )
                    }
                } else {
                    item(key = "performance_intensity_spacer") {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.digest != null) {
                    item(key = "digest", span = { GridItemSpan(2) }) {
                        DigestCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scalableClick(onClick = contract::onOpenWeeklyDigest),
                            value = state.digest,
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onStartTraining
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    )
}

@AppPreview
@Composable
private fun HomeScreenPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(
                lastTraining = stubTraining(),
                digest = stubDigest(),
                totalDuration = 28.hours,
                spotlight = stubExerciseSpotlight(),
                muscleLoad = stubMuscleLoadSummary(),
                streak = stubTrainingStreaks().first(),
                performance = stubPerformanceMetrics(),
            ),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}

@AppPreview
@Composable
private fun HomeScreenEmptyPreview() {
    PreviewContainer {
        HomeScreen(
            state = HomeState(),
            loaders = persistentSetOf(),
            contract = HomeContract.Empty
        )
    }
}
