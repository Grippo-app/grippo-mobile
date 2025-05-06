package com.grippo.wheel.picker.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.grippo.wheel.picker.SelectorProperties
import kotlin.math.absoluteValue

// https://github.com/commandiron/WheelPickerCompose
@Composable
internal fun WheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    selectorProperties: SelectorProperties,
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState(startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    val layoutInfo by remember { derivedStateOf { lazyListState.layoutInfo } }
    val isScrollInProgress = lazyListState.isScrollInProgress

    val shape = selectorProperties.shape().value
    val border = selectorProperties.border().value
    val enabled = selectorProperties.enabled().value
    val color = selectorProperties.color().value

    LaunchedEffect(isScrollInProgress, layoutInfo.totalItemsCount) {
        if (!isScrollInProgress) {
            val centerIndex = layoutInfo.closestItemToCenter()?.index ?: return@LaunchedEffect
            onScrollFinished(centerIndex)?.let { lazyListState.scrollToItem(it) }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val itemHeight = maxHeight / rowCount

        if (enabled) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        this.shape = shape
                        this.clip = true
                    }
                    .background(color)
                    .let { border?.let { border -> it.border(border, shape) } ?: it }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxHeight),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = itemHeight * ((rowCount - 1) / 2)),
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count) { index ->
                val alpha = calculateAnimatedAlpha(layoutInfo, index, rowCount)
                val rotationX = calculateAnimatedRotationX(layoutInfo, index, rowCount)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(alpha)
                        .graphicsLayer { this.rotationX = rotationX },
                    contentAlignment = Alignment.Center,
                    content = { content(index) }
                )
            }
        }
    }
}

private fun LazyListLayoutInfo.closestItemToCenter(): LazyListItemInfo? {
    val center = viewportStartOffset + viewportSize.height / 2
    return visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        (itemCenter - center).absoluteValue
    }
}

private fun calculateAnimatedAlpha(
    layoutInfo: LazyListLayoutInfo,
    index: Int,
    rowCount: Int
): Float {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return 0.2f
    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    val itemCenter = item.offset + item.size / 2
    val distance = (itemCenter - center).absoluteValue
    val maxDistance = layoutInfo.viewportSize.height.toFloat() / rowCount

    return if (distance <= maxDistance) {
        lerp(0.2f, 1.0f, 1f - (distance / maxDistance))
    } else 0.2f
}

private fun calculateAnimatedRotationX(
    layoutInfo: LazyListLayoutInfo,
    index: Int,
    rowCount: Int
): Float {
    val item = layoutInfo.visibleItemsInfo.find { it.index == index } ?: return 0f
    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    val itemCenter = item.offset + item.size / 2
    val distance = itemCenter - center
    val maxDistance = layoutInfo.viewportSize.height.toFloat() / rowCount

    return (-20f * (distance / maxDistance)).coerceIn(-90f, 90f)
}