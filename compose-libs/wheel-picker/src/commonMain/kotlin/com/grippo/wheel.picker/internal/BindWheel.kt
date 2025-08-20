package com.grippo.wheel.picker.internal

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Composable
internal fun <T> BindWheel(
    listState: LazyListState,
    items: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    isValid: (T) -> Boolean
) {
    val onSelectRef = rememberUpdatedState(onSelect)
    val isValidRef = rememberUpdatedState(isValid)

    // First sync should be instant; subsequent ones animated.
    var firstSync by remember(listState) { mutableStateOf(true) }

    // --- Sync the wheel position to the current `selected` item.
    // Wait for layout; if snapped != target -> first time: scroll (no anim), then: animate.
    LaunchedEffect(items, selected, listState) {
        if (items.isEmpty()) return@LaunchedEffect

        snapshotFlow { listState.layoutInfo.totalItemsCount }
            .first { it > 0 }

        val target = items.indexOf(selected)
        if (target < 0) {
            firstSync = false // mark done to avoid re-running as "first"
            return@LaunchedEffect
        }

        val snapped = calculateSnappedItemIndex(listState)
        if (snapped == target) {
            firstSync = false
            return@LaunchedEffect
        }

        if (firstSync) {
            listState.scrollToItem(target) // no animation on the very first sync
            firstSync = false
        } else {
            listState.animateScrollToItem(target)
        }
    }

    // --- After the user scrolls and the list snaps, ensure the snapped item is valid.
    // If it's invalid, automatically animate to the nearest valid item.
    // `distinctUntilChanged` prevents duplicate triggers; `collectLatest` cancels older animations.
    LaunchedEffect(listState, items, isValidRef.value) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .map { calculateSnappedItemIndex(listState) }
            .collectLatest { snapped ->
                val curr = items.getOrNull(snapped) ?: return@collectLatest
                if (isValidRef.value(curr)) {
                    onSelectRef.value(curr)
                } else {
                    val target = nearestValidIndex(
                        items = items,
                        fromIndex = snapped,
                        isValid = isValidRef.value
                    ) ?: return@collectLatest

                    listState.animateScrollToItem(target)
                    items.getOrNull(target)?.let { onSelectRef.value(it) }
                }
            }
    }
}

private fun calculateSnappedItemIndex(state: LazyListState): Int {
    val i = state.firstVisibleItemIndex
    val itemH = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return i
    val off = state.firstVisibleItemScrollOffset
    val last = state.layoutInfo.totalItemsCount - 1
    return if (off > itemH / 2 && i < last) i + 1 else i
}

private fun <T> nearestValidIndex(
    items: List<T>,
    fromIndex: Int,
    isValid: (T) -> Boolean
): Int? {
    if (fromIndex !in items.indices) return null
    if (isValid(items[fromIndex])) return null
    var l = fromIndex - 1
    var r = fromIndex + 1
    while (l >= 0 || r < items.size) {
        if (l >= 0 && isValid(items[l])) return l
        if (r < items.size && isValid(items[r])) return r
        l--; r++
    }
    return null
}