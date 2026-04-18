package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.grippo.core.state.formatters.EmailFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputTrailingIconButton
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.email_placeholder
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Mail

@Composable
public fun InputEmail(
    modifier: Modifier = Modifier,
    value: EmailFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.email_placeholder),
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
        leading = { color ->
            Icon(
                modifier = Modifier.size(AppTokens.dp.input.icon),
                imageVector = AppTokens.icons.Mail,
                tint = color,
                contentDescription = null
            )
        },
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
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
    )
}

@AppPreview
@Composable
private fun InputEmailPreview() {
    PreviewContainer {
        InputEmail(
            value = EmailFormatState.of("user@mail.com"),
            onValueChange = {}
        )

        InputEmail(
            value = EmailFormatState.Empty(),
            onValueChange = {}
        )
    }
}
