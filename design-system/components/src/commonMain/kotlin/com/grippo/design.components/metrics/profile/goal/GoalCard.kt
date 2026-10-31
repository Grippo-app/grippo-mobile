package com.grippo.design.components.metrics.profile.goal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateRangeFormatState
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardAssessment
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardCalendar
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardGrowth
import com.grippo.design.components.metrics.profile.goal.internal.GoalDeadline
import com.grippo.design.components.metrics.profile.goal.internal.GoalIdentity
import com.grippo.design.components.metrics.profile.goal.internal.GoalStatusLabel
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_limited_data
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.goal_details_analysis_period
import com.grippo.design.resources.provider.home_goal_details
import com.grippo.design.resources.provider.home_goal_less_details
import com.grippo.design.resources.provider.home_goal_open_details
import com.grippo.design.resources.provider.metric_overview_goal_score
import com.grippo.design.resources.provider.percent
import com.grippo.toolkit.date.utils.DateRangeKind

@Stable
public sealed interface GoalCardStyle {
    @Stable
    public data class Summary(
        public val range: DateRangeFormatState,
        public val onDetailsClick: () -> Unit,
        public val onUpdateClick: () -> Unit,
    ) : GoalCardStyle
    @Stable
    public data class Overview(
        public val onUpdateClick: () -> Unit,
    ) : GoalCardStyle

    @Stable
    public data object Assessment : GoalCardStyle

    @Stable
    public data class Calendar(
        public val onUpdateClick: () -> Unit,
    ) : GoalCardStyle

    @Immutable
    public data class Growth(
        public val previous: GoalProgressState?,
    ) : GoalCardStyle
}

@Composable
public fun GoalCard(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    style: GoalCardStyle,
) {
    when (style) {
        is GoalCardStyle.Summary -> {
            var expanded by rememberSaveable(value.goal, style.range) { mutableStateOf(false) }
            GoalSummary(
                modifier = modifier,
                value = value,
                style = style,
                expanded = expanded,
                onToggle = { expanded = !expanded },
            )
        }

        is GoalCardStyle.Overview -> MetricSectionPanel(
            modifier = modifier,
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = style.onUpdateClick,
                )
            },
        )

        GoalCardStyle.Assessment -> GoalCardAssessment(
            modifier = modifier,
            value = value,
        )

        is GoalCardStyle.Calendar -> GoalCardCalendar(
            modifier = modifier,
            value = value,
            onUpdateClick = style.onUpdateClick,
        )

        is GoalCardStyle.Growth -> GoalCardGrowth(
            modifier = modifier,
            value = value,
            previous = style.previous,
        )
    }
}

@Composable
private fun GoalSummary(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    style: GoalCardStyle.Summary,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val partial = value.evidence.excludedSetCount > 0 || value.evidence.missingMetadataSetCount > 0
    MetricSectionPanel(modifier = modifier, style = MetricSectionPanelStyle.Large) {
        GoalIdentity(modifier = Modifier.scalableClick(onClick = style.onDetailsClick), value = value)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                Text(
                    text = value.score?.let { "$it${AppTokens.strings.res(Res.string.percent)}" } ?: "—",
                    style = AppTokens.typography.h3(),
                    color = AppTokens.colors.text.primary,
                )
                Text(
                    text = AppTokens.strings.res(Res.string.metric_overview_goal_score),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
                GoalStatusLabel(value = value)
                Text(
                    text = AppTokens.strings.res(Res.string.goal_details_analysis_period, style.range.shortStartEnd()),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
        if (partial) {
            Text(
                text = AppTokens.strings.res(Res.string.goal_card_limited_data),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }
        if (value.daysRemaining < 0) {
            Text(
                text = value.remainingLine(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.semantic.warning,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
            Button(
                modifier = Modifier.weight(1f),
                content = ButtonContent.Text(text = AppTokens.strings.res(
                    if (expanded) Res.string.home_goal_less_details else Res.string.home_goal_details,
                )),
                size = ButtonSize.Small,
                style = ButtonStyle.Secondary,
                onClick = onToggle,
            )
            if (value.daysRemaining < 0) {
                Button(
                    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.goal_card_update_btn)),
                    size = ButtonSize.Small,
                    style = ButtonStyle.Secondary,
                    onClick = style.onUpdateClick,
                )
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)) {
                if (value.daysRemaining < 0) {
                    GoalDeadline(value = value)
                } else {
                    GoalCardCalendar(value = value, onUpdateClick = style.onUpdateClick)
                }
                Text(
                    text = value.analyzedWorkoutsLabel(),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
                Button(
                    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.home_goal_open_details)),
                    size = ButtonSize.Small,
                    style = ButtonStyle.Secondary,
                    onClick = style.onDetailsClick,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun GoalCardSummaryExpandedPreview() {
    PreviewContainer {
        GoalSummary(
            value = stubGoalProgressList().first(),
            style = goalSummaryPreviewStyle(),
            expanded = true,
            onToggle = {},
        )
    }
}

@AppPreview
@Composable
private fun GoalCardSummaryExpandedOverduePreview() {
    PreviewContainer {
        GoalSummary(
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
            style = goalSummaryPreviewStyle(),
            expanded = true,
            onToggle = {},
        )
    }
}

private fun goalSummaryPreviewStyle(): GoalCardStyle.Summary = GoalCardStyle.Summary(
    range = DateRangeFormatState.of(DateRangeKind.Last30Days),
    onDetailsClick = {},
    onUpdateClick = {},
)

@AppPreview
@Composable
private fun GoalCardSummaryPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first(),
            style = goalSummaryPreviewStyle(),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardSummaryOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
            style = goalSummaryPreviewStyle(),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardSummaryPartialEvidencePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { it.daysRemaining > 0 }
        GoalCard(
            value = value.copy(evidence = value.evidence.copy(missingMetadataSetCount = 8)),
            style = goalSummaryPreviewStyle(),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardSummaryNoScorePreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining > 0 }
        GoalCard(
            value = value.copy(
                score = null,
                sessionCount = 0,
                evidence = value.evidence.copy(setCount = 0),
                findings = kotlinx.collections.immutable.persistentListOf(),
            ),
            style = goalSummaryPreviewStyle(),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { !it.isFinished && it.daysRemaining > 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewDueTodayPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining == 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
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
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOffTrackPreview() {
    PreviewContainer {
        val value = stubGoalProgressList().first {
            it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true
        }
        GoalCard(
            value = value.copy(
                goal = value.goal.copy(primaryGoal = GoalPrimaryGoalEnumState.RETURN_TO_TRAINING),
            ),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.daysRemaining < 0 },
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardOverviewCompletedPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().first { it.isFinished }.copy(score = 100),
            style = GoalCardStyle.Overview(onUpdateClick = {}),
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
            style = GoalCardStyle.Overview(onUpdateClick = {}),
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
            style = GoalCardStyle.Overview(onUpdateClick = {}),
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
            style = GoalCardStyle.Overview(onUpdateClick = {}),
        )
    }
}
