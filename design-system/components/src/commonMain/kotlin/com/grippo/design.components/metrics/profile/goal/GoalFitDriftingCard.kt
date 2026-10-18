package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.design.components.metrics.profile.goal.internal.GoalFitFindingsBlock
import com.grippo.core.state.metrics.profile.stubGoalFitFindings
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun GoalFitDriftingCard(
    modifier: Modifier = Modifier,
    findings: ImmutableList<GoalFitFindingState>,
) {
    val drifting = remember(findings) {
        findings
            .filter { it.severity != GoalFitSeverityState.PASS }
            .toPersistentList()
    }

    GoalFitFindingsBlock(
        findings = drifting,
        modifier = modifier,
    )
}

@AppPreview
@Composable
private fun GoalFitDriftingCardBuildMusclePreview() {
    PreviewContainer {
        GoalFitDriftingCard(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.BUILD_MUSCLE))
    }
}

@AppPreview
@Composable
private fun GoalFitDriftingCardGetStrongerPreview() {
    PreviewContainer {
        GoalFitDriftingCard(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.GET_STRONGER))
    }
}

@AppPreview
@Composable
private fun GoalFitDriftingCardReturnToTrainingPreview() {
    PreviewContainer {
        GoalFitDriftingCard(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.RETURN_TO_TRAINING))
    }
}
