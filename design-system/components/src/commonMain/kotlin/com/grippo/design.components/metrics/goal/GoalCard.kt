package com.grippo.design.components.metrics.goal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.core.state.metrics.stubGoalProgressList
import com.grippo.design.components.metrics.goal.internal.GoalCardContent
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun GoalCard(
    value: GoalProgressState,
    modifier: Modifier = Modifier,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
        content = { GoalCardContent(value = value) }
    )
}

@AppPreview
@Composable
private fun GoalCardPreview() {
    PreviewContainer {
        GoalCard(
            value = stubGoalProgressList().random()
        )
        GoalCard(
            value = stubGoalProgressList().random()
        )
        GoalCard(
            value = stubGoalProgressList().random()
        )
    }
}
