package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitRuleState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_fit_diagnostic_subtitle
import com.grippo.design.resources.provider.goal_fit_diagnostic_title
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun GoalFitDiagnosticBlock(
    modifier: Modifier = Modifier,
    findings: ImmutableList<GoalFitFindingState>,
) {
    if (findings.isEmpty()) return

    val groups = remember(findings) {
        findings
            .groupBy { it.rule }
            .map { (rule, list) ->
                val sorted = list.sortedBy { it.severity.sortKey }
                FindingGroup(
                    rule = rule,
                    findings = sorted,
                    worstSeverity = sorted.first().severity
                )
            }
            .sortedWith(
                compareBy({ it.worstSeverity.sortKey }, { it.rule.ordinal }),
            )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        DiagnosticHeader(modifier = Modifier.fillMaxWidth())

        groups.forEach { group ->
            key(group.rule) {
                FindingRule(
                    group = group,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DiagnosticHeader(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_title),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_fit_diagnostic_subtitle),
            style = AppTokens.typography.b11Reg(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun FindingRule(
    modifier: Modifier = Modifier,
    group: FindingGroup,
) {
    val title = group.rule.title()
    val singleFinding = group.findings.singleOrNull()

    if (singleFinding != null && singleFinding.rule == GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION) {
        FindingInversionRow(title = title, finding = singleFinding, modifier = modifier)
        return
    }

    if (singleFinding != null && singleFinding.context.isEmpty()) {
        FindingSingleRow(title = title, finding = singleFinding, modifier = modifier)
        return
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
        )

        group.findings.forEach { finding ->
            key(finding.context) {
                FindingSubRow(finding = finding, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FindingSingleRow(
    modifier: Modifier = Modifier,
    title: String,
    finding: GoalFitFindingState,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        SeverityDot(color = finding.dotColor())

        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = finding.valueTargetText(),
            style = AppTokens.typography.b13Med(),
            color = finding.valueColor(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FindingInversionRow(
    modifier: Modifier = Modifier,
    title: String,
    finding: GoalFitFindingState,
) {
    val statusColor = if (finding.severity == GoalFitSeverityState.FAIL) {
        AppTokens.colors.semantic.error
    } else {
        AppTokens.colors.text.secondary
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        SeverityDot(color = finding.dotColor())

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = finding.inversionStatusText(),
                style = AppTokens.typography.b11Reg(),
                color = statusColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FindingSubRow(
    modifier: Modifier = Modifier,
    finding: GoalFitFindingState,
) {
    Row(
        modifier = modifier.padding(start = AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        SeverityDot(color = finding.dotColor())

        Text(
            modifier = Modifier.weight(1f),
            text = finding.contextLabel(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = finding.valueTargetText(),
            style = AppTokens.typography.b13Med(),
            color = finding.valueColor(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SeverityDot(
    modifier: Modifier = Modifier,
    color: Color,
) {
    Box(
        modifier = modifier
            .size(AppTokens.dp.metrics.profile.goal.focusDistribution.legendDot)
            .clip(CircleShape)
            .background(color, CircleShape),
    )
}

@Immutable
private data class FindingGroup(
    val rule: GoalFitRuleState,
    val findings: List<GoalFitFindingState>,
    val worstSeverity: GoalFitSeverityState,
)

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockBuildMuscleHealthyPreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 12f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("LEGS"),
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockBuildMuscleMisalignedPreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 14f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("BACK_MUSCLES"),
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
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 0.7f,
                    targetMin = 1.5f,
                    targetMax = null,
                    context = persistentListOf("LEGS"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 1f,
                    targetMin = null,
                    targetMax = 0f,
                    context = persistentListOf("ARMS_AND_FOREARMS"),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = -0.02f,
                    targetMin = 0.01f,
                    targetMax = null,
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockGetStrongerHealthyPreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockMixedSeverityPreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
                GoalFitFindingState(
                    rule = GoalFitRuleState.MAINTAIN_GROUP_COVERAGE,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 6f,
                    targetMin = 5f,
                    targetMax = 6f,
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockInversionFailPreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitDiagnosticBlockRttOverdonePreview() {
    PreviewContainer {
        GoalFitDiagnosticBlock(
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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
