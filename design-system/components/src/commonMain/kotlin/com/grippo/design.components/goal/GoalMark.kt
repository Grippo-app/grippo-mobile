package com.grippo.design.components.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Compass

@Composable
public fun GoalMark(
    modifier: Modifier = Modifier,
) {
    GoalMarkContent(
        modifier = modifier,
        icon = AppTokens.icons.Compass,
    )
}

@Composable
public fun GoalMark(
    modifier: Modifier = Modifier,
    goal: GoalPrimaryGoalEnumState?,
) {
    GoalMarkContent(
        modifier = modifier,
        icon = goal?.icon() ?: AppTokens.icons.Compass,
    )
}

@Composable
private fun GoalMarkContent(
    modifier: Modifier = Modifier,
    icon: ImageVector,
) {
    val shape = RoundedCornerShape(AppTokens.dp.goal.radius)
    Box(
        modifier = modifier
            .size(AppTokens.dp.goal.markSize)
            .clip(shape)
            .background(AppTokens.colors.goal.background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.goal.iconSize),
            imageVector = icon,
            tint = AppTokens.colors.goal.accent,
            contentDescription = null,
        )
    }
}

@AppPreview
@Composable
private fun GoalMarkPrimaryGoalsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            GoalPrimaryGoalEnumState.entries.forEach { goal ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    GoalMark(goal = goal)
                    Text(
                        text = goal.label(),
                        style = AppTokens.typography.b14Semi(),
                        color = AppTokens.colors.text.primary,
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun GoalMarkUnspecifiedPreview() {
    PreviewContainer {
        GoalMark()
    }
}
