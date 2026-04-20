package com.grippo.design.components.metrics.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.metrics.GoalProgressState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalState
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
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.persistentListOf

@Composable
public fun GoalEmptyCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val placeholder = remember {
        val now = DateFormatState.of(
            value = DateTimeUtils.now(),
            range = DateTimeUtils.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy
        )
        GoalProgressState(
            goal = GoalState(
                primaryGoal = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
                secondaryGoal = null,
                target = now,
                createdAt = now,
                personalizations = persistentListOf(),
            ),
            now = now,
            score = 78,
            strengthShare = 45,
            hypertrophyShare = 40,
            enduranceShare = 15,
            daysTotal = 14,
            daysElapsed = 3,
            daysRemaining = 11,
            progressFraction = 3f / 14f,
            isFinished = false,
        )
    }

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
            GoalCardContent(
                value = placeholder
            )
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
