package com.grippo.weight.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.wheel.WheelItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.weight.picker.DefaultWeightSuggestions
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.MultiWheelPicker
import com.grippo.wheel.picker.WheelColumn
import kotlinx.collections.immutable.PersistentList

@Composable
internal fun WeightWheelPicker(
    modifier: Modifier = Modifier,
    suggestions: PersistentList<Float>,
    value: Float?,
    select: (Float) -> Unit
) {
    val weightListState = rememberLazyListState()

    val weightColumn = WheelColumn(
        id = "weight",
        items = suggestions,
        selected = value,
        onSelect = select,
        isValid = { true },
        itemContent = { item, _ ->
            WheelItem(
                text = item.toString(),
                isValid = true
            )
        },
        listState = weightListState
    )

    MultiWheelPicker(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = CircleShape,
            color = AppTokens.colors.background.card,
        ),
        columns = listOf(weightColumn)
    )
}

@AppPreview
@Composable
private fun WeightWheelPickerPreview() {
    PreviewContainer {
        WeightWheelPicker(
            suggestions = DefaultWeightSuggestions,
            value = 403f,
            select = {},
        )
    }
}
