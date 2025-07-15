package com.grippo.wheel.picker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.wheel.picker.internal.SingleWheelColumn

@Immutable
public sealed interface IWheelColumn {
    public val id: String
    public val labelContent: (@Composable () -> Unit)?
    @Composable
    public fun Content(modifier: Modifier, rowCount: Int)
}

@Immutable
public data class WheelColumn<T : Comparable<T>>(
    override val id: String,
    val items: List<T>,
    val initial: T,
    val onValueChange: (T) -> Unit,
    val itemContent: @Composable (T) -> Unit,
    override val labelContent: (@Composable () -> Unit)? = null,
) : IWheelColumn {
    @Composable
    override fun Content(modifier: Modifier, rowCount: Int) {
        SingleWheelColumn(
            modifier = modifier,
            items = items,
            initial = initial,
            rowCount = rowCount,
            onValueChange = onValueChange,
            itemContent = itemContent,
            labelContent = labelContent
        )
    }
}