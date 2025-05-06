package com.grippo.wheel.picker

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.wheel.picker.internal.WheelPicker

@Composable
public fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    items: List<T>,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    content: @Composable LazyItemScope.(item: T) -> Unit,
) {
    WheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        count = items.size,
        rowCount = rowCount,
        selectorProperties = selectorProperties,
        onScrollFinished = onScrollFinished,
        content = { index -> content.invoke(this, items[index]) }
    )
}