package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.grippo.core.state.metrics.profile.GoalFitFindingState
import com.grippo.core.state.metrics.profile.GoalFitRuleState
import com.grippo.core.state.metrics.profile.GoalFitSeverityState
import com.grippo.core.state.metrics.profile.stubGoalFitFindings
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.indicators.Dot
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun GoalFitFindingsBlock(
    modifier: Modifier = Modifier,
    findings: ImmutableList<GoalFitFindingState>,
) {
    if (findings.isEmpty()) return

    val orderedFindings = remember(findings) {
        findings
            .groupBy { it.rule }
            .values
            .map { group -> group.sortedBy { it.severity.sortKey } }
            .sortedWith(
                compareBy({ it.first().severity.sortKey }, { it.first().rule.ordinal }),
            )
            .flatten()
    }

    val radius = AppTokens.dp.metrics.profile.goal.breakdown.radius
    val shape = RoundedCornerShape(radius)
    val titleStyle = AppTokens.typography.b14Semi()
    val titleLineHeight = rememberTextHeight(style = titleStyle)

    Column(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.metrics.profile.goal.breakdown.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.goal.breakdown.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        orderedFindings.forEachIndexed { index, finding ->
            key(finding.rule, finding.context) {
                FindingRow(
                    modifier = Modifier.fillMaxWidth(),
                    finding = finding,
                    titleStyle = titleStyle,
                    titleLineHeight = titleLineHeight,
                    showDivider = index < orderedFindings.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun FindingRow(
    modifier: Modifier = Modifier,
    finding: GoalFitFindingState,
    titleStyle: TextStyle,
    titleLineHeight: Dp,
    showDivider: Boolean,
) {
    val targetText = finding.targetText()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Box(
            modifier = Modifier.height(titleLineHeight),
            contentAlignment = Alignment.Center,
        ) {
            Dot(color = finding.dotColor())
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = finding.rule.title(),
                    style = titleStyle,
                    color = AppTokens.colors.text.primary,
                )
                if (finding.context.isNotEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = finding.contextLabel(),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
            Column(
                modifier = Modifier.padding(top = AppTokens.dp.contentPadding.subContent),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = finding.actualText(),
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                )
                if (targetText != null) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = targetText,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
            if (showDivider) {
                ContentDivider(
                    modifier = Modifier.padding(top = AppTokens.dp.contentPadding.content),
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockRepeatedRulePreview() {
    PreviewContainer {
        GoalFitFindingsBlock(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.BUILD_MUSCLE))
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockSingleRowsPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.GET_STRONGER))
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockRangePreview() {
    PreviewContainer {
        GoalFitFindingsBlock(findings = stubGoalFitFindings(GoalPrimaryGoalEnumState.RETURN_TO_TRAINING))
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockOptionalContentPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_FREQUENCY_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 2.6f,
                    targetMin = 1f,
                    targetMax = null,
                    context = persistentListOf(
                        MuscleGroupEnumState.CHEST_MUSCLES,
                        MuscleGroupEnumState.BACK_MUSCLES,
                        MuscleGroupEnumState.LEGS,
                    ),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_LOAD_PROGRESSION,
                    severity = GoalFitSeverityState.PASS,
                    actualValue = 0.024f,
                    targetMin = null,
                    targetMax = null,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockRoundingBoundariesPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(
            findings = persistentListOf(
                GoalFitFindingState(
                    rule = GoalFitRuleState.BUILD_MUSCLE_WEEKLY_SETS_PER_PRIMARY_GROUP,
                    severity = GoalFitSeverityState.WARN,
                    actualValue = 9.96f,
                    targetMin = 10f,
                    targetMax = null,
                    context = persistentListOf(MuscleGroupEnumState.CHEST_MUSCLES),
                ),
                GoalFitFindingState(
                    rule = GoalFitRuleState.RETURN_TO_TRAINING_LOAD_RAMP_GENTLE,
                    severity = GoalFitSeverityState.FAIL,
                    actualValue = 1.504f,
                    targetMin = null,
                    targetMax = 1.5f,
                ),
            ),
        )
    }
}

@AppPreview
@Composable
private fun GoalFitFindingsBlockEmptyPreview() {
    PreviewContainer {
        GoalFitFindingsBlock(findings = persistentListOf())
    }
}
