package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.InputPlaceholder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cm
import com.grippo.design.resources.provider.height_placeholder

@Composable
public fun InputHeight(
    modifier: Modifier = Modifier,
    value: HeightFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.height_placeholder),
    onClick: () -> Unit
) {
    val unitLabel = AppTokens.strings.res(Res.string.cm)
    val valueDescription = if (value.display.isBlank()) unitLabel else "${value.display} $unitLabel"

    Input(
        modifier = modifier,
        value = value.display,
        valueDescription = valueDescription,
        maxLines = 1,
        minLines = 1,
        inputStyle = InputStyle.Clickable(
            onClick = onClick
        ),
        error = value.toInputError(),
        trailing = { color ->
            Text(
                modifier = Modifier.padding(end = AppTokens.dp.input.unitEndPadding),
                text = unitLabel,
                style = AppTokens.typography.b15Med(),
                color = color
            )
        },
        placeholder = InputPlaceholder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputHeightPreview() {
    PreviewContainer {
        InputHeight(
            value = HeightFormatState.of(120),
            onClick = {}
        )

        InputHeight(
            value = HeightFormatState.of(175),
            onClick = {}
        )
    }
}
