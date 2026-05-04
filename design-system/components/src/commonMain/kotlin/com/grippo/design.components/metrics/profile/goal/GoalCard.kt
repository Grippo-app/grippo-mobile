package com.grippo.design.components.metrics.profile.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.GoalProgressState
import com.grippo.core.state.metrics.profile.stubGoalProgressList
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.components.metrics.profile.goal.internal.GoalCardContent
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun GoalCard(
    modifier: Modifier = Modifier,
    value: GoalProgressState,
    onUpdateClick: () -> Unit
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
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
