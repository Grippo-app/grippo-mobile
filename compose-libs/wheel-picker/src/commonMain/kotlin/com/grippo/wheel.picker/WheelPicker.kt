package com.grippo.wheel.picker

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.wheel.picker.internal.WheelPicker

@Composable
public fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    initial: T,
    items: List<T>,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    onValueChange: (T) -> Unit,
    content: @Composable LazyItemScope.(item: T) -> Unit,
) {

    val initialIndex = remember(items) {
        items.indexOf(initial).takeIf { it >= 0 } ?: 0
    }

    WheelPicker(
        modifier = modifier,
        startIndex = initialIndex,
        count = items.size,
        rowCount = rowCount,
        selectorProperties = selectorProperties,
        onScrollFinished = { snappedIndex ->
            onValueChange(items[snappedIndex])
            null // no forced scroll
        },
        content = { index ->
            content.invoke(this, items[index])
        }
    )
}