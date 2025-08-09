package com.grippo.design.components.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.grippo.design.components.internal.Input
import com.grippo.design.components.internal.InputStyle
import com.grippo.design.components.internal.PlaceHolder
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.EyeEmpty
import com.grippo.design.resources.icons.EyeOff
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.password_placeholder

@Composable
public fun InputPassword(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value,
        enabled = true,
        maxLines = 1,
        minLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus(force = true)
        },
        trailing = { color ->
            if (value.isEmpty()) return@Input
            Box {
                AnimatedVisibility(
                    modifier = Modifier
                        .size(40.dp)
                        .scalableClick { passwordVisible = true },
                    visible = !passwordVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                    content = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(AppTokens.dp.input.icon),
                                imageVector = AppTokens.icons.EyeOff,
                                tint = color,
                                contentDescription = null,
                            )
                        }
                    },
                )

                AnimatedVisibility(
                    modifier = Modifier
                        .size(40.dp)
                        .scalableClick { passwordVisible = false },
                    visible = passwordVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                    content = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modifier = Modifier.size(AppTokens.dp.input.icon),
                                imageVector = AppTokens.icons.EyeEmpty,
                                tint = color,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
        },
        inputStyle = InputStyle.Default(
            onValueChange = onValueChange,
        ),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.password_placeholder)
        ),
    )
}

@AppPreview
@Composable
private fun InputPasswordPreview() {
    PreviewContainer {
        InputPassword(
            value = "qwerty123",
            onValueChange = {}
        )

        InputPassword(
            value = "",
            onValueChange = {}
        )
    }
}