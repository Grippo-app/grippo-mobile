package com.grippo.height.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun HeightWheelPicker(
    modifier: Modifier = Modifier,
    suggestions: PersistentList<Int>,
    value: Int,
    select: (Int) -> Unit
) {
    MultiWheelPicker(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(
            WheelColumn(
                id = "height",
                initial = value,
                onValueChange = select,
                items = suggestions,
                itemContent = {
                    Text(
                        text = it.toString(),
                        style = AppTokens.typography.b16Bold(),
                        color = AppTokens.colors.text.primary
                    )
                }
            )
        )
    )
}

@AppPreview
@Composable
private fun HeightWheelPickerPreview() {
    PreviewContainer {
        HeightWheelPicker(
            suggestions = (100..250).toPersistentList(),
            value = 150,
            select = {},
        )
    }
}