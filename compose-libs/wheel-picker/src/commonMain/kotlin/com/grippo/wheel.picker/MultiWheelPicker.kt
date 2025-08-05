package com.grippo.wheel.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Immutable
public data class MultiWheelPickerConfig(
    val rowCount: Int = 3,
)

@Composable
public fun MultiWheelPicker(
    modifier: Modifier = Modifier,
    selectorProperties: SelectorProperties,
    config: MultiWheelPickerConfig = MultiWheelPickerConfig(),
    columns: List<IWheelColumn>,
) {
    BoxWithConstraints(modifier = modifier) {
        val itemHeight = maxHeight / config.rowCount

        val background = selectorProperties.color().value
        val shape = selectorProperties.shape().value

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(background, shape)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEach { column ->
                key(column.id) {
                    column.Content(
                        modifier = Modifier.weight(1f),
                        height = itemHeight,
                        rowCount = config.rowCount
                    )
                }
            }
        }
    }
}