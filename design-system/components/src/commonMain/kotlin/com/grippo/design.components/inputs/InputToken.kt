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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.modifiers.scalableClick
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
            Box {
                AnimatedVisibility(
                    modifier = Modifier
                        .size(40.dp)
                        .scalableClick { /*copy to clipboard*/ },
                    visible = value.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(AppTokens.dp.input.icon),
                            imageVector = AppTokens.icons.Mail,
                            tint = color,
                            contentDescription = null
                        )
                    }
                }
            }
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