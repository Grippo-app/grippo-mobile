package com.grippo.home.home.components

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.metrics.HighlightsHeader
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardSkeleton
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingCardStyle
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardSkeleton
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonPanel
import com.grippo.design.components.skeleton.SkeletonPanelStyle
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading
import com.grippo.home.home.HomeState
import com.grippo.home.home.HomeUnlock

@Composable
internal fun HomeSkeleton(
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
        item(key = "highlights_header_skeleton") {
            val textMeasurer = rememberTextMeasurer()

            Column(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                if (state.range.value != null && (lifetimeTrainingCount ?: 0) > 0) {
                    HighlightsHeader(
                        modifier = Modifier.fillMaxWidth(),
                        range = state.range,
                        onPeriodChange = onPeriodChange,
                    )
                } else {
                    val pickerLine = rememberTextHeight(style = AppTokens.typography.h6(), textMeasurer = textMeasurer)
                    val pickerHeight = maxOf(pickerLine, AppTokens.dp.periodPicker.icon)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SkeletonText(
                                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                                style = AppTokens.typography.h4(),
                                textMeasurer = textMeasurer,
                            )
                        }
                        SkeletonBone(
                            modifier = Modifier
                                .fillMaxWidth(AppTokens.dp.skeleton.line.short)
                                .height(pickerHeight),
                            animated = false,
                        )
                    }
                }
            }
        }

        item(key = "last_training_skeleton") {
            HomeRecapSkeleton(
                modifier = Modifier.padding(horizontal = horizontalPadding),
            )
        }

        if (state.hasGoal) {
            item(key = "current_goal_skeleton") {
                HomeGoalSkeleton(modifier = Modifier.padding(horizontal = horizontalPadding))
            }
        }

        item(key = "muscle_load_skeleton") {
            MuscleLoadingCardSkeleton(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                style = MuscleLoadingCardStyle.Overview,
            )
        }

        item(key = "training_streak_skeleton") {
            TrainingStreakCardSkeleton(
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.subContent,
                ),
                style = TrainingStreakCardStyle.Section,
            )
        }

        if (lifetimeTrainingCount != null && lifetimeTrainingCount > 0) {
            item(key = "exercise_spotlight_skeleton") {
                Column(
                    modifier = Modifier
                        .padding(top = AppTokens.dp.contentPadding.subContent)
                        .clearAndSetSemantics {},
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    SkeletonText(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                        style = AppTokens.typography.h4(),
                    )
                    HomeSpotlightSkeleton(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .fillMaxWidth(),
                    )
                }
            }
        }

        if (lifetimeTrainingCount != null && HomeUnlock.DurationTrend.isUnlocked(lifetimeTrainingCount)) {
            item(key = "performance_header_skeleton") {
                val textMeasurer = rememberTextMeasurer()

                Column(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(top = AppTokens.dp.contentPadding.subContent),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
                ) {
                    SkeletonText(
                        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                        style = AppTokens.typography.h4(),
                        textMeasurer = textMeasurer,
                    )
                }
            }

            item(key = "performance_duration_skeleton") {
                Row(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    HomePerformanceSkeleton(modifier = Modifier.weight(1f))
                    if (HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
                        HomePerformanceSkeleton(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (lifetimeTrainingCount != null && HomeUnlock.PerformanceTrends.isUnlocked(lifetimeTrainingCount)) {
            item(key = "all_indicators_skeleton") {
                SkeletonBone(
                    modifier = Modifier.padding(horizontal = horizontalPadding)
                        .fillMaxWidth().height(AppTokens.dp.button.medium.height),
                )
            }
        }
    }
}

@Composable
private fun HomeRecapSkeleton(modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val dateLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val labelLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text
    val factHeight = valueLine + textGap + labelLine
    val summaryHeight = titleLine + textGap + dateLine + AppTokens.dp.contentPadding.content + factHeight

    SkeletonPanel(
        modifier = modifier,
        contentHeight = summaryHeight,
    )
}

@Composable
private fun HomeGoalSkeleton(modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val scoreLine = rememberTextHeight(style = AppTokens.typography.h3(), textMeasurer = textMeasurer)
    val nameLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text

    val identityHeight = maxOf(AppTokens.dp.goal.markSize, nameLine + textGap + captionLine)
    val alignmentHeight = maxOf(scoreLine + textGap + captionLine, captionLine * 2 + textGap)

    SkeletonPanel(
        modifier = modifier,
        contentHeight = identityHeight + alignmentHeight + AppTokens.dp.button.small.height +
            AppTokens.dp.metrics.panel.large.spacer * 2,
        style = SkeletonPanelStyle.Large,
    )
}

@Composable
private fun HomeSpotlightSkeleton(modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val nameLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val chipLine = rememberTextHeight(style = AppTokens.typography.b11Semi(), textMeasurer = textMeasurer)
    val insightLine = rememberTextHeight(style = AppTokens.typography.h4(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val identityHeight = maxOf(nameLine, AppTokens.dp.metrics.overview.detailsIcon) +
        AppTokens.dp.contentPadding.subContent + chipLine + AppTokens.dp.metrics.status.verticalPadding * 2

    SkeletonPanel(
        modifier = modifier,
        contentHeight = maxOf(AppTokens.dp.exerciseExampleImage.medium.size, identityHeight) +
            insightLine + captionLine * 2 + AppTokens.dp.metrics.panel.large.spacer * 3,
    )
}

@Composable
private fun HomePerformanceSkeleton(
    modifier: Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h2(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val spacer = AppTokens.dp.metrics.panel.small.spacer
    val summaryHeight = maxOf(titleLine, AppTokens.dp.metrics.overview.detailsIcon) + valueLine + captionLine +
        spacer * 2
    SkeletonPanel(modifier = modifier, contentHeight = summaryHeight, style = SkeletonPanelStyle.Small)
}

@AppPreview
@Composable
private fun HomeSkeletonUnknownUserPreview() {
    PreviewContainer {
        HomeSkeleton(
            modifier = Modifier.fillMaxSize(),
            state = HomeState(),
            onPeriodChange = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeSkeletonGoalAndAllMetricsPreview() {
    PreviewContainer {
        HomeSkeleton(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState(lifetimeTrainingCount = 12, hasGoal = true),
            onPeriodChange = {},
        )
    }
}

@AppPreview
@Composable
private fun HomeSkeletonDurationUnlockedPreview() {
    PreviewContainer {
        HomeSkeleton(
            modifier = Modifier.fillMaxSize(),
            state = homePreviewState(lifetimeTrainingCount = 2, hasGoal = false),
            onPeriodChange = {},
        )
    }
}
