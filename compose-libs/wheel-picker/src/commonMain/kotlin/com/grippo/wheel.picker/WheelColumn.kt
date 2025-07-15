package com.grippo.wheel.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.grippo.wheel.picker.internal.SingleWheelColumn

@Immutable
public data class WheelColumn<T : Comparable<T>>(
    override val id: String,
    val items: List<T>,
    val initial: T,
    val onValueChange: (T) -> Unit,
    val itemContent: @Composable (T) -> Unit,
) : IWheelColumn {
    @Composable
    override fun Content(modifier: Modifier, itemHeight: Dp, rowCount: Int) {
        SingleWheelColumn(
            modifier = modifier,
            items = items,
            initial = initial,
            rowCount = rowCount,
            itemHeight = itemHeight,
            onValueChange = onValueChange,
            itemContent = itemContent,
        )
    }
}

@Immutable
public sealed interface IWheelColumn {
    public val id: String

    @Composable
    public fun Content(modifier: Modifier, itemHeight: Dp, rowCount: Int)
}