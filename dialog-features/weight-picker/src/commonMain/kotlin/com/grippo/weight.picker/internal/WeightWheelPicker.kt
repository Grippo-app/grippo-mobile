package com.grippo.weight.picker.internal

import androidx.compose.foundation.BorderStroke
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
import com.grippo.wheel.picker.WheelPicker
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun WeightWheelPicker(
    modifier: Modifier = Modifier,
    suggestions: PersistentList<Float>,
    value: Float,
    select: (Float) -> Unit
) {
    WheelPicker(
        modifier = modifier.height(AppTokens.dp.wheelPicker.height),
        items = suggestions,
        initial = value,
        onValueChange = select,
        rowCount = 3,
        selectorProperties = DefaultSelectorProperties(
            enabled = true,
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
            color = AppTokens.colors.background.primary,
            border = BorderStroke(1.dp, AppTokens.colors.border.defaultPrimary)
        ),
        content = {
            Text(
                text = it.toString(),
                style = AppTokens.typography.b16Bold()
            )
        }
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