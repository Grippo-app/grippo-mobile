package com.grippo.design.components.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.grippo.design.components.internal.Input
import com.grippo.design.components.internal.PlaceHolder

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
        enabled = enabled,
        maxLines = 1,
        minLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Text,
            capitalization = KeyboardCapitalization.Sentences,
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus(force = true)
        },
        trailing = { color ->
            Box {
                AnimatedVisibility(
                    modifier = Modifier.clickable { passwordVisible = true },
                    visible = !passwordVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                    content = {
                        Icon(
                            imageVector = AppTokens.icons.CrossedEye,
                            tint = color,
                            contentDescription = null,
                        )
                    },
                )

                AnimatedVisibility(
                    modifier = Modifier.clickable { passwordVisible = false },
                    visible = passwordVisible,
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                    content = {
                        Icon(
                            imageVector = AppTokens.icons.Eye,
                            tint = color,
                            contentDescription = null,
                        )
                    },
                )
            }
        },
        onValueChange = onValueChange,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        placeholder = PlaceHolder.OverInput(value = placeholder),
    )
}