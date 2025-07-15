package com.grippo.wheel.picker

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

@Composable
public fun MultiWheelPicker(
    modifier: Modifier = Modifier,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    columns: List<IWheelColumn>,
    spacing: Dp = 8.dp,
) {
    BoxWithConstraints(modifier = modifier) {
        val itemHeight = maxHeight / rowCount
        val background = selectorProperties.color().value
        val shape = selectorProperties.shape().value
        val border = selectorProperties.border().value
        val enabled = selectorProperties.enabled().value

        if (enabled) Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(background, shape)
                .then(border?.let { Modifier.border(it, shape) } ?: Modifier)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                columns.forEach { column ->
                    key(column.id) {
                        column.Content(
                            modifier = modifier.weight(1f),
                            itemHeight = itemHeight,
                            rowCount = rowCount
                        )
                    }
                }
            }
        )
    }
}