package com.grippo.design.components.metrics.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.stubGoalProgressList
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.metrics.goal.internal.GoalCardContent
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun GoalEmptyCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
        overlay = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(AppTokens.colors.overlay.overlay),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    content = ButtonContent.Text(
                        text = "Add goal",
                    ),
                    size = ButtonSize.Small,
                    style = ButtonStyle.Primary,
                    onClick = onClick
                )
            }
        },
        content = {
            GoalCardContent(value = stubGoalProgressList().random())
        }
    )
}

@AppPreview
@Composable
private fun GoalEmptyCardPreview() {
    PreviewContainer {
        GoalEmptyCard(
            onClick = {}
        )
        GoalEmptyCard(
            onClick = {}
        )
        GoalEmptyCard(
            onClick = {}
        )
    }
}
