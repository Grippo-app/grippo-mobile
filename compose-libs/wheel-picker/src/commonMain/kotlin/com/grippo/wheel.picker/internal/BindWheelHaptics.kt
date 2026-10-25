package com.grippo.wheel.picker.internal

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun BindWheelHaptics(listState: LazyListState) {
    val tick = rememberWheelTick()

    LaunchedEffect(listState, tick) {
        // Cleared when scrolling settles rather than on DragInteraction.Stop, so the fling after release still ticks.
        var driverActive = false
        var lastSnapped = -1

        coroutineScope {
            launch {
                listState.interactionSource.interactions.collect { interaction ->
                    if (interaction is DragInteraction.Start) driverActive = true
                }
            }

            snapshotFlow {
                calculateSnappedItemIndex(listState) to listState.isScrollInProgress
            }.collect { (idx, scrolling) ->
                if (driverActive && lastSnapped != -1 && idx != lastSnapped) {
                    tick()
                }
                lastSnapped = idx
                if (!scrolling && driverActive) driverActive = false
            }
        }
    }
}
