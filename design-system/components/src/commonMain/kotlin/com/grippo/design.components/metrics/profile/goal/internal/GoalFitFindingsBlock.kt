package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitRuleState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.core.state.metrics.profile.stubGoalFitFindings
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Immutable
private data class FindingGroup(
    val rule: GoalFitRuleState,
    val findings: ImmutableList<GoalFitFindingState>,
    val worstSeverity: GoalFitSeverityState,
)

@Composable
internal fun GoalFitFindingsBlock(
    modifier: Modifier = Modifier,
    findings: ImmutableList<GoalFitFindingState>,
) {
    if (findings.isEmpty()) return

    val groups = remember(findings) {
        findings
            .groupBy { it.rule }
            .map { (rule, list) ->
                val sorted = list.sortedBy { it.severity.sortKey }.toPersistentList()
                FindingGroup(
                    rule = rule,
                    findings = sorted,
                    worstSeverity = sorted.first().severity,
                )
            }
            .sortedWith(
                compareBy({ it.worstSeverity.sortKey }, { it.rule.ordinal }),
            )
    }

    val radius = AppTokens.dp.metrics.profile.goal.breakdown.radius
    val shape = RoundedCornerShape(radius)

    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .border(AppTokens.dp.metrics.panel.borderWidth, AppTokens.colors.border.default, shape)
            .padding(
                horizontal = AppTokens.dp.metrics.profile.goal.breakdown.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.goal.breakdown.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
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
    FindingValueRow(
        title = title,
        finding = finding,
        modifier = modifier,
        titleStyle = AppTokens.typography.b14Med(),
    )
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
            )
            Text(
                text = finding.inversionStatusText(),
                style = AppTokens.typography.b11Reg(),
                color = statusColor,
            )
        }
    }
}

@Composable
private fun FindingSubRow(
    modifier: Modifier = Modifier,
    finding: GoalFitFindingState,
) {
    FindingValueRow(
        title = finding.contextLabel(),
        finding = finding,
        modifier = modifier.padding(start = AppTokens.dp.contentPadding.content),
        titleStyle = AppTokens.typography.b13Med(),
    )
}

@Composable
private fun FindingValueRow(
    modifier: Modifier = Modifier,
    title: String,
    finding: GoalFitFindingState,
    titleStyle: TextStyle,
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
            style = titleStyle,
            color = AppTokens.colors.text.primary,
        )
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = finding.valueTargetText(),
            style = AppTokens.typography.b13Med(),
            color = finding.valueColor(),
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
            .size(AppTokens.dp.metrics.profile.goal.breakdown.severityDot)
            .clip(CircleShape)
            .background(color, CircleShape),
    )
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockSingleRowPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 48f,
                    targetMin = 60f,
                    targetMax = null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockInversionPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
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
private fun GoalFitFindingsBlockGroupedSubRowsPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = persistentListOf(
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
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 14f,
                    targetMin = 10f,
                    targetMax = 20f,
                    context = persistentListOf("BACK_MUSCLES"),
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockMixedSeveritiesPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = -0.02f,
                    targetMin = 0.01f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_HYPERTROPHY_REP_RANGE_SHARE,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 48f,
                    targetMin = 60f,
                    targetMax = null,
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_ACCESSORY_INVERSION,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0f,
                    targetMin = null,
                    targetMax = 0f,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockFromStubPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.BUILD_MUSCLE)
                .toPersistentList(),
        )
    }
}
