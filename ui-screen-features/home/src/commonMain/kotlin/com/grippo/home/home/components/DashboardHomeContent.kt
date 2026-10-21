package com.grippo.home.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.performance.ExerciseSpotlightsCard
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.utils.AnchorScrollBehavior
import com.grippo.design.components.utils.rememberAnchoredLazyGridState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.home_exercise_highlights_title
import com.grippo.design.resources.provider.home_metrics_empty_description
import com.grippo.design.resources.provider.home_metrics_title
import com.grippo.design.resources.provider.home_progress_details_hint
import com.grippo.design.resources.provider.home_unlock_banner_header
import com.grippo.home.home.HomeContract
import com.grippo.home.home.HomeState
import com.grippo.home.home.HomeUnlock

@Composable
internal fun DashboardHomeContent(
    modifier: Modifier,
    state: HomeState,
    contract: HomeContract,
) {
    val basePadding = PaddingValues(
        start = AppTokens.dp.screen.horizontalPadding,
        top = AppTokens.dp.contentPadding.content,
        end = AppTokens.dp.screen.horizontalPadding,
        bottom = AppTokens.dp.screen.verticalPadding,
    )

    val lifetimeTrainingCount: Int = state.user?.stats?.trainingsCount ?: 0

    val metricsByType = remember(state.performance) { state.performance.associateBy { it.type } }
    val durationMetric = metricsByType[PerformanceMetricTypeState.Duration]
    val densityMetric = metricsByType[PerformanceMetricTypeState.Density]
    val volumeMetric = metricsByType[PerformanceMetricTypeState.Volume]
    val repetitionsMetric = metricsByType[PerformanceMetricTypeState.Repetitions]
    val intensityMetric = metricsByType[PerformanceMetricTypeState.Intensity]
    val visiblePerformanceMetrics = buildList {
        if (HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            durationMetric?.let { add(it) }
        }
        if (HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            addAll(listOfNotNull(densityMetric, volumeMetric, repetitionsMetric, intensityMetric))
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = if (state.lastTraining != null) {
                            AppTokens.dp.contentPadding.subContent
                        } else {
                            0.dp
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                HighlightsHeader(
                    modifier = Modifier.fillMaxWidth(),
                    range = state.range,
                    onPeriodChange = contract::onOpenPeriodPicker,
                )

                if (state.goalProgress != null || state.muscleLoad != null || state.streak != null) {
                    Text(
                        text = AppTokens.strings.res(Res.string.home_progress_details_hint),
                        style = AppTokens.typography.b13Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }

        if (state.goalProgress != null) {
            item(key = "goal_progress", span = { GridItemSpan(2) }) {
                GoalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onOpenGoalDetails),
                    value = state.goalProgress,
                    style = GoalCardStyle.Overview(onUpdateClick = contract::onAddGoal),
                )
            }
        }

        if (state.muscleLoad != null) {
            item(key = "muscle_loading", span = { GridItemSpan(2) }) {
                MuscleLoadingCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onOpenMuscleLoading),
                    summary = state.muscleLoad,
                    style = MuscleLoadingCardStyle.Overview,
                )
            }
        }

        if (state.streak != null) {
            item(key = "training_streak", span = { GridItemSpan(2) }) {
                TrainingStreakCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scalableClick(onClick = contract::onOpenTrainingStreak),
                    value = state.streak,
                    presentation = MetricCardPresentation.Overview,
                )
            }
        }

        if (state.spotlights.isNotEmpty()) {
            item(key = "exercise_spotlight", span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.subContent),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    Text(
                        text = AppTokens.strings.res(Res.string.home_exercise_highlights_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val horizontalPadding = AppTokens.dp.screen.horizontalPadding
                        ExerciseSpotlightsCard(
                            modifier = Modifier.requiredWidth(maxWidth + horizontalPadding * 2),
                            value = state.spotlights,
                            contentPadding = PaddingValues(horizontal = horizontalPadding),
                            onExampleClick = contract::onOpenExample,
                        )
                    }
                }
            }
        }

        if (visiblePerformanceMetrics.isNotEmpty()) {
            item(key = "performance_header", span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.subContent),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                ) {
                    Text(
                        text = AppTokens.strings.res(Res.string.home_metrics_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                    if (visiblePerformanceMetrics.all { it.isEmpty() }) {
                        Text(
                            text = AppTokens.strings.res(Res.string.home_metrics_empty_description),
                            style = AppTokens.typography.b13Med(),
                            color = AppTokens.colors.text.secondary,
                        )
                    }
                }
            }
        }

        if (HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            performanceMetricsItem(
                itemKey = "performance_duration",
                first = durationMetric,
                contract = contract,
            )
        }

        if (HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            performanceMetricsItem(
                itemKey = "performance_density_volume",
                first = densityMetric,
                second = volumeMetric,
                contract = contract,
            )
            performanceMetricsItem(
                itemKey = "performance_repetitions_intensity",
                first = repetitionsMetric,
                second = intensityMetric,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTokens.dp.contentPadding.subContent),
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

private fun LazyGridScope.performanceMetricsItem(
    itemKey: String,
    first: PerformanceMetricState?,
    second: PerformanceMetricState? = null,
    contract: HomeContract,
) {
    val metrics = listOfNotNull(first, second)
    if (metrics.isEmpty()) return
    item(key = itemKey, span = { GridItemSpan(2) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            metrics.forEach { metric ->
                key(metric.type) {
                    val onClick = remember(contract, metric.type) {
                        { contract.onPerformanceMetricClick(metric.type) }
                    }
                    PerformanceMetricCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .scalableClick(onClick = onClick),
                        metric = metric,
                        presentation = MetricCardPresentation.Overview,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun DashboardHomeContentPreview() {
    PreviewContainer {
        DashboardHomeContent(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState(),
            contract = HomeContract.Empty,
        )
    }
}
