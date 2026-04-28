package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitRuleState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.design.components.metrics.profile.goal.internal.GoalFitFindingsBlock
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun GoalFitDriftingCard(
    findings: ImmutableList<GoalFitFindingState>,
    modifier: Modifier = Modifier,
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
        GoalFitDriftingCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 35f,
                    targetMin = 60f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 8f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("CHEST_MUSCLES"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 3f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("LEGS"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = -0.02f,
                    targetMin = 0.01f,
                    targetMax = null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDriftingCardInversionPreview() {
    PreviewContainer {
        GoalFitDriftingCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 2f,
                    targetMin = null,
                    targetMax = 0f,
                    context = persistentListOf("ARMS_AND_FOREARMS", "SHOULDER_MUSCLES"),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDriftingCardRttOverdonePreview() {
    PreviewContainer {
        GoalFitDriftingCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.RETURN_TO_TRAINING_WEEKLY_FREQUENCY,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 5.0f,
                    targetMin = 1.0f,
                    targetMax = 3.0f,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 1.7f,
                    targetMin = null,
                    targetMax = 1.5f,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.RETURN_TO_TRAINING_INTENSITY_FLOOR,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 32f,
                    targetMin = null,
                    targetMax = 10f,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDriftingCardMixedWithPassPreview() {
    // Mixed input incl. PASS findings; the card filters them out.
    PreviewContainer {
        GoalFitDriftingCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.MAINTAIN_WORK_BALANCE_SKEW,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0.55f,
                    targetMin = null,
                    targetMax = 0.70f,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.MAINTAIN_WEEKLY_FREQUENCY,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 1.7f,
                    targetMin = 2.0f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.MAINTAIN_WEEKLY_VOLUME_STABILITY,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 0.72f,
                    targetMin = null,
                    targetMax = 0.30f,
                ),
            ),
        )
    }
}
