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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.chart.internal.RingChart
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_progress_day
import com.grippo.design.resources.provider.goal_card_title
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.percent

@Composable
internal fun GoalCardContent(
    value: GoalProgressState,
    onUpdateClick: () -> Unit
) {
    val score = value.score.coerceIn(0, 100)
    val ringColors = goalRingColors(score = score, isFinished = value.isFinished)
    goalStatusColor(score = score, isFinished = value.isFinished)

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
            isFinished = value.isFinished,
            statusText = value.statusLabel()
        )

        AdherenceRing(
            modifier = Modifier.size(AppTokens.dp.metrics.profile.goal.chart),
            score = score,
            ringColors = ringColors,
        )
    }

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
    ) {
        LineIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTokens.dp.contentPadding.text),
            progress = value.progressFraction,
            colors = AppTokens.colors.charts.indicator.muted,
            barHeight = 4.dp,
            labelSpacing = AppTokens.dp.contentPadding.text,
            startLabel = {
                Text(
                    text = value.goal.createdAt.display,
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            endLabel = {
                Text(
                    text = value.goal.target.display,
                    style = AppTokens.typography.b11Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                )
            },
            marker = {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AppTokens.colors.text.primary),
                )
            },
        )

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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    text = progressLineHighlighted(value),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                style = SpanStyle(
                    color = AppTokens.colors.text.primary,
                    fontWeight = FontWeight.SemiBold,
                ),
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
    score: Int,
    isFinished: Boolean
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = title,
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        val statusColor = goalStatusColor(score = score, isFinished = isFinished)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )

            Text(
                text = statusText,
                style = AppTokens.typography.b11Semi(),
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AdherenceRing(
    modifier: Modifier = Modifier,
    score: Int,
    ringColors: AppColor.Charts.RingColor.RingPalette,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        RingChart(
            modifier = Modifier.matchParentSize(),
            value = score.toFloat(),
            max = 100f,
            colors = ringColors,
        )

        val percent = AppTokens.strings.res(Res.string.percent)

        Text(
            text = buildAnnotatedString {
                withStyle(
                    AppTokens.typography.h1().toSpanStyle()
                        .copy(color = AppTokens.colors.text.primary)
                ) { append(score.toString()) }
                withStyle(
                    AppTokens.typography.h4().toSpanStyle()
                        .copy(color = AppTokens.colors.text.secondary)
                ) { append(percent) }
            },
            style = AppTokens.typography.h1(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardContentOnTrackPreview() {
    PreviewContainer {
        val value =
            stubGoalProgressList().first { !it.isFinished && it.score >= GoalProgressState.ON_TRACK_MIN }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
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
        val value = stubGoalProgressList().first {
            !it.isFinished &&
                    it.score in GoalProgressState.DRIFTING_MIN until GoalProgressState.ON_TRACK_MIN
        }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
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
        val value = stubGoalProgressList().first { it.score < GoalProgressState.DRIFTING_MIN }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
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
        val value = stubGoalProgressList().first { it.daysRemaining < 0 }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
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
        val value = stubGoalProgressList().first { it.isFinished }
        MetricSectionPanel(
            style = MetricSectionPanelStyle.Small,
            content = {
                GoalCardContent(
                    value = value,
                    onUpdateClick = {}
                )
            }
        )
    }
}
