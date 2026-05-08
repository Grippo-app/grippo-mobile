package com.grippo.wheel.picker.internal

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Emits one platform-native scroll-tick per snapped-index change while the user is the one
 * driving the scroll — drag and the fling/snap that follows the release. Programmatic
 * scrolls (initial position sync, invalid → nearest-valid auto-correction, external
 * `selected` updates) stay silent so the wheel matches the native iOS picker.
 *
 * The `tick` callback is platform-specific (see [rememberWheelTick]). On iOS it goes through
 * `UISelectionFeedbackGenerator`; on Android it uses `SEGMENT_FREQUENT_TICK` on API 34+ and
 * falls back to `CLOCK_TICK` on older devices.
 */
@Composable
internal fun BindWheelHaptics(listState: LazyListState) {
    val tick = rememberWheelTick()

    LaunchedEffect(listState, tick) {
        // `driverActive` flips on at DragInteraction.Start and stays on through the fling+snap
        // that follows the finger release. It clears only when the resulting scroll fully
        // settles — so ticks fire across the whole drag → fling → snap arc, but never on
        // programmatic scrolls (which never produce a DragInteraction.Start).
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
