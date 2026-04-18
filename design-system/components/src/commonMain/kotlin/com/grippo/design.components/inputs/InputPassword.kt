package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.grippo.core.state.formatters.PasswordFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputTrailingIconButton
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.EyeOff
import com.grippo.design.resources.provider.icons.EyeOn
import com.grippo.design.resources.provider.icons.Lock
import com.grippo.design.resources.provider.password_placeholder_default

@Composable
public fun InputPassword(
    modifier: Modifier = Modifier,
    value: PasswordFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.password_placeholder_default),
    onValueChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Input(
        modifier = modifier,
        value = value.display,
        enabled = true,
        maxLines = 1,
        minLines = 1,
        error = value.toInputError(),
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus(force = true)
        },
        leading = { color ->
            Icon(
                modifier = Modifier.size(AppTokens.dp.input.icon),
                imageVector = AppTokens.icons.Lock,
                tint = color,
                contentDescription = null
            )
        },
        trailing = { color ->
            if (value is PasswordFormatState.Empty) return@Input
            Box {
                InputTrailingIconButton(
                    visible = !passwordVisible,
                    icon = AppTokens.icons.EyeOn,
                    tint = color,
                    onClick = { passwordVisible = true },
                )
                InputTrailingIconButton(
                    visible = passwordVisible,
                    icon = AppTokens.icons.EyeOff,
                    tint = color,
                    onClick = { passwordVisible = false },
                )
            }
        },
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputPasswordPreview() {
    PreviewContainer {
        InputPassword(
            value = PasswordFormatState.of("qwerty123"),
            onValueChange = {}
        )

        InputPassword(
            value = PasswordFormatState.Empty(),
            onValueChange = {}
        )
    }
}
