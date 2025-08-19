package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.weight_placeholder

@Composable
public fun InputWeight(
    modifier: Modifier = Modifier,
    value: Float,
    onClick: () -> Unit
) {

    val internalValue = remember(value) {
        value.toString()
    }

    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = internalValue,
        inputStyle = InputStyle.Clickable(
            onClick = onClick
        ),
        trailing = { color ->
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.b14Semi(),
                color = color
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.weight_placeholder)
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
private fun InputWeightPreview() {
    PreviewContainer {
        InputWeight(
            value = 12.5F,
            onClick = {}
        )

        InputWeight(
            value = 123.2F,
            onClick = {}
        )
    }
}