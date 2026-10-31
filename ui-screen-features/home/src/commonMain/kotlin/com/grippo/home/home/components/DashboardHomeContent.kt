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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.resources.provider.home_all_indicators
import com.grippo.design.resources.provider.home_fewer_indicators
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.performance.PerformanceMetricState
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.metrics.performance.stubEmptyPerformanceMetrics
import com.grippo.design.components.empty.EmptyState
import com.grippo.design.components.empty.EmptyStateStyle
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.LastTrainingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCard
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCard
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.metrics.performance.ExerciseSpotlightsCard
import com.grippo.design.components.metrics.performance.PerformanceMetricCard
import com.grippo.design.components.metrics.performance.PerformanceMetricCardStyle
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
import com.grippo.design.resources.provider.home_unlock_banner_header
import com.grippo.home.home.HomeContract
import com.grippo.home.home.HomeState
import com.grippo.home.home.HomeUnlock
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

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
    var allIndicatorsExpanded by rememberSaveable(state.range) { mutableStateOf(false) }

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

        if (state.goalProgress != null) {
            item(key = "current_goal", span = { GridItemSpan(2) }) {
                GoalCard(
                    modifier = Modifier.fillMaxWidth().scalableClick(onClick = contract::onOpenGoalDetails),
                    value = state.goalProgress,
                    style = GoalCardStyle.Current(onUpdateClick = contract::onAddGoal),
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
            }
        }

        if (state.goalProgress != null) {
            item(key = "goal_progress", span = { GridItemSpan(2) }) {
                GoalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppTokens.dp.contentPadding.subContent)
                        .scalableClick(onClick = contract::onOpenGoalDetails),
                    value = state.goalProgress,
                    style = GoalCardStyle.Period,
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
                        .padding(vertical = AppTokens.dp.contentPadding.subContent)
                        .scalableClick(onClick = contract::onOpenTrainingStreak),
                    value = state.streak,
                    style = TrainingStreakCardStyle.Section,
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
                        modifier = Modifier.semantics { heading() },
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
                        modifier = Modifier.semantics { heading() },
                        text = AppTokens.strings.res(Res.string.home_metrics_title),
                        style = AppTokens.typography.h4(),
                        color = AppTokens.colors.text.primary,
                    )
                    if (visiblePerformanceMetrics.all { it.isEmpty() }) {
                        EmptyState(
                            text = AppTokens.strings.res(Res.string.home_metrics_empty_description),
                            style = EmptyStateStyle.Text,
                        )
                    }
                }
            }
        }

        if (HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            performanceMetricsItem(
                itemKey = "performance_primary",
                first = durationMetric,
                second = volumeMetric.takeIf { HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount) },
                contract = contract,
            )
        }

        if (HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            item(key = "all_indicators", span = { GridItemSpan(2) }) {
                AllIndicatorsSection(
                    metrics = listOfNotNull(densityMetric, repetitionsMetric, intensityMetric).toPersistentList(),
                    expanded = allIndicatorsExpanded,
                    onToggle = { allIndicatorsExpanded = !allIndicatorsExpanded },
                    onMetricClick = contract::onPerformanceMetricClick,
                )
            }
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

@Composable
private fun AllIndicatorsSection(
    modifier: Modifier = Modifier,
    metrics: ImmutableList<PerformanceMetricState>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onMetricClick: (PerformanceMetricTypeState) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(text = AppTokens.strings.res(
                if (expanded) Res.string.home_fewer_indicators else Res.string.home_all_indicators,
            )),
            style = ButtonStyle.Secondary,
            onClick = onToggle,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
                metrics.forEach { metric ->
                    key(metric.type) {
                        PerformanceMetricCard(
                            modifier = Modifier.fillMaxWidth().scalableClick { onMetricClick(metric.type) },
                            metric = metric,
                            style = PerformanceMetricCardStyle.Overview,
                        )
                    }
                }
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
                        style = PerformanceMetricCardStyle.Overview,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun AllIndicatorsExpandedPreview() {
    PreviewContainer {
        AllIndicatorsSection(
            metrics = homePreviewState().performance.filter {
                it.type != PerformanceMetricTypeState.Duration && it.type != PerformanceMetricTypeState.Volume
            }.toPersistentList(),
            expanded = true,
            onToggle = {},
            onMetricClick = {},
        )
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

@AppPreview
@Composable
private fun DashboardHomeContentEmptyPeriodPreview() {
    PreviewContainer {
        DashboardHomeContent(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState().copy(
                lastTraining = null,
                muscleLoad = null,
                streak = null,
                goalProgress = null,
                spotlights = persistentListOf(),
                performance = stubEmptyPerformanceMetrics(),
            ),
            contract = HomeContract.Empty,
        )
    }
}
