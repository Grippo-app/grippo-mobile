package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.design.components.goal.GoalMark
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_limited_data
import com.grippo.design.resources.provider.goal_course_title
import com.grippo.design.resources.provider.goal_details_analysis_evidence
import com.grippo.design.resources.provider.goal_details_analysis_period
import com.grippo.design.resources.provider.metric_overview_goal_analyzed
import com.grippo.design.resources.provider.metric_overview_goal_score
import com.grippo.design.resources.provider.percent

@Composable
internal fun GoalCardAssessment(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
) {
    val score = value.score?.coerceIn(0, 100)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            GoalMark(goal = value.goal.primaryGoal)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.goal_course_title),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Text(
                    text = value.goal.primaryGoal.label(),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = AppTokens.strings.res(Res.string.metric_overview_goal_score),
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = score?.let { "$it${AppTokens.strings.res(Res.string.percent)}" } ?: "—",
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Box(
                    modifier = Modifier
                        .size(AppTokens.dp.metrics.profile.goal.statusMarker)
                        .clip(CircleShape)
                        .background(value.statusColor()),
                )
                Text(
                    text = value.statusLabel(),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            if (score != null &&
                (value.evidence.excludedSetCount > 0 || value.evidence.missingMetadataSetCount > 0)
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.goal_card_limited_data),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        GoalAnalysisSummary(value = value)
    }
}

@Composable
private fun GoalAnalysisSummary(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
) {
    val range = value.evidence.range
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        if (range.value != null) {
            Text(
                text = AppTokens.strings.res(Res.string.goal_details_analysis_period, range.shortStartEnd()),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        Text(
            text = AppTokens.strings.res(Res.string.metric_overview_goal_analyzed, value.sessionCount),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
        Text(
            text = AppTokens.strings.res(
                Res.string.goal_details_analysis_evidence,
                value.evidence.days,
                value.evidence.setCount,
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardAssessmentPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(),
            style = GoalCardStyle.Assessment,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardAssessmentNotEvaluatedPreview() {
    val base = stubGoalProgress(score = null)
    PreviewContainer {
        GoalCard(
            value = base.copy(
                sessionCount = 0,
                evidence = base.evidence.copy(setCount = 0),
            ),
            style = GoalCardStyle.Assessment,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardAssessmentPartialEvidencePreview() {
    val base = stubGoalProgress(score = 50)
    PreviewContainer {
        GoalCard(
            value = base.copy(
                evidence = base.evidence.copy(excludedSetCount = 3, missingMetadataSetCount = 8),
            ),
            style = GoalCardStyle.Assessment,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardAssessmentDriftingPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(score = 54),
            style = GoalCardStyle.Assessment,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardAssessmentOffTrackPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(score = 28),
            style = GoalCardStyle.Assessment,
        )
    }
}
