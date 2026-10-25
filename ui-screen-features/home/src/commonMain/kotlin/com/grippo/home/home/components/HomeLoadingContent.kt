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
            val textMeasurer = rememberTextMeasurer()

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
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                    style = AppTokens.typography.b13Med(),
                    textMeasurer = textMeasurer,
                    animated = true,
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
            MuscleLoadingCardSkeleton(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                style = MuscleLoadingCardStyle.Overview,
            )
        }

        item(key = "training_streak_loading") {
            TrainingStreakCardSkeleton(
                modifier = Modifier.padding(horizontal = horizontalPadding),
                style = TrainingStreakCardStyle.Overview,
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
                    SkeletonText(
                        modifier = Modifier
                            .padding(horizontal = horizontalPadding)
                            .fillMaxWidth(AppTokens.dp.skeleton.line.medium),
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
                    SkeletonText(
                        modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                        style = AppTokens.typography.b13Med(),
                        textMeasurer = textMeasurer,
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
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val dateLine = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val labelLine = rememberTextHeight(style = AppTokens.typography.b11Med(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text
    val factHeight = valueLine + textGap + labelLine
    val summaryHeight = titleLine + textGap + dateLine + AppTokens.dp.contentPadding.content + factHeight

    SkeletonPanel(
        modifier = modifier,
        contentHeight = maxOf(AppTokens.dp.illustration.compact, summaryHeight) +
            AppTokens.dp.metrics.panel.large.spacer +
            maxOf(factHeight, AppTokens.dp.button.small.height),
    )
}

@Composable
private fun HomeLoadingGoal(modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val goalLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val scoreLine = rememberTextHeight(style = AppTokens.typography.h2(), textMeasurer = textMeasurer)
    val criterionLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val periodLine = rememberTextHeight(style = AppTokens.typography.b12Semi(), textMeasurer = textMeasurer)
    val remainingLine = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text
    val rowGap = AppTokens.dp.contentPadding.subContent

    val identityHeight = maxOf(
        AppTokens.dp.goal.markSize,
        captionLine + textGap + goalLine,
        AppTokens.dp.metrics.overview.detailsIcon,
    )
    val alignmentHeight = maxOf(scoreLine, criterionLine + textGap + captionLine) + rowGap + captionLine
    val calendarHeight = maxOf(AppTokens.dp.metrics.overview.detailsIcon, periodLine, captionLine) + rowGap +
        remainingLine

    SkeletonPanel(
        modifier = modifier,
        contentHeight = identityHeight + alignmentHeight + calendarHeight +
            AppTokens.dp.divider.thickness * 2 + AppTokens.dp.metrics.panel.large.spacer * 4,
    )
}

@Composable
private fun HomeLoadingSpotlight(modifier: Modifier) {
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
private fun HomeLoadingPerformance(
    modifier: Modifier,
    compact: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val titleLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.h2(), textMeasurer = textMeasurer)
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val chipLine = rememberTextHeight(style = AppTokens.typography.b11Semi(), textMeasurer = textMeasurer)
    val deltaLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val spacer = AppTokens.dp.metrics.panel.small.spacer
    val summaryHeight = maxOf(titleLine, AppTokens.dp.metrics.overview.detailsIcon) + valueLine + captionLine +
        spacer * 2
    val contentHeight = if (compact) {
        summaryHeight
    } else {
        val chipHeight = chipLine + AppTokens.dp.metrics.status.verticalPadding * 2
        summaryHeight + chipHeight + spacer + AppTokens.dp.contentPadding.text + deltaLine
    }

    SkeletonPanel(modifier = modifier, contentHeight = contentHeight, style = SkeletonPanelStyle.Small)
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
