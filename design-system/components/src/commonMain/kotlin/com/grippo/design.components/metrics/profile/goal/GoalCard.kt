package com.grippo.design.components.metrics.profile.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.components.metrics.profile.goal.internal.goalStatusColor
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun GoalCard(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    onUpdateClick: () -> Unit
) {
    val score = value.score.coerceIn(0, 100)
    val statusColor = goalStatusColor(score = score, isFinished = value.isFinished)
    val panelShape = RoundedCornerShape(AppTokens.dp.metrics.panel.small.radius)

    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
        decorator = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(statusColor.copy(alpha = 0.09f), shape = panelShape)
            )
        },
        content = {
            GoalCardContent(
                value = value,
                onUpdateClick = onUpdateClick
            )
        }
    )
}

@AppPreview
@Composable
private fun GoalCardPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().random(),
            onUpdateClick = {}
        )
        GoalCard(
            value = stubGoalProgressList().random(),
            onUpdateClick = {}
        )
        GoalCard(
            value = stubGoalProgressList().random(),
            onUpdateClick = {}
        )
    }
}
