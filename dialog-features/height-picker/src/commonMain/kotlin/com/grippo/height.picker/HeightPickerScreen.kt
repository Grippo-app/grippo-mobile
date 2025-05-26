package com.grippo.height.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.BaseComposeScreen
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.height_picker_description
import com.grippo.design.resources.height_picker_title
import com.grippo.design.resources.submit_btn
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.WheelPicker
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun HeightPickerScreen(
    state: HeightPickerState,
    loaders: ImmutableSet<HeightPickerLoader>,
    contract: HeightPickerContract
) = BaseComposeScreen {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTokens.dp.paddings.screenHorizontal,
                vertical = AppTokens.dp.paddings.screenVertical
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.height_picker_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.height_picker_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(26.dp))

        WheelPicker(
            modifier = Modifier.fillMaxWidth()
                .height(AppTokens.dp.size.componentHeight * 3),
            items = state.suggestions,
            initial = state.initial,
            onValueChange = contract::select,
            rowCount = 3,
            selectorProperties = DefaultSelectorProperties(
                enabled = true,
                shape = RoundedCornerShape(AppTokens.dp.shape.large),
                color = AppTokens.colors.background.primary,
                border = BorderStroke(1.dp, AppTokens.colors.border.defaultPrimary)
            ),
            content = {
                Text(
                    modifier = Modifier,
                    text = it.toString(),
                    style = AppTokens.typography.b16Bold()
                )
            }
        )

        Spacer(modifier = Modifier.size(26.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.submit_btn),
            style = ButtonStyle.Primary,
            onClick = contract::submit
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        HeightPickerScreen(
            state = HeightPickerState(
                initial = 155
            ),
            contract = HeightPickerContract.Empty,
            loaders = persistentSetOf()
        )
    }
}