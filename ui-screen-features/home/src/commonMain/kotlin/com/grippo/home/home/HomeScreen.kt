package com.grippo.home.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.metrics.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.stubExerciseSpotlightProgressWin
import com.grippo.core.state.metrics.stubGoalProgressList
import com.grippo.core.state.metrics.stubMuscleLoadSummary
import com.grippo.core.state.metrics.stubPerformanceMetrics
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.core.state.metrics.stubTrainingStreaks
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.frames.BottomOverlayContainer
import com.grippo.design.components.metrics.ExerciseSpotlightsCard
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.metrics.PerformanceMetricCard
import com.grippo.design.components.metrics.goal.GoalCard
import com.grippo.design.components.metrics.goal.GoalEmptyCard
import com.grippo.design.components.metrics.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.streak.TrainingStreakCard
import com.grippo.design.components.metrics.training.profile.TrainingLoadProfileCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dashboard
import com.grippo.design.resources.provider.icons.User
import com.grippo.design.resources.provider.resume_training_btn
import com.grippo.design.resources.provider.start_workout
import com.grippo.home.home.components.EmptyHomeContent
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
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
    val isEmptyState = state.lastTraining == null

    var showEmptyState by remember { mutableStateOf(false) }

    LaunchedEffect(isEmptyState) {
        if (isEmptyState) {
            delay(350)
            showEmptyState = true
        } else {
            showEmptyState = false
        }
    }

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

    AnimatedVisibility(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        visible = isEmptyState && showEmptyState,
        enter = fadeIn(animationSpec = tween(500)) + scaleIn(
            initialScale = 1.03f,
            animationSpec = tween(500),
        ),
    ) {
        EmptyHomeContent(
            modifier = Modifier
                .fillMaxWidth(),
            onStartTraining = contract::onStartTraining,
            onResumeTraining = contract::onResumeTraining,
            hasDraftTraining = state.hasDraftTraining
        )
    }

    if (isEmptyState) {
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
                item(key = "last_training", span = { GridItemSpan(2) }) {
                    LastTrainingCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.lastTraining,
                        onClick = contract::onOpenTrainings
                    )
                }

                item(key = "highlights_header", span = { GridItemSpan(2) }) {
                    HighlightsHeader(
                        modifier = Modifier.fillMaxWidth(),
                        range = state.range,
                        onPeriodChange = contract::onOpenPeriodPicker
                    )
                }

                if (state.goalProgress != null) {
                    item(key = "goal_progress", span = { GridItemSpan(2) }) {
                        GoalCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = state.goalProgress,
                        )
                    }
                } else {
                    item(key = "goal_progress_empty", span = { GridItemSpan(2) }) {
                        GoalEmptyCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = contract::onAddGoal
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

                if (state.profile != null) {
                    item(key = "training_load_profile", span = { GridItemSpan(2) }) {
                        TrainingLoadProfileCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scalableClick(onClick = contract::onOpenTrainingProfile),
                            value = state.profile,
                        )
                    }
                }

                if (state.spotlights.isNotEmpty()) {
                    item(key = "exercise_spotlight", span = { GridItemSpan(2) }) {
                        val list = remember(state.spotlights) {
                            state.spotlights
                                .toPersistentList()
                        }

                        ExerciseSpotlightsCard(
                            modifier = Modifier.fillMaxWidth(),
                            value = list,
                            onExampleClick = contract::onOpenExample
                        )
                    }
                }

                if (densityMetric != null) {
                    item(
                        key = "performance_density",
                        span = { GridItemSpan(if (volumeMetric == null) 2 else 1) }
                    ) {
                        val onPerformanceMetricClickProvider =
                            remember(densityMetric.type) {
                                { contract.onPerformanceMetricClick(densityMetric.type) }
                            }
                        PerformanceMetricCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = densityMetric
                        )
                    }
                }

                if (volumeMetric != null) {
                    item(
                        key = "performance_volume",
                        span = { GridItemSpan(if (densityMetric == null) 2 else 1) }
                    ) {
                        val onPerformanceMetricClickProvider =
                            remember(volumeMetric.type) {
                                { contract.onPerformanceMetricClick(volumeMetric.type) }
                            }
                        PerformanceMetricCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = volumeMetric
                        )
                    }
                }

                if (repetitionsMetric != null) {
                    item(
                        key = "performance_repetitions",
                        span = { GridItemSpan(if (intensityMetric == null) 2 else 1) }
                    ) {
                        val onPerformanceMetricClickProvider =
                            remember(repetitionsMetric.type) {
                                { contract.onPerformanceMetricClick(repetitionsMetric.type) }
                            }
                        PerformanceMetricCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = repetitionsMetric
                        )
                    }
                }

                if (intensityMetric != null) {
                    item(
                        key = "performance_intensity",
                        span = { GridItemSpan(if (repetitionsMetric == null) 2 else 1) }
                    ) {
                        val onPerformanceMetricClickProvider =
                            remember(intensityMetric.type) {
                                { contract.onPerformanceMetricClick(intensityMetric.type) }
                            }
                        PerformanceMetricCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .scalableClick(onClick = onPerformanceMetricClickProvider),
                            metric = intensityMetric
                        )
                    }
                }
            }
        },
        bottom = {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))


            if (state.hasDraftTraining) {
                Button(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    onClick = contract::onResumeTraining,
                    style = ButtonStyle.Error,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.resume_training_btn)
                    )
                )
            } else {
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
            }

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
                totalDuration = 28.hours,
                spotlights = persistentListOf(
                    stubExerciseSpotlightNeedsAttention(),
                    stubExerciseSpotlightProgressWin(),
                    stubExerciseSpotlightGoodFrequency(),
                    stubExerciseSpotlightNearBest(),
                ),
                muscleLoad = stubMuscleLoadSummary(),
                streak = stubTrainingStreaks().random(),
                performance = stubPerformanceMetrics(),
                profile = stubTrainingLoadProfile(),
                goalProgress = stubGoalProgressList().random(),
                hasDraftTraining = true
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
