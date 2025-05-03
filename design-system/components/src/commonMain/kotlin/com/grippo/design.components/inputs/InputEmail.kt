package com.grippo.design.components.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.grippo.design.components.internal.Input
import com.grippo.design.components.internal.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.email_placeholder
import com.grippo.design.resources.icons.X

@Composable
public fun InputEmail(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit
) {

    val focusManager = LocalFocusManager.current

    Input(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        trailing = { color ->
            Box {
                AnimatedVisibility(
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(40.dp)
                        .clickable { onValueChange.invoke("") },
                    visible = value.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier
                                .size(AppTokens.dp.icon.component),
                            imageVector = AppTokens.icons.X,
                            tint = color,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        placeholder = PlaceHolder.OverInput(
            value = AppTokens.strings.res(Res.string.email_placeholder)
        ),
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Email
        )
    )
}