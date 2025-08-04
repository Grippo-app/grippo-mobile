package com.grippo.weight.picker.internal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun WeightWheelPicker(
    modifier: Modifier = Modifier,
    suggestions: PersistentList<Float>,
    value: Float,
    select: (Float) -> Unit
) {
    MultiWheelPicker(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            enabled = true,
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.primary,
            border = BorderStroke(1.dp, AppTokens.colors.border.primary)
        ),
        columns = listOf(
            WheelColumn(
                id = "weight",
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
private fun WeightWheelPickerPreview() {
    PreviewContainer {
        WeightWheelPicker(
            suggestions = buildList(capacity = 1201) {
                for (i in 301..1500) {
                    add(i / 10f)
                }
            }.toPersistentList(),
            value = 403f,
            select = {},
        )
    }
}