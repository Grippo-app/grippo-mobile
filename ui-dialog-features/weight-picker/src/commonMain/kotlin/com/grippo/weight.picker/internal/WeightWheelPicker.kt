package com.grippo.weight.picker.internal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.wheel.WheelItem
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.kg
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
                subText = AppTokens.strings.res(Res.string.kg),
                isValid = true
            )
        },
        listState = weightListState
    )

    MultiWheelPicker(
        modifier = modifier
            .fillMaxWidth()
            .height(AppTokens.dp.wheelPicker.height),
        selectorProperties = DefaultSelectorProperties(
            shape = RoundedCornerShape(AppTokens.dp.wheelPicker.radius),
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
