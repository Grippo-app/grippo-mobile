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
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputError
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.volume_placeholder

@Composable
public fun InputVolume(
    modifier: Modifier = Modifier,
    value: VolumeFormatState,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value.display,
        maxLines = 1,
        minLines = 1,
        error = when (value) {
            is VolumeFormatState.Empty -> InputError.Non
            is VolumeFormatState.Invalid -> InputError.Error("")
            is VolumeFormatState.Valid -> InputError.Non
        },
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        trailing = { color ->
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.b15Med(),
                color = color
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.volume_placeholder)
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
private fun InputVolumePreview() {
    PreviewContainer {
        InputVolume(
            value = VolumeFormatState.of("12"),
            placeholder = AppTokens.strings.res(Res.string.volume_placeholder),
            onValueChange = {}
        )

        InputVolume(
            value = VolumeFormatState.of("1234"),
            placeholder = AppTokens.strings.res(Res.string.volume_placeholder),
            onValueChange = {}
        )
    }
}