package com.grippo.design.components.inputs

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.select_date
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun InputDate(
    modifier: Modifier = Modifier,
    value: DateFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.select_date),
    onClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    val formatted = remember(value.value) {
        val v = value.value ?: return@remember "-"
        DateTimeUtils.format(v, DateFormat.DateOnly.DateMmmDdYyyy)
    }

    Input(
        modifier = modifier,
        value = formatted,
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Clickable(
            onClick = onClick
        ),
        trailing = { _ -> },
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
        keyboardActions = KeyboardActions {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
        )
    )
}

@AppPreview
@Composable
private fun InputDatePreview() {
    PreviewContainer {
        InputDate(
            value = DateFormatState.of(
                value = DateTimeUtils.now(),
                range = DateRange.Range.Yearly().range,
            ),
            onClick = {}
        )

        InputDate(
            value = DateFormatState.Empty(),
            onClick = {}
        )
    }
}
