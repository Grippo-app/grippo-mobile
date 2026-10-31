package com.grippo.design.components.metrics.profile.goal.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgress
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.metrics.profile.goal.GoalCard
import com.grippo.design.components.metrics.profile.goal.GoalCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_card_update_btn
import com.grippo.design.resources.provider.goal_period_label
import com.grippo.design.resources.provider.icons.Calendar

@Composable
internal fun GoalCardCalendar(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    onUpdateClick: () -> Unit,
) {
    val isOverdue = value.daysRemaining < 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        GoalDeadline(value = value)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.remainingLine(),
                style = AppTokens.typography.b12Med(),
                color = if (isOverdue) AppTokens.colors.semantic.warning else AppTokens.colors.text.secondary,
            )
            if (isOverdue) {
                Button(
                    size = ButtonSize.Small,
                    style = ButtonStyle.Secondary,
                    content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.goal_card_update_btn)),
                    onClick = onUpdateClick,
                )
            }
        }
    }
}

@Composable
internal fun GoalDeadline(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
) {
    val targetText = value.goal.target.short()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.overview.detailsIcon),
            imageVector = AppTokens.icons.Calendar,
            tint = AppTokens.colors.icon.secondary,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = "${AppTokens.strings.res(Res.string.goal_period_label)} · $targetText",
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun GoalCardCalendarPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(),
            style = GoalCardStyle.Calendar(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardCalendarOverduePreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(daysTotal = 30, daysElapsed = 34),
            style = GoalCardStyle.Calendar(onUpdateClick = {}),
        )
    }
}

@AppPreview
@Composable
private fun GoalCardCalendarDueTodayPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgress(daysTotal = 30, daysElapsed = 29),
            style = GoalCardStyle.Calendar(onUpdateClick = {}),
        )
    }
}
