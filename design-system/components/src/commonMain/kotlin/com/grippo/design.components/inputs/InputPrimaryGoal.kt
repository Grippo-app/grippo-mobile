package com.grippo.design.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_title

@Composable
public fun InputPrimaryGoal(
    modifier: Modifier = Modifier,
    value: GoalPrimaryGoalEnumState?,
    placeholder: String = AppTokens.strings.res(Res.string.goal_title),
    onClick: () -> Unit,
) {
    Input(
        modifier = modifier,
        value = value?.label() ?: "",
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Clickable(
            onClick = onClick
        ),
        trailing = { _ -> },
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputPrimaryGoalPreview() {
    PreviewContainer {
        InputPrimaryGoal(
            value = GoalPrimaryGoalEnumState.BUILD_MUSCLE,
            onClick = {}
        )

        InputPrimaryGoal(
            value = null,
            onClick = {}
        )
    }
}
