package com.grippo.wheel.picker.internal

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue

@Composable
internal fun <T> SingleWheelColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    initialIndex: Int,
    rowCount: Int,
    onValueChange: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    descriptionContent: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState(initialIndex)
    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }

    val centerIndex by remember {
        derivedStateOf { layoutInfo.closestItemToCenter()?.index ?: initialIndex }
    }

    LaunchedEffect(centerIndex) {
        items.getOrNull(centerIndex)?.let { onValueChange(it) }
    }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val itemHeight = maxHeight / rowCount

        // Measure width of description
        val descriptionWidthPx = remember { mutableStateOf(0) }
        val density = LocalDensity.current

        if (descriptionContent != null) {
            SubcomposeLayout { constraints ->
                val placeables = subcompose("desc", descriptionContent).map {
                    it.measure(constraints)
                }
                descriptionWidthPx.value = placeables.maxOfOrNull { it.width } ?: 0
                layout(0, 0) {}
            }
        }

        val offset = with(density) { descriptionWidthPx.value.toDp() + 4.dp }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .offset(x = -offset),
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeight * ((rowCount - 1) / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            flingBehavior = rememberSnapFlingBehavior(listState)
        ) {
            itemsIndexed(items) { index, item ->
                val alpha = calculateAnimatedAlpha(layoutInfo, index, rowCount)
                val rotationX = calculateAnimatedRotationX(layoutInfo, index, rowCount)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .alpha(alpha)
                        .graphicsLayer { this.rotationX = rotationX },
                    contentAlignment = Alignment.Center
                ) {
                    itemContent(item)
                }
            }
        }

        // Description rendered next to center item
        descriptionContent?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(start = 4.dp),
                content = { it() }
            )
        }
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

private fun LazyListLayoutInfo.closestItemToCenter(): LazyListItemInfo? {
    val center = viewportStartOffset + viewportSize.height / 2
    return visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        (itemCenter - center).absoluteValue
    }
}