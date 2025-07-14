package com.grippo.wheel.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grippo.wheel.picker.internal.MultiWheelPicker

@Composable
public fun <T> WheelPicker(
    modifier: Modifier = Modifier,
    initial: T,
    items: List<T>,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    onValueChange: (T) -> Unit,
    descriptionContent: (@Composable () -> Unit)? = null,
    content: @Composable (item: T) -> Unit,
) {
    val initialIndex = remember(items) {
        items.indexOf(initial).takeIf { it >= 0 } ?: 0
    }

    MultiWheelPicker(
        modifier = modifier,
        rowCount = rowCount,
        selectorProperties = selectorProperties,
        columns = listOf(
            WheelColumn(
                id = "single",
                items = items,
                initialIndex = initialIndex,
                onValueChange = onValueChange,
                itemContent = content,
                descriptionContent = descriptionContent
            )
        )
    )
}