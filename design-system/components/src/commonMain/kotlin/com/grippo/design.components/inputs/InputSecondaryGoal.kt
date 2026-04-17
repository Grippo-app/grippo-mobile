package com.grippo.design.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.goal_secondary_section

@Composable
public fun InputSecondaryGoal(
    modifier: Modifier = Modifier,
    value: GoalSecondaryGoalEnumState?,
    placeholder: String = AppTokens.strings.res(Res.string.goal_secondary_section),
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
private fun InputSecondaryGoalPreview() {
    PreviewContainer {
        InputSecondaryGoal(
            value = GoalSecondaryGoalEnumState.GET_STRONGER,
            onClick = {}
        )

        InputSecondaryGoal(
            value = null,
            onClick = {}
        )
    }
}
