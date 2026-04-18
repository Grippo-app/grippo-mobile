package com.grippo.design.components.inputs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputTrailingIconButton
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.utils.rememberClipboardCopyAction
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Mail
import com.grippo.design.resources.provider.push_token

@Composable
public fun InputToken(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String = AppTokens.strings.res(Res.string.push_token),
    onValueChange: (String) -> Unit
) {
    val copyToClipboard = rememberClipboardCopyAction()
    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value,
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        trailing = { color ->
            val clickProvider = remember(value) {
                { copyToClipboard(value) }
            }

            InputTrailingIconButton(
                visible = value.isNotEmpty(),
                icon = AppTokens.icons.Mail,
                tint = color,
                onClick = clickProvider,
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
private fun InputTokenPreview() {
    PreviewContainer {
        InputToken(
            value = "FCM-123456...",
            onValueChange = {}
        )

        InputToken(
            value = "",
            onValueChange = {}
        )
    }
}
