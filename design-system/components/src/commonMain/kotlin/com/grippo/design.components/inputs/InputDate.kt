package com.grippo.design.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.select_date
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun InputDate(
    modifier: Modifier = Modifier,
    value: DateFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.select_date),
    onClick: () -> Unit,
) {
    Input(
        modifier = modifier,
        value = value.display,
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Clickable(
            onClick = onClick
        ),
        error = value.toInputError(),
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputDatePreview() {
    PreviewContainer {
        InputDate(
            value = DateFormatState.of(
                value = DateTimeUtils.now(),
                range = DateRangePresets.yearly(),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            onClick = {}
        )

        InputDate(
            value = DateFormatState.Empty(format = DateFormat.DateOnly.DateMmmDdYyyy),
            onClick = {}
        )
    }
}
