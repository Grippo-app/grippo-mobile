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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
public data class MultiWheelPickerConfig(
    val spacing: Dp = 8.dp,
    val rowCount: Int = 3,
    val snapEnabled: Boolean = false,
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
        val border = selectorProperties.border().value
        val enabled = selectorProperties.enabled().value
        val wheelConfig = WheelConfig(
            rowCount = config.rowCount,
            itemHeight = itemHeight,
            snapEnabled = config.snapEnabled
        )

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
            horizontalArrangement = Arrangement.spacedBy(config.spacing),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                columns.forEach { column ->
                    key(column.id) {
                        column.Content(
                            modifier = modifier.weight(1f),
                            config = wheelConfig,
                        )
                    }
                }
            }
        )
    }
}