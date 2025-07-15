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
import androidx.compose.ui.unit.dp

@Composable
internal fun <T> SingleWheelColumn(
    modifier: Modifier = Modifier,
    items: List<T>,
    initial: T,
    rowCount: Int,
    onValueChange: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    labelContent: (@Composable () -> Unit)? = null,
) {
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

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val itemHeight = maxHeight / rowCount

        val labelWidthPx = remember { mutableStateOf(0) }

        if (labelContent != null) {
            SubcomposeLayout { constraints ->
                val placeables = subcompose("label", labelContent).map {
                    it.measure(constraints)
                }
                labelWidthPx.value = placeables.maxOfOrNull { it.width } ?: 0
                layout(0, 0) {}
            }
        }

        val centerItemOffsetY = remember(itemHeight) { itemHeight * ((rowCount - 1) / 2) }

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.Center),
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

        if (labelContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = centerItemOffsetY)
                    .padding(start = 4.dp),
                contentAlignment = Alignment.Center,
                content = { labelContent() }
            )
        }
    }
}