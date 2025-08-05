package com.grippo.wheel.picker

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.grippo.wheel.picker.internal.WheelPicker
import com.grippo.wheel.picker.internal.WheelPickerDefaults

@Immutable
public data class WheelColumn<T : Comparable<T>>(
    override val id: String,
    val items: List<T>,
    val initial: T,
    val onValueChange: (T) -> Unit,
    val itemContent: @Composable (T) -> Unit,
) : IWheelColumn {

    @Composable
    override fun Content(modifier: Modifier, height: Dp, rowCount: Int) {
        val initialIndex = remember(items, initial) {
            items.indexOf(initial).coerceAtLeast(0)
        }

        WheelPicker(
            modifier = modifier.height(height * rowCount),
            startIndex = initialIndex,
            count = items.size,
            rowCount = rowCount,
            size = DpSize(
                width = Dp.Unspecified,
                height = height * rowCount
            ),
            selectorProperties = WheelPickerDefaults.selectorProperties(),
            onScrollFinished = { index ->
                items.getOrNull(index)?.let { onValueChange(it) }
                null
            },
            content = { index ->
                itemContent(items[index])
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