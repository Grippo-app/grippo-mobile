package com.grippo.home.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
        contentPadding = PaddingValues(
            top = AppTokens.dp.contentPadding.content,
            bottom = AppTokens.dp.screen.verticalPadding,
        ),
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
                    animated = state.range.value != null && (lifetimeTrainingCount ?: 0) > 0,
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
                    HomeLoadingSpotlight(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .fillMaxWidth(),
                    )
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
    val titleHeight = AppTokens.typography.h5().lineHeightDp()
    val factHeight = titleHeight + AppTokens.typography.b11Med().lineHeightDp() +
        AppTokens.dp.contentPadding.text
    val summaryHeight = titleHeight + AppTokens.typography.b13Med().lineHeightDp() +
        AppTokens.dp.contentPadding.text + AppTokens.dp.contentPadding.content + factHeight
    HomeLoadingPanel(
        modifier = modifier,
        contentHeight = maxOf(AppTokens.dp.illustration.compact, summaryHeight) +
            AppTokens.dp.metrics.panel.large.spacer +
            maxOf(factHeight, AppTokens.dp.button.small.height),
    )
}

@Composable
private fun HomeLoadingGoal(modifier: Modifier) {
    val identityHeight = maxOf(
        AppTokens.dp.goal.markSize,
        AppTokens.typography.b12Med().lineHeightDp() + AppTokens.dp.contentPadding.text +
            AppTokens.typography.h5().lineHeightDp(),
    )
    val alignmentHeight = maxOf(
        AppTokens.typography.h2().lineHeightDp(),
        AppTokens.typography.b13Semi().lineHeightDp() + AppTokens.dp.contentPadding.text +
            AppTokens.typography.b12Med().lineHeightDp(),
    ) + AppTokens.dp.contentPadding.subContent + AppTokens.typography.b12Med().lineHeightDp()
    val calendarHeight = maxOf(
        AppTokens.dp.metrics.overview.detailsIcon,
        AppTokens.typography.b12Semi().lineHeightDp(),
    ) + AppTokens.dp.contentPadding.subContent + AppTokens.dp.button.small.height
    HomeLoadingPanel(
        modifier = modifier,
        contentHeight = identityHeight + alignmentHeight + calendarHeight +
            AppTokens.typography.b12Med().lineHeightDp() * 2 +
            AppTokens.dp.divider.thickness * 2 + AppTokens.dp.metrics.panel.large.spacer * 5,
    )
}

@Composable
private fun HomeLoadingMuscle(modifier: Modifier) {
    val distributionHeight = (
        AppTokens.typography.b13Semi().lineHeightDp() + AppTokens.dp.contentPadding.text +
            AppTokens.dp.metrics.charts.indicator.height
        ) * 3 + AppTokens.dp.contentPadding.subContent * 2
    HomeLoadingPanel(
        modifier = modifier,
        contentHeight = AppTokens.typography.h5().lineHeightDp() +
            AppTokens.typography.b12Med().lineHeightDp() * 3 + AppTokens.dp.contentPadding.text +
            maxOf(AppTokens.dp.metrics.overview.muscleImageHeight, distributionHeight) +
            AppTokens.dp.metrics.panel.large.spacer * 3,
    )
}

@Composable
private fun HomeLoadingStreak(modifier: Modifier) {
    val headlineHeight = maxOf(
        AppTokens.dp.metrics.overview.rhythmIcon,
        AppTokens.typography.h4().lineHeightDp(),
    ) + AppTokens.typography.b13Med().lineHeightDp()
    val activeDaysHeight = AppTokens.typography.h3().lineHeightDp() +
        AppTokens.typography.b12Med().lineHeightDp()
    HomeLoadingPanel(
        modifier = modifier,
        contentHeight = AppTokens.typography.h5().lineHeightDp() +
            maxOf(headlineHeight, activeDaysHeight) + AppTokens.dp.contentPadding.text +
            AppTokens.dp.metrics.charts.indicator.height + AppTokens.dp.metrics.panel.large.spacer * 2,
    )
}

@Composable
private fun HomeLoadingSpotlight(modifier: Modifier) {
    val identityHeight = AppTokens.typography.h5().lineHeightDp() +
        AppTokens.dp.contentPadding.subContent + AppTokens.typography.b11Semi().lineHeightDp()
    HomeLoadingPanel(
        modifier = modifier,
        contentHeight = maxOf(AppTokens.dp.exerciseExampleImage.medium.size, identityHeight) +
            AppTokens.typography.h4().lineHeightDp() + AppTokens.typography.b12Med().lineHeightDp() * 2 +
            AppTokens.dp.metrics.panel.large.spacer * 3,
    )
}

@Composable
private fun HomeLoadingPerformance(
    modifier: Modifier,
    compact: Boolean = false,
) {
    val summaryHeight = AppTokens.typography.h5().lineHeightDp() +
        AppTokens.typography.h2().lineHeightDp() + AppTokens.typography.b12Med().lineHeightDp() +
        AppTokens.dp.metrics.panel.small.spacer * 2
    val contentHeight = if (compact) {
        summaryHeight
    } else {
        summaryHeight + AppTokens.typography.b11Semi().lineHeightDp() +
            AppTokens.typography.b13Semi().lineHeightDp() + AppTokens.dp.metrics.panel.small.spacer * 2
    }
    HomeLoadingPanel(modifier = modifier, contentHeight = contentHeight, small = true)
}

@Composable
private fun HomeLoadingPanel(
    modifier: Modifier,
    contentHeight: Dp,
    small: Boolean = false,
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
    val shape = RoundedCornerShape(radius)
    Box(
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
    ) {
        HomeLoadingBone(modifier = Modifier.fillMaxWidth().height(contentHeight))
    }
}

@Composable
private fun HomeLoadingText(
    modifier: Modifier,
    style: TextStyle,
    animated: Boolean = false,
) {
    HomeLoadingBone(modifier = modifier.height(style.lineHeightDp()), animated = animated)
}

@Composable
private fun TextStyle.lineHeightDp(): Dp = with(LocalDensity.current) { lineHeight.toDp() }

@Composable
private fun HomeLoadingBone(
    modifier: Modifier,
    animated: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.metrics.status.radius))
            .background(AppTokens.colors.divider.default)
            .shimmer(enabled = animated),
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
