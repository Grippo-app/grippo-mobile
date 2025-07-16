package com.grippo.wheel.picker.internal

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.grippo.wheel.picker.WheelConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun <T> SingleWheelColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    initial: T,
    config: WheelConfig,
    onValueChange: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    val initialIndex = remember(items, initial) {
        items.indexOf(initial).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialIndex)

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.closestItemToCenter()?.index
                ?: listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collectLatest { index ->
                delay(100)
                items.getOrNull(index)?.let(onValueChange)
            }
    }

    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    val center = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
    val maxDistance = layoutInfo.viewportSize.height.toFloat() / config.rowCount

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(vertical = config.itemHeight * ((config.rowCount - 1) / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = rememberSnapFlingBehavior(listState)
    ) {
        itemsIndexed(items, key = { index, item -> item.hashCode() }) { index, item ->
            val alpha = calculateAnimatedAlpha(visibleItems, index, center, maxDistance)
            val rotationX = calculateAnimatedRotationX(visibleItems, index, center, maxDistance)

            Box(
                modifier = Modifier
                    .height(config.itemHeight)
                    .alpha(alpha)
                    .graphicsLayer { this.rotationX = rotationX },
                contentAlignment = Alignment.Center,
                content = { itemContent(item) }
            )
        }
    }
}