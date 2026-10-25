package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.goal.GoalMark
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_limited_data
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.goal_course_title
import com.grippo.design.resources.provider.goal_period_label
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.metric_overview_goal_analyzed
import com.grippo.design.resources.provider.metric_overview_goal_score
import com.grippo.design.resources.provider.percent

@Composable
internal fun GoalCardContent(
    value: GoalProgressState,
    onUpdateClick: () -> Unit,
) {
    GoalIdentity(value = value)

    ContentDivider()

    GoalTrainingAlignment(value = value)

    ContentDivider()

    GoalCalendarPeriod(
        value = value,
        onUpdateClick = onUpdateClick,
    )
}

@Composable
internal fun GoalIdentity(
    value: GoalProgressState,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
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
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary,
            )
        }

        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
            imageVector = AppTokens.icons.ArrowRight,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

@Composable
private fun GoalTrainingAlignment(
    value: GoalProgressState,
) {
    val score = value.score?.coerceIn(0, 100)
    val hasPartialEvidence = value.evidence.excludedSetCount > 0 ||
            value.evidence.missingMetadataSetCount > 0
    val percent = AppTokens.strings.res(Res.string.percent)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildAnnotatedString {
                    append(score?.toString() ?: "—")
                    if (score != null) {
                        withStyle(
                            AppTokens.typography.b14Med().toSpanStyle()
                                .copy(color = AppTokens.colors.text.secondary),
                        ) {
                            append(percent)
                        }
                    }
                },
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = AppTokens.strings.res(Res.string.metric_overview_goal_score),
                    style = AppTokens.typography.b13Semi(),
                    color = AppTokens.colors.text.primary,
                )
                GoalStatusLabel(
                    value = value,
                )
            }
        }

        Text(
            text = AppTokens.strings.res(
                Res.string.metric_overview_goal_analyzed,
                value.sessionCount,
            ),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        if (score != null && hasPartialEvidence) {
            Text(
                text = AppTokens.strings.res(Res.string.goal_card_limited_data),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun GoalStatusLabel(
    value: GoalProgressState,
) {
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
}

@Composable
private fun GoalCalendarPeriod(
    value: GoalProgressState,
    onUpdateClick: () -> Unit,
) {
    val targetText = value.goal.target.short()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
                imageVector = AppTokens.icons.Calendar,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.goal_period_label),
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.secondary,
            )
            Text(
                modifier = Modifier.weight(1f),
                text = targetText,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.End,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isOverdue = value.daysRemaining < 0
            Text(
                modifier = Modifier.weight(1f),
                text = value.remainingLine(),
                style = AppTokens.typography.b13Med(),
                color = if (isOverdue) {
                    AppTokens.colors.semantic.warning
                } else {
                    AppTokens.colors.text.secondary
                },
            )

            if (isOverdue) {
                Button(
                    size = ButtonSize.Small,
                    style = ButtonStyle.Secondary,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.goal_card_update_btn),
                    ),
                    onClick = onUpdateClick,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun GoalCardContentOnTrackPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first {
                !it.isFinished && it.score?.let { score -> score >= GoalProgressState.ON_TRACK_MIN } == true
            },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentDriftingPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first {
                !it.isFinished &&
                        it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
            },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOffTrackPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first {
                it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true
            },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentNoScorePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first()
        GoalCard(
            value = value.copy(score = null),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentPartialEvidencePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first()
        GoalCard(
            value = value.copy(evidence = value.evidence.copy(excludedSetCount = 3)),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}
