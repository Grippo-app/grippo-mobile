package com.grippo.design.components.inputs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration

@Composable
public fun InputDuration(
    modifier: Modifier = Modifier,
    value: DurationFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.duration),
    onClick: () -> Unit
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
private fun InputDurationPreview() {
    PreviewContainer {
        InputDuration(
            value = DurationFormatState.of("2h"),
            onClick = {}
        )
    }
}
