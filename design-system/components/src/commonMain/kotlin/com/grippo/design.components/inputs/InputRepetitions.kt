package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputError
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.repetitions_placeholder
import com.grippo.design.resources.provider.reps

@Composable
public fun InputRepetitions(
    modifier: Modifier = Modifier,
    value: RepetitionsFormatState,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value.display,
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        error = when (value) {
            is RepetitionsFormatState.Empty -> InputError.Non
            is RepetitionsFormatState.Invalid -> InputError.Error("")
            is RepetitionsFormatState.Valid -> InputError.Non
        },
        trailing = { color ->
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = AppTokens.strings.res(Res.string.reps),
                style = AppTokens.typography.b15Med(),
                color = color
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.repetitions_placeholder)
        ),
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number
        )
    )
}

@AppPreview
@Composable
private fun InputRepetitionsPreview() {
    PreviewContainer {
        InputRepetitions(
            value = RepetitionsFormatState.of("12"),
            onValueChange = {}
        )

        InputRepetitions(
            value = RepetitionsFormatState.of("123"),
            onValueChange = {}
        )
    }
}