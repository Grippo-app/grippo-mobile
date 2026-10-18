package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

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
            value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining >= 0 },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardCompletedPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.isFinished },
            onUpdateClick = {}
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining > 0 },
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
            value = stubGoalProgressList().first { it.daysRemaining == 0 },
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
            value = stubGoalProgressList().first {
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
        val value = stubGoalProgressList().first { it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true }
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
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
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
            value = stubGoalProgressList().first { it.isFinished }.copy(score = 100),
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
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
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
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
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
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
            onUpdateClick = {},
            presentation = MetricCardPresentation.Overview,
        )
    }
}
