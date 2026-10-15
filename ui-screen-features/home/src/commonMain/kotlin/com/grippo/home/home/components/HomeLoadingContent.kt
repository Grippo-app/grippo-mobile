package com.grippo.home.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading
import com.grippo.home.home.HomeState
import com.grippo.home.home.HomeUnlock

@Composable
internal fun HomeLoadingContent(
    modifier: Modifier,
    state: HomeState,
    onPeriodChange: () -> Unit,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val lifetimeTrainingCount = state.user?.stats?.trainingsCount
    val horizontalPadding = AppTokens.dp.screen.horizontalPadding

    LazyColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentPadding = PaddingValues(vertical = AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        item(key = "last_training_loading") {
            HomeLoadingRecap(
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        item(key = "highlights_header_loading") {
            Column(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .padding(top = AppTokens.dp.contentPadding.subContent),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                if (state.range.value != null && (lifetimeTrainingCount ?: 0) > 0) {
                    HighlightsHeader(
                        modifier = Modifier.fillMaxWidth(),
                        range = state.range,
                        onPeriodChange = onPeriodChange,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeLoadingText(
                            modifier = Modifier.weight(2.5f),
                            style = AppTokens.typography.h4(),
                        )
                        HomeLoadingBone(
                            modifier = Modifier
                                .weight(1f)
                                .height(AppTokens.dp.button.small.height),
                        )
                    }
                }
                HomeLoadingText(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    style = AppTokens.typography.b13Med(),
                )
            }
        }

        if (state.hasGoal) {
            item(key = "goal_progress_loading") {
                HomeLoadingGoal(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                )
            }
        }

        item(key = "muscle_loading") {
            HomeLoadingMuscle(
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        item(key = "training_streak_loading") {
            HomeLoadingStreak(
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        if (lifetimeTrainingCount != null && lifetimeTrainingCount > 0) {
            item(key = "exercise_spotlight_loading") {
                Column(
                    modifier = Modifier
                        .padding(top = AppTokens.dp.contentPadding.subContent)
                        .clearAndSetSemantics {},
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    HomeLoadingText(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .fillMaxWidth(0.6f),
                        style = AppTokens.typography.h4(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                    ) {
                        repeat(2) {
                            HomeLoadingSpotlight(
                                modifier = Modifier
                                    .width(AppTokens.dp.metrics.overview.insightCardWidth)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }

        if (lifetimeTrainingCount != null && HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            item(key = "performance_header_loading") {
                Column(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(top = AppTokens.dp.contentPadding.subContent),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                ) {
                    HomeLoadingText(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        style = AppTokens.typography.h4(),
                    )
                    HomeLoadingText(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = AppTokens.typography.b13Med(),
                    )
                }
            }

            item(key = "performance_duration_loading") {
                HomeLoadingPerformance(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    compact = true,
                )
            }
        }

        if (lifetimeTrainingCount != null && HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            items(count = 2, key = { "performance_row_loading_$it" }) {
                Row(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    repeat(2) {
                        HomeLoadingPerformance(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLoadingRecap(modifier: Modifier) {
    HomeLoadingPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                    HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.h5())
                    HomeLoadingText(modifier = Modifier.fillMaxWidth(0.7f), style = AppTokens.typography.b13Med())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
                    HomeLoadingFact(modifier = Modifier.weight(1f))
                    HomeLoadingFact(modifier = Modifier.weight(1f))
                }
            }
            HomeLoadingBone(modifier = Modifier.size(AppTokens.dp.illustration.compact))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeLoadingFact(modifier = Modifier.weight(1f))
            HomeLoadingBone(
                modifier = Modifier
                    .width(AppTokens.dp.illustration.compact)
                    .height(AppTokens.dp.button.small.height),
                shape = RoundedCornerShape(AppTokens.dp.button.radius),
            )
        }
    }
}

@Composable
private fun HomeLoadingGoal(modifier: Modifier) {
    HomeLoadingPanel(modifier = modifier) {
        HomeLoadingCardHeader()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.h4())
                HomeLoadingText(modifier = Modifier.fillMaxWidth(0.7f), style = AppTokens.typography.b11Semi())
                Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                    HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b12Med())
                    HomeLoadingText(modifier = Modifier.fillMaxWidth(0.6f), style = AppTokens.typography.b12Med())
                    HomeLoadingText(modifier = Modifier.fillMaxWidth(0.9f), style = AppTokens.typography.b12Med())
                }
            }
            Column(
                modifier = Modifier.width(AppTokens.dp.metrics.profile.goal.chart),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                HomeLoadingBone(
                    modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.chart),
                    shape = CircleShape,
                )
                HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b11Med())
            }
        }
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.8f), style = AppTokens.typography.b13Med())
    }
}

@Composable
private fun HomeLoadingMuscle(modifier: Modifier) {
    HomeLoadingPanel(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            HomeLoadingCardHeader()
            HomeLoadingText(modifier = Modifier.fillMaxWidth(0.8f), style = AppTokens.typography.b12Med())
        }
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.7f), style = AppTokens.typography.b12Med())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeLoadingBone(
                modifier = Modifier
                    .width(AppTokens.dp.metrics.overview.muscleImageWidth)
                    .height(AppTokens.dp.metrics.overview.muscleImageHeight),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                repeat(3) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.8f), style = AppTokens.typography.b13Semi())
                        HomeLoadingBone(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppTokens.dp.metrics.charts.indicator.height),
                        )
                    }
                }
            }
        }
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.65f), style = AppTokens.typography.b12Med())
    }
}

@Composable
private fun HomeLoadingStreak(modifier: Modifier) {
    HomeLoadingPanel(modifier = modifier) {
        HomeLoadingCardHeader()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(2.5f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeLoadingBone(modifier = Modifier.size(AppTokens.dp.metrics.overview.rhythmIcon))
                    HomeLoadingText(modifier = Modifier.weight(1f), style = AppTokens.typography.h4())
                }
                HomeLoadingText(modifier = Modifier.fillMaxWidth(0.85f), style = AppTokens.typography.b13Med())
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                horizontalAlignment = Alignment.End,
            ) {
                HomeLoadingText(modifier = Modifier.fillMaxWidth(0.5f), style = AppTokens.typography.h3())
                HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b12Med())
            }
        }
        HomeLoadingBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.metrics.charts.indicator.height),
        )
    }
}

@Composable
private fun HomeLoadingSpotlight(modifier: Modifier) {
    HomeLoadingPanel(modifier = modifier, small = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeLoadingBone(
                modifier = Modifier.size(AppTokens.dp.exerciseExampleImage.medium.size),
                shape = RoundedCornerShape(AppTokens.dp.exerciseExampleImage.medium.radius),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                HomeLoadingCardHeader()
                HomeLoadingText(modifier = Modifier.fillMaxWidth(0.6f), style = AppTokens.typography.b11Semi())
            }
        }
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.6f), style = AppTokens.typography.h4())
        HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b12Med())
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.8f), style = AppTokens.typography.b12Med())
    }
}

@Composable
private fun HomeLoadingPerformance(
    modifier: Modifier,
    compact: Boolean = false,
) {
    HomeLoadingPanel(modifier = modifier, small = true) {
        HomeLoadingCardHeader()
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.75f), style = AppTokens.typography.h2())
        if (!compact) {
            HomeLoadingText(modifier = Modifier.fillMaxWidth(0.5f), style = AppTokens.typography.b11Semi())
        }
        HomeLoadingText(modifier = Modifier.fillMaxWidth(), style = AppTokens.typography.b12Med())
        if (!compact) {
            HomeLoadingText(modifier = Modifier.fillMaxWidth(0.4f), style = AppTokens.typography.b13Semi())
        }
    }
}

@Composable
private fun HomeLoadingCardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HomeLoadingText(modifier = Modifier.weight(1f), style = AppTokens.typography.h5())
        HomeLoadingBone(modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon))
    }
}

@Composable
private fun HomeLoadingFact(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.65f), style = AppTokens.typography.h5())
        HomeLoadingText(modifier = Modifier.fillMaxWidth(0.8f), style = AppTokens.typography.b11Med())
    }
}

@Composable
private fun HomeLoadingPanel(
    modifier: Modifier,
    small: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = if (small) AppTokens.dp.metrics.panel.small.radius else AppTokens.dp.metrics.panel.large.radius
    val horizontalPadding = if (small) {
        AppTokens.dp.metrics.panel.small.horizontalPadding
    } else {
        AppTokens.dp.metrics.panel.large.horizontalPadding
    }
    val verticalPadding = if (small) {
        AppTokens.dp.metrics.panel.small.verticalPadding
    } else {
        AppTokens.dp.metrics.panel.large.verticalPadding
    }
    val spacer = if (small) AppTokens.dp.metrics.panel.small.spacer else AppTokens.dp.metrics.panel.large.spacer
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .border(
                width = AppTokens.dp.metrics.panel.borderWidth,
                color = AppTokens.colors.border.default,
                shape = shape,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(spacer),
        content = content,
    )
}

@Composable
private fun HomeLoadingText(
    modifier: Modifier,
    style: TextStyle,
) {
    val height = with(LocalDensity.current) { style.lineHeight.toDp() }
    HomeLoadingBone(modifier = modifier.height(height))
}

@Composable
private fun HomeLoadingBone(
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(AppTokens.dp.metrics.status.radius),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.divider.default)
            .shimmer(),
    )
}

@AppPreview
@Composable
private fun HomeLoadingUnknownUserPreview() {
    PreviewContainer {
        HomeLoadingContent(
            modifier = Modifier.fillMaxSize(),
            state = HomeState(),
            onPeriodChange = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeLoadingGoalAndAllMetricsPreview() {
    PreviewContainer {
        HomeLoadingContent(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState(lifetimeTrainingCount = 12, hasGoal = true),
            onPeriodChange = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeLoadingDurationUnlockedPreview() {
    PreviewContainer {
        HomeLoadingContent(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState(lifetimeTrainingCount = 2, hasGoal = false),
            onPeriodChange = {},
        )
    }
}
