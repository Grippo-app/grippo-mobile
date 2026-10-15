package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Composable
public fun GoalCard(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
    onUpdateClick: () -> Unit,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Large,
        content = {
            GoalCardContent(
                value = value,
                onUpdateClick = onUpdateClick,
                presentation = presentation,
            )
        }
    )
}

@AppPreview
@Composable
private fun GoalCardPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { !it.isFinished && it.daysRemaining >= 0 },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { it.daysRemaining < 0 },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardCompletedPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { it.isFinished },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { !it.isFinished && it.daysRemaining > 0 },
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewDueTodayPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { !it.isFinished && it.daysRemaining == 0 }
                .copy(isFinished = true),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewDriftingPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first {
                !it.isFinished && it.daysRemaining > 0 &&
                        it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
            },
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOffTrackPreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first { it.score < GoalProgressState.DRIFTING_MIN }
        GoalCard(
            value = value.copy(
                goal = value.goal.copy(primaryGoal = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
            ),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { it.daysRemaining < 0 },
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewCompletedPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { it.isFinished }.copy(score = 100),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewNoAnalyzedWorkoutsPreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first { !it.isFinished && it.daysRemaining > 0 }
        GoalCard(
            value = value.copy(
                goal = value.goal.copy(primaryGoal = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
                score = 0,
                sessionCount = 0,
            ),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewFinishedOffTrackPreview() {
    PreviewContainer {
        GoalCard(
            value = goalCardPreviewValues().first { it.isFinished }.copy(
                score = 0,
                sessionCount = 0,
            ),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}

internal fun goalCardPreviewValues(): List<GoalProgressState> {
    val now = LocalDateTime(2026, 10, 15, 10, 0)
    val date = DateTimeFormatState.of(
        value = now,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    )
    return stubGoalProgressList().map { value ->
        value.copy(
            now = date,
            goal = value.goal.copy(
                primaryGoal = GoalPrimaryGoalEnumState.GET_STRONGER,
                createdAt = DateTimeFormatState.of(
                    value = now.date.minus(DatePeriod(days = value.daysElapsed)).atTime(now.time),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
                target = DateTimeFormatState.of(
                    value = now.date.plus(DatePeriod(days = value.daysRemaining)).atTime(now.time),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.DateMmmDdYyyy,
                ),
            ),
        )
    }
}
