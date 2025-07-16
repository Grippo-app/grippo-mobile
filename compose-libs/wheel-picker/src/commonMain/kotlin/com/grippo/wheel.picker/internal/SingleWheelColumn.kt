package com.grippo.wheel.picker.internal

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.grippo.wheel.picker.WheelConfig

@Composable
internal fun <T> SingleWheelColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    initial: T,
    config: WheelConfig,
    onValueChange: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
) {

    DisposableEffect(Unit) {
        println("DISPOSE TEST START")
        onDispose {
            println("DISPOSE TEST END")
        }
    }

    val initialIndex = remember(items, initial) {
        items.indexOf(initial).takeIf { it >= 0 } ?: 0
    }

    val listState = rememberLazyListState(initialIndex)

    val layoutInfo by remember { derivedStateOf { listState.layoutInfo } }

    val centerIndex by remember {
        derivedStateOf { layoutInfo.closestItemToCenter()?.index ?: initialIndex }
    }

    LaunchedEffect(centerIndex) {
        items.getOrNull(centerIndex)?.let { onValueChange(it) }
    }

    val flingBehavior = if (config.snapEnabled) {
        ScrollableDefaults.flingBehavior() // 🧯 basic snap
    } else {
        rememberSnapFlingBehavior(listState) // 🔄 auto snap
    }

    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(vertical = config.itemHeight * ((config.rowCount - 1) / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        flingBehavior = flingBehavior,
    ) {
        itemsIndexed(items) { index, item ->
            val alpha = calculateAnimatedAlpha(layoutInfo, index, config.rowCount)
            val rotationX = calculateAnimatedRotationX(layoutInfo, index, config.rowCount)

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