package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardAssessment
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardCalendar
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Stable
public sealed interface GoalCardStyle {
    @Stable
    public data class Overview(
        public val onUpdateClick: () -> Unit,
    ) : GoalCardStyle

    @Stable
    public data object Assessment : GoalCardStyle

    @Stable
    public data class Calendar(
        public val onUpdateClick: () -> Unit,
    ) : GoalCardStyle
}

@Composable
public fun GoalCard(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    style: GoalCardStyle,
) {
    when (style) {
        is GoalCardStyle.Overview -> MetricSectionPanel(
            modifier = modifier,
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = style.onUpdateClick,
                )
            },
        )

        GoalCardStyle.Assessment -> GoalCardAssessment(
            modifier = modifier,
            value = value,
        )

        is GoalCardStyle.Calendar -> GoalCardCalendar(
            modifier = modifier,
            value = value,
            onUpdateClick = style.onUpdateClick,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining > 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewDueTodayPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining == 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewDriftingPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first {
                !it.isFinished && it.daysRemaining > 0 &&
                        it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
            },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOffTrackPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true }
        GoalCard(
            value = value.copy(
                goal = value.goal.copy(primaryGoal = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
            ),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewCompletedPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.isFinished }.copy(score = 100),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewNoAnalyzedWorkoutsPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining > 0 }
        GoalCard(
            value = value.copy(
                goal = value.goal.copy(primaryGoal = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
                score = null,
                sessionCount = 0,
                evidence = value.evidence.copy(setCount = 0),
                findings = kotlinx.collections.immutable.persistentListOf(),
            ),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewFinishedOffTrackPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.isFinished }
        GoalCard(
            value = value.copy(
                score = null,
                sessionCount = 0,
                evidence = value.evidence.copy(setCount = 0),
                findings = kotlinx.collections.immutable.persistentListOf(),
            ),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewPartialEvidencePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.daysRemaining > 0 }
        GoalCard(
            value = value.copy(evidence = value.evidence.copy(missingMetadataSetCount = 8)),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}
