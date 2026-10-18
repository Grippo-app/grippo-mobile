package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.MetricCardPresentation
import com.grippo.design.components.metrics.internal.MetricOverviewHeader
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.goalCardPreviewValues
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.available_training_details_notice
import com.grippo.design.resources.provider.goal_card_progress_day
import com.grippo.design.resources.provider.goal_card_limited_data
import com.grippo.design.resources.provider.goal_card_title
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.metric_overview_goal_analyzed
import com.grippo.design.resources.provider.metric_overview_goal_score
import com.grippo.design.resources.provider.metric_overview_goal_target
import com.grippo.design.resources.provider.percent
import com.grippo.toolkit.date.utils.DateCompose

@Composable
internal fun GoalCardContent(
    value: GoalProgressState,
    onUpdateClick: () -> Unit,
    presentation: MetricCardPresentation = MetricCardPresentation.Detailed,
) {
    val score = value.score?.coerceIn(0, 100)
    val ringColors = goalRingColors(score = score)

    if (presentation == MetricCardPresentation.Overview) {
        MetricOverviewHeader(
            title = AppTokens.strings.res(Res.string.goal_card_title),
        )

        GoalOverviewSummary(
            value = value,
            score = score,
            ringColors = ringColors,
        )
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(intrinsicSize = IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoalTitleBlock(
                modifier = Modifier.weight(1f)
                    .fillMaxHeight(),
                title = value.goal.primaryGoal.label(),
                score = score,
                statusText = value.statusLabel(),
                hasPartialEvidence = value.evidence.excludedSetCount > 0 || value.evidence.missingMetadataSetCount > 0,
            )

            Column(
                modifier = Modifier.width(AppTokens.dp.metrics.profile.goal.chart),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                AdherenceRing(
                    modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.chart),
                    score = score,
                    ringColors = ringColors,
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.metric_overview_goal_score),
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    Text(
        text = AppTokens.strings.res(Res.string.available_training_details_notice),
        style = AppTokens.typography.b12Med(),
        color = AppTokens.colors.text.secondary,
    )

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        if (presentation == MetricCardPresentation.Detailed) {
            val createdAt = value.goal.createdAt
            val createdAtValue = createdAt.value
            val createdAtText = if (createdAtValue != null) {
                DateCompose.rememberFormat(value = createdAtValue, format = createdAt.format)
            } else {
                createdAt.display
            }
            val target = value.goal.target
            val targetValue = target.value
            val targetText = if (targetValue != null) {
                DateCompose.rememberFormat(value = targetValue, format = target.format)
            } else {
                target.display
            }

            LineIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTokens.dp.contentPadding.text),
                progress = value.progressFraction,
                colors = AppTokens.colors.charts.indicator.muted,
                labelSpacing = AppTokens.dp.contentPadding.text,
                startLabel = {
                    Text(
                        text = createdAtText,
                        style = AppTokens.typography.b11Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 2,
                    )
                },
                endLabel = {
                    Text(
                        text = targetText,
                        style = AppTokens.typography.b11Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 2,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                },
                marker = {
                    Box(
                        modifier = Modifier
                            .size(AppTokens.dp.metrics.profile.goal.timelineMarker)
                            .clip(CircleShape)
                            .background(AppTokens.colors.text.primary),
                    )
                },
            )
        }

        val isOverdue = value.daysRemaining < 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOverdue) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = value.remainingLine(),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.semantic.warning,
                )

                Button(
                    size = ButtonSize.Small,
                    style = ButtonStyle.Secondary,
                    content = ButtonContent.Text(
                        text = AppTokens.strings.res(Res.string.goal_card_update_btn)
                    ),
                    onClick = onUpdateClick
                )
            } else {
                Text(
                    modifier = Modifier.weight(1f),
                    text = if (presentation == MetricCardPresentation.Overview) {
                        AnnotatedString(value.remainingLine())
                    } else {
                        progressLineHighlighted(value)
                    },
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun GoalOverviewSummary(
    value: GoalProgressState,
    score: Int?,
    ringColors: AppColor.Charts.RingColor.RingPalette,
) {
    val target = value.goal.target
    val targetValue = target.value
    val targetText = if (targetValue != null) {
        DateCompose.rememberFormat(value = targetValue, format = target.format)
    } else {
        target.display
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                text = value.goal.primaryGoal.label(),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )

            GoalStatusLabel(
                statusText = value.statusLabel(),
                score = score,
                hasPartialEvidence = value.evidence.excludedSetCount > 0 || value.evidence.missingMetadataSetCount > 0,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = AppTokens.strings.res(
                        Res.string.metric_overview_goal_analyzed,
                        value.sessionCount,
                    ),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )

                Text(
                    text = AppTokens.strings.res(
                        Res.string.metric_overview_goal_target,
                        targetText,
                    ),
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }

        Column(
            modifier = Modifier.width(AppTokens.dp.metrics.profile.goal.chart),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            AdherenceRing(
                modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.chart),
                score = score,
                ringColors = ringColors,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.metric_overview_goal_score),
                style = AppTokens.typography.b11Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun progressLineHighlighted(value: GoalProgressState): AnnotatedString {
    val day = (value.daysElapsed + 1).coerceAtMost(value.daysTotal)
    val total = value.daysTotal
    val formatted = AppTokens.strings.res(Res.string.goal_card_progress_day, day, total)
    val dayText = day.toString()
    val highlightStart = formatted.indexOf(dayText)
    return buildAnnotatedString {
        append(formatted)
        if (highlightStart >= 0) {
            addStyle(
                style = AppTokens.typography.b13Semi().toSpanStyle()
                    .copy(color = AppTokens.colors.text.primary),
                start = highlightStart,
                end = highlightStart + dayText.length,
            )
        }
    }
}

@Composable
private fun GoalTitleBlock(
    modifier: Modifier = Modifier,
    title: String,
    statusText: String,
    score: Int?,
    hasPartialEvidence: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.goal_card_title),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.primary,
        )

        GoalStatusLabel(
            statusText = statusText,
            score = score,
            hasPartialEvidence = hasPartialEvidence,
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun GoalStatusLabel(
    statusText: String,
    score: Int?,
    hasPartialEvidence: Boolean,
) {
    val statusColor = goalStatusColor(score = score)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Box(
            modifier = Modifier
                .size(AppTokens.dp.metrics.profile.goal.statusMarker)
                .clip(CircleShape)
                .background(statusColor),
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            Text(
                text = statusText,
                style = AppTokens.typography.b11Semi(),
                color = AppTokens.colors.text.secondary,
            )
            if (score != null && hasPartialEvidence) {
                Text(
                    text = AppTokens.strings.res(Res.string.goal_card_limited_data),
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun AdherenceRing(
    modifier: Modifier = Modifier,
    score: Int?,
    ringColors: AppColor.Charts.RingColor.RingPalette,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = score?.toFloat() ?: 0f,
            max = 100f,
            colors = ringColors,
        )

        val percent = AppTokens.strings.res(Res.string.percent)

        Text(
            text = buildAnnotatedString {
                withStyle(
                    AppTokens.typography.h4().toSpanStyle()
                        .copy(color = AppTokens.colors.text.primary)
                ) { append(score?.toString() ?: "—") }
                withStyle(
                    AppTokens.typography.b14Med().toSpanStyle()
                        .copy(color = AppTokens.colors.text.secondary)
                ) { if (score != null) append(percent) }
            },
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOnTrackPreview() {
    PreviewContainer {
        val value =
            goalCardPreviewValues().first { !it.isFinished && it.score?.let { score -> score >= GoalProgressState.ON_TRACK_MIN } == true }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentDriftingPreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first {
            !it.isFinished &&
                    it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
        }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOffTrackPreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first { it.score?.let { score -> score < GoalProgressState.DRIFTING_MIN } == true }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOverduePreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first { it.daysRemaining < 0 }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentCompletedPreview() {
    PreviewContainer {
        val value = goalCardPreviewValues().first { it.isFinished }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Large,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}
