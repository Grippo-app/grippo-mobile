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
public fun GoalFitOnTrackCard(
    findings: ImmutableList<GoalFitFindingState>,
    modifier: Modifier = Modifier,
) {
    val onTrack = remember(findings) {
        findings
            .filter { it.severity == GoalFitSeverityState.PASS }
            .toPersistentList()
    }

    GoalFitFindingsBlock(
        findings = onTrack,
        modifier = modifier,
    )
}

@AppPreview
@Composable
private fun GoalFitOnTrackCardBuildMusclePreview() {
    PreviewContainer {
        GoalFitOnTrackCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 72f,
                    targetMin = 60f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 14f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("CHEST_MUSCLES"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 16f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("BACK_MUSCLES"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0f,
                    targetMin = null,
                    targetMax = 0f,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0.04f,
                    targetMin = 0.01f,
                    targetMax = null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitOnTrackCardGetStrongerPreview() {
    PreviewContainer {
        GoalFitOnTrackCard(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.GET_STRONGER_COMPOUND_PRESENCE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 5f,
                    targetMin = 3f,
                    targetMax = 5f,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.GET_STRONGER_HEAVY_INTENSITY_SHARE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 48f,
                    targetMin = 40f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.GET_STRONGER_STRENGTH_REP_RANGE_SHARE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 62f,
                    targetMin = 50f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.GET_STRONGER_COMPOUND_TO_ISOLATION_RATIO,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 2.6f,
                    targetMin = 2.0f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.GET_STRONGER_STRENGTH_PROGRESSION,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0.05f,
                    targetMin = 0.03f,
                    targetMax = null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitOnTrackCardMixedWithFailingPreview() {
    // Mixed input incl. WARN/FAIL findings; the card filters them out.
    PreviewContainer {
        GoalFitOnTrackCard(
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
                    rule = GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 6f,
                    targetMin = 5f,
                    targetMax = 6f,
                ),
            ),
        )
    }
}
