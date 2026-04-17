package com.grippo.design.components.inputs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
    val focusManager = LocalFocusManager.current

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
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
        )
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
