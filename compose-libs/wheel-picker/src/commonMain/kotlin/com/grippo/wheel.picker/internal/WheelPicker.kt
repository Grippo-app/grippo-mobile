package com.grippo.wheel.picker.internal

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.grippo.wheel.picker.DefaultSelectorProperties
import com.grippo.wheel.picker.SelectorProperties
import kotlin.math.abs

@Composable
internal fun WheelPicker(
    modifier: Modifier = Modifier,
    count: Int,
    rowCount: Int,
    size: DpSize,
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    content: @Composable LazyItemScope.(index: Int) -> Unit,
    listState: LazyListState,
) {
    val flingBehavior = rememberSnapFlingBehavior(listState)

    // Precompute snapshot-derived values once per recomposition
    val layoutInfo = remember(listState, rowCount) { derivedStateOf { listState.layoutInfo } }
    val viewportHeight = layoutInfo.value.viewportSize.height.toFloat()
    val singleViewportHeight = viewportHeight / rowCount
    val centerIndexState = remember(listState) { derivedStateOf { listState.firstVisibleItemIndex } }
    val centerOffsetState = remember(listState) { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    LazyColumn(
        modifier = modifier
            .height(size.height)
            .width(size.width),
        state = listState,
        contentPadding = PaddingValues(vertical = size.height / rowCount * ((rowCount - 1) / 2)),
        flingBehavior = flingBehavior
    ) {

        items(count) { index ->
            val (alpha, rotX) = calculateAnimatedAlphaAndRotationX(
                rowCount = rowCount,
                index = index,
                singleViewportHeight = singleViewportHeight,
                centerIndex = centerIndexState.value,
                centerIndexOffset = centerOffsetState.value
            )
            Box(
                modifier = Modifier
                    .height(size.height / rowCount)
                    .fillMaxWidth()
                    .alpha(alpha)
                    .graphicsLayer { rotationX = rotX },
                contentAlignment = Alignment.Center,
                content = { content(index) }
            )
        }
    }
}

@Composable
private fun calculateAnimatedAlphaAndRotationX(
    rowCount: Int,
    index: Int,
    singleViewportHeight: Float,
    centerIndex: Int,
    centerIndexOffset: Int
): Pair<Float, Float> {
    val distanceToCenterIndex = index - centerIndex
    val distanceToIndexSnap = distanceToCenterIndex * singleViewportHeight.toInt() - centerIndexOffset
    val distanceToIndexSnapAbs = abs(distanceToIndexSnap)

    val animatedAlpha = if (abs(distanceToIndexSnap) in 0..singleViewportHeight.toInt()) {
        1.2f - (distanceToIndexSnapAbs / singleViewportHeight)
    } else {
        0.2f
    }

    val animatedRotationX = (-20 * (distanceToIndexSnap / singleViewportHeight)).takeUnless { it.isNaN() } ?: 0f

    return animatedAlpha to animatedRotationX
}

internal object WheelPickerDefaults {
    @Composable
    fun selectorProperties(
        shape: Shape = RoundedCornerShape(16.dp),
        color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    ): SelectorProperties = DefaultSelectorProperties(
        shape = shape,
        color = color,
    )
}
