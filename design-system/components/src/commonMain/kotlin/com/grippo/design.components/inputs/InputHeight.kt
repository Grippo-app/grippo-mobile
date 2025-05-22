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
import com.grippo.design.components.internal.Input
import com.grippo.design.components.internal.InputStyle
import com.grippo.design.components.internal.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.cm
import com.grippo.design.resources.height_placeholder

@Composable
public fun InputHeight(
    modifier: Modifier = Modifier,
    value: Int,
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
                modifier = Modifier.padding(end = 4.dp),
                text = AppTokens.strings.res(Res.string.cm),
                style = AppTokens.typography.b14Semi(),
                color = color
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.height_placeholder)
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
private fun InputHeightPreview() {
    PreviewContainer {
        InputHeight(
            value = 12,
            onClick = {}
        )

        InputHeight(
            value = 123,
            onClick = {}
        )
    }
}