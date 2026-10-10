package com.grippo.home.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.distribution.stubMuscleLoadSummary
import com.grippo.core.state.metrics.engagement.stubTrainingStreaks
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightGoodFrequency
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNearBest
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightNeedsAttention
import com.grippo.core.state.metrics.performance.stubExerciseSpotlightProgressWin
import com.grippo.core.state.metrics.performance.stubPerformanceMetrics
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.stubUser
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.performance.ExerciseSpotlightsCard
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyGridState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.home_unlock_banner_header
import com.grippo.home.home.HomeContract
import com.grippo.home.home.HomeState
import com.grippo.home.home.HomeUnlock
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardHomeContent(
    modifier: Modifier,
    state: HomeState,
    contract: HomeContract,
) {
    val basePadding = PaddingValues(
        horizontal = AppTokens.dp.screen.horizontalPadding,
        vertical = AppTokens.dp.contentPadding.content,
    )

    val lifetimeTrainingCount: Int = state.user?.stats?.trainingsCount ?: 0

    val metricsByType = remember(state.performance) { state.performance.associateBy { it.type } }
    val durationMetric = metricsByType[PerformanceMetricTypeState.Duration]
    val densityMetric = metricsByType[PerformanceMetricTypeState.Density]
    val volumeMetric = metricsByType[PerformanceMetricTypeState.Volume]
    val repetitionsMetric = metricsByType[PerformanceMetricTypeState.Repetitions]
    val intensityMetric = metricsByType[PerformanceMetricTypeState.Intensity]

    val gridState = rememberAnchoredLazyGridState(
        behavior = AnchorScrollBehavior.Animated,
    )

    LazyVerticalGrid(
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(2),
        contentPadding = basePadding,
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

        if (state.goalProgress != null) {
            item(key = "goal_progress", span = { GridItemSpan(2) }) {
                GoalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onOpenGoalDetails),
                    value = state.goalProgress,
                    onUpdateClick = contract::onAddGoal
                )
            }
        }

        if (state.muscleLoad != null || state.streak != null) {
            item(
                key = "muscle_loading_and_training_streak",
                span = { GridItemSpan(2) }) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    if (state.muscleLoad != null) {
                        MuscleLoadingCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxRowHeight()
                                .scalableClick(onClick = contract::onOpenMuscleLoading),
                            summary = state.muscleLoad
                        )
                    }
                    if (state.streak != null) {
                        TrainingStreakCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxRowHeight()
                                .scalableClick(onClick = contract::onOpenTrainingStreak),
                            value = state.streak
                        )
                    }
                }
            }
        }

        if (state.spotlights.isNotEmpty()) {
            item(key = "exercise_spotlight", span = { GridItemSpan(2) }) {
                ExerciseSpotlightsCard(
                    modifier = Modifier.fillMaxWidth(),
                    value = state.spotlights,
                    onExampleClick = contract::onOpenExample
                )
            }
        }

        if (HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            performanceMetricItem(
                key = "performance_duration",
                metric = durationMetric,
                span = 2,
                contract = contract,
            )
        }

        if (HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            performanceMetricItem(
                key = "performance_density",
                metric = densityMetric,
                span = if (volumeMetric == null) 2 else 1,
                contract = contract,
            )
            performanceMetricItem(
                key = "performance_volume",
                metric = volumeMetric,
                span = if (densityMetric == null) 2 else 1,
                contract = contract,
            )
            performanceMetricItem(
                key = "performance_repetitions",
                metric = repetitionsMetric,
                span = if (intensityMetric == null) 2 else 1,
                contract = contract,
            )
            performanceMetricItem(
                key = "performance_intensity",
                metric = intensityMetric,
                span = if (repetitionsMetric == null) 2 else 1,
                contract = contract,
            )
        }

        if (state.user != null && HomeUnlock.shouldShowBanner(
                lifetimeCount = lifetimeTrainingCount,
                hasGoal = state.hasGoal,
            )
        ) {
            item(key = "home_unlock_banner_header", span = { GridItemSpan(2) }) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.home_unlock_banner_header),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }

            item(key = "home_unlock_banner", span = { GridItemSpan(2) }) {
                HomeUnlockBanner(
                    stats = state.user.stats,
                    hasGoal = state.hasGoal,
                    onAddGoal = contract::onAddGoal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun LazyGridScope.performanceMetricItem(
    key: String,
    metric: PerformanceMetricState?,
    span: Int,
    contract: HomeContract,
) {
    if (metric == null) return
    item(key = key, span = { GridItemSpan(span) }) {
        val onClick = remember(contract, metric.type) {
            { contract.onPerformanceMetricClick(metric.type) }
        }
        PerformanceMetricCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .scalableClick(onClick = onClick),
            metric = metric,
        )
    }
}

@AppPreview
@Composable
private fun DashboardHomeContentPreview() {
    PreviewContainer {
        DashboardHomeContent(
            modifier = Modifier.fillMaxSize(),
            state = HomeState(
                lastTraining = stubTraining(),
                spotlights = persistentListOf(
                    stubExerciseSpotlightNeedsAttention(),
                    stubExerciseSpotlightProgressWin(),
                    stubExerciseSpotlightGoodFrequency(),
                    stubExerciseSpotlightNearBest(),
                ),
                muscleLoad = stubMuscleLoadSummary(),
                streak = stubTrainingStreaks().random(),
                performance = stubPerformanceMetrics(),
                goalProgress = stubGoalProgressList().random(),
                user = stubUser()
            ),
            contract = HomeContract.Empty,
        )
    }
}
