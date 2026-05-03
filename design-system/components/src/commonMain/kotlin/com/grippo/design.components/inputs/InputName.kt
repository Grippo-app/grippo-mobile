package com.grippo.design.components.inputs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputTrailingIconButton
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.name_placeholder

@Composable
public fun InputName(
    modifier: Modifier = Modifier,
    value: NameFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.name_placeholder),
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value.display,
        maxLines = 1,
        minLines = 1,
        error = value.toInputError(),
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        trailing = { color ->
            InputTrailingIconButton(
                visible = value.display.isNotEmpty(),
                icon = AppTokens.icons.Cancel,
                tint = color,
                onClick = { onValueChange("") },
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus(force = true)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        )
    )
}

@AppPreview
@Composable
private fun InputNamePreview() {
    PreviewContainer {
        InputName(
            value = NameFormatState.of("Mark B."),
            onValueChange = {}
        )

        InputName(
            value = NameFormatState.Empty(),
            onValueChange = {}
        )
    }
}
