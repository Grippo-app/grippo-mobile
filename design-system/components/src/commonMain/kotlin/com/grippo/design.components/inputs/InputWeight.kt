package com.grippo.design.components.inputs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.design.components.inputs.core.Input
import com.grippo.design.components.inputs.core.InputStyle
import com.grippo.design.components.inputs.core.PlaceHolder
import com.grippo.design.components.inputs.core.toInputError
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.weight_placeholder

@Composable
public fun InputWeight(
    modifier: Modifier = Modifier,
    value: WeightFormatState,
    placeholder: String = AppTokens.strings.res(Res.string.weight_placeholder),
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
        trailing = { color ->
            Text(
                modifier = Modifier.padding(end = 8.dp),
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.b15Med(),
                color = color
            )
        },
        placeholder = PlaceHolder.OverInput(
            value = placeholder
        ),
    )
}

@AppPreview
@Composable
private fun InputWeightPreview() {
    PreviewContainer {
        InputWeight(
            value = WeightFormatState.of(70.5f),
            onClick = {}
        )

        InputWeight(
            value = WeightFormatState.of(85.0f),
            onClick = {}
        )
    }
}
