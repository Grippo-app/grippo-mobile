package com.grippo.wheel.picker

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.grippo.wheel.picker.internal.BindWheel
import com.grippo.wheel.picker.internal.WheelPicker
import com.grippo.wheel.picker.internal.WheelPickerDefaults

@Immutable
public data class WheelColumn<T : Comparable<T>>(
    override val id: String,
    val items: List<T>,
    val selected: T,
    val onSelect: (T) -> Unit,
    val isValid: (T) -> Boolean,
    val itemContent: @Composable (T, Boolean) -> Unit,
    val listState: LazyListState
) : IWheelColumn {

    @Composable
    override fun Content(modifier: Modifier, height: Dp, rowCount: Int) {
        BindWheel(
            listState = listState,
            items = items,
            selected = selected,
            onSelect = onSelect,
            isValid = isValid
        )

        WheelPicker(
            modifier = modifier.height(height * rowCount),
            listState = listState,
            count = items.size,
            rowCount = rowCount,
            size = DpSize(width = Dp.Unspecified, height = height * rowCount),
            selectorProperties = WheelPickerDefaults.selectorProperties(),
            content = { index ->
                val item = items[index]
                itemContent(item, isValid(item))
            }
        )
    }
}

@Immutable
public sealed interface IWheelColumn {
    public val id: String

    @Composable
    public fun Content(modifier: Modifier, height: Dp, rowCount: Int)
}