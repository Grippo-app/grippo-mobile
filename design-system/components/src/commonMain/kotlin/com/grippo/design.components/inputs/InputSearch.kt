package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputTrailingIconButton
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.Search
import com.grippo.design.resources.provider.search_placeholder

@Composable
public fun InputSearch(
    modifier: Modifier = Modifier,
    value: String,
    placeholder: String = AppTokens.strings.res(Res.string.search_placeholder),
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
        leading = { color ->
            Icon(
                modifier = Modifier.size(AppTokens.dp.input.icon),
                imageVector = AppTokens.icons.Search,
                tint = color,
                contentDescription = null
            )
        },
        trailing = { color ->
            InputTrailingIconButton(
                visible = value.isNotEmpty(),
                icon = AppTokens.icons.Cancel,
                tint = color,
                onClick = { onValueChange("") },
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
        keyboardActions = KeyboardActions {
            focusManager.clearFocus(force = true)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        )
    )
}

@AppPreview
@Composable
private fun InputNamePreview() {
    PreviewContainer {
        InputSearch(
            value = "Search something",
            onValueChange = {}
        )

        InputSearch(
            value = "",
            onValueChange = {}
        )
    }
}
