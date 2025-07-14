package com.grippo.wheel.picker.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.wheel.picker.SelectorProperties
import com.grippo.wheel.picker.WheelColumn

@Composable
internal fun <T> MultiWheelPicker(
    modifier: Modifier = Modifier,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    columns: List<WheelColumn<T>>,
    spacing: Dp = 8.dp,
) {
    BoxWithConstraints(modifier = modifier) {
        val itemHeight = maxHeight / rowCount

        if (selectorProperties.enabled().value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .background(selectorProperties.color().value, selectorProperties.shape().value)
                    .then(
                        selectorProperties.border().value?.let {
                            Modifier.border(it, selectorProperties.shape().value)
                        } ?: Modifier
                    )
            )
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { column ->
                key(column.id) {
                    SingleWheelColumn(
                        modifier = Modifier.weight(1f),
                        items = column.items,
                        initialIndex = column.initialIndex,
                        rowCount = rowCount,
                        onValueChange = column.onValueChange,
                        itemContent = column.itemContent,
                        descriptionContent = column.descriptionContent
                    )
                }
            }
        }
    }
}