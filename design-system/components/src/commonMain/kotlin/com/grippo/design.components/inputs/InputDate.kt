package com.grippo.design.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputPlaceholder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.select_date
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

@Composable
public fun InputDate(
    modifier: Modifier = Modifier,
    value: DateTimeFormatState,
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
        placeholder = InputPlaceholder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputDatePreview() {
    PreviewContainer {
        InputDate(
            value = DateTimeFormatState.of(
                value = LocalDateTime(2026, 1, 15, 12, 0),
                range = DateRange(
                    from = LocalDateTime(2026, 1, 1, 0, 0),
                    to = LocalDateTime(2026, 12, 31, 23, 59),
                ),
                format = DateFormat.DateOnly.DateMmmDdYyyy,
            ),
            onClick = {}
        )

        InputDate(
            value = DateTimeFormatState.Empty(format = DateFormat.DateOnly.DateMmmDdYyyy),
            onClick = {}
        )
    }
}
