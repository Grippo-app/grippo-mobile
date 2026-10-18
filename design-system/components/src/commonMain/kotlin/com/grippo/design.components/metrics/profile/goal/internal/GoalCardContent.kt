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
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.available_training_details_notice
import com.grippo.design.resources.provider.goal_card_limited_data
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.goal_course_title
import com.grippo.design.resources.provider.goal_period_label
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.metric_overview_goal_analyzed
import com.grippo.design.resources.provider.metric_overview_goal_score
import com.grippo.design.resources.provider.percent
import com.grippo.toolkit.date.utils.DateCompose

@Composable
internal fun GoalCardContent(
    value: GoalProgressState,
    onUpdateClick: () -> Unit,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    GoalIdentity(
        value = value,
        showDetails = presentation == MetricCardPresentation.Overview,
    )

    ContentDivider()

    GoalTrainingAlignment(value = value)

    ContentDivider()

    GoalCalendarPeriod(
        value = value,
        presentation = presentation,
        onUpdateClick = onUpdateClick,
    )

    Text(
        text = AppTokens.strings.res(Res.string.available_training_details_notice),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )
}

@Composable
private fun GoalIdentity(
    value: GoalProgressState,
    showDetails: Boolean,
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

        if (showDetails) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
                imageVector = AppTokens.icons.ArrowRight,
                tint = AppTokens.colors.icon.secondary,
                contentDescription = null,
            )
        }
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
    presentation: MetricCardPresentation,
    onUpdateClick: () -> Unit,
) {
    val target = value.goal.target
    val targetValue = target.value
    val targetText = if (targetValue != null) {
        DateCompose.rememberFormat(value = targetValue, format = target.format)
    } else {
        target.display
    }

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
            if (presentation == MetricCardPresentation.Overview) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = targetText,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.End,
                )
            }
        }

        if (presentation == MetricCardPresentation.Detailed) {
            val createdAt = value.goal.createdAt
            val createdAtValue = createdAt.value
            val createdAtText = if (createdAtValue != null) {
                DateCompose.rememberFormat(value = createdAtValue, format = createdAt.format)
            } else {
                createdAt.display
            }

            LineIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = value.progressFraction,
                colors = AppTokens.colors.charts.indicator.muted,
                labelSpacing = AppTokens.dp.contentPadding.subContent,
                startLabel = {
                    Text(
                        text = createdAtText,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                },
                endLabel = {
                    Text(
                        text = targetText,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        textAlign = TextAlign.End,
                    )
                },
                marker = {
                    Box(
                        modifier = Modifier
                            .size(AppTokens.dp.metrics.profile.goal.timelineMarker)
                            .clip(CircleShape)
                            .background(AppTokens.colors.goal.accent),
                    )
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isOverdue = value.daysRemaining < 0
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = value.remainingLine(),
                    style = AppTokens.typography.b13Med(),
                    color = if (isOverdue) {
                        AppTokens.colors.semantic.warning
                    } else {
                        AppTokens.colors.text.secondary
                    },
                )
                if (presentation == MetricCardPresentation.Detailed && !isOverdue) {
                    Text(
                        text = value.progressLine(),
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }

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
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            GoalCardContent(
                value = stubGoalProgressList().first {
                    !it.isFinished && it.score?.let { score -> score >= GoalProgressState.ON_TRACK_MIN } == true
                },
                onUpdateClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun GoalCardContentDriftingPreview() {
    PreviewContainer {
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            GoalCardContent(
                value = stubGoalProgressList().first {
                    !it.isFinished &&
                            it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
                },
                onUpdateClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun GoalCardContentOffTrackPreview() {
    PreviewContainer {
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            GoalCardContent(
                value = stubGoalProgressList().first {
                    it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true
                },
                onUpdateClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun GoalCardContentNoScorePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first()
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            GoalCardContent(
                value = value.copy(score = null),
                onUpdateClick = {},
            )
        }
    }
}

@AppPreview
@Composable
private fun GoalCardContentPartialEvidencePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first()
        MetricSectionPanel(style = MetricSectionPanelStyle.Large) {
            GoalCardContent(
                value = value.copy(evidence = value.evidence.copy(excludedSetCount = 3)),
                onUpdateClick = {},
            )
        }
    }
}
