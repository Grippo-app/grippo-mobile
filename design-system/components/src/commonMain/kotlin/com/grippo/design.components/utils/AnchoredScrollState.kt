package com.grippo.design.components.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Immutable
public enum class AnchorScrollBehavior {

    Instant,

    Animated,
}

// Lazy states anchor to the first visible key, so items prepended above it leave a fresh screen scrolled down.
@Composable
public fun rememberAnchoredLazyGridState(
    behavior: AnchorScrollBehavior = AnchorScrollBehavior.Instant,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyGridState {
    val state = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    AnchorToTopEffect(
        firstIndex = { state.firstVisibleItemIndex },
        firstOffset = { state.firstVisibleItemScrollOffset },
        isScrolling = { state.isScrollInProgress },
        behavior = behavior,
        instantScrollToTop = { state.requestScrollToItem(0) },
        animateScrollToTop = { state.animateScrollToItem(0) },
    )
    return state
}

@Composable
public fun rememberAnchoredLazyListState(
    behavior: AnchorScrollBehavior = AnchorScrollBehavior.Instant,
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
): LazyListState {
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset,
    )
    AnchorToTopEffect(
        firstIndex = { state.firstVisibleItemIndex },
        firstOffset = { state.firstVisibleItemScrollOffset },
        isScrolling = { state.isScrollInProgress },
        behavior = behavior,
        instantScrollToTop = { state.requestScrollToItem(0) },
        animateScrollToTop = { state.animateScrollToItem(0) },
    )
    return state
}

@Composable
private fun AnchorToTopEffect(
    firstIndex: () -> Int,
    firstOffset: () -> Int,
    isScrolling: () -> Boolean,
    behavior: AnchorScrollBehavior,
    instantScrollToTop: () -> Unit,
    animateScrollToTop: suspend () -> Unit,
) {
    val firstIndexState = rememberUpdatedState(firstIndex)
    val firstOffsetState = rememberUpdatedState(firstOffset)
    val isScrollingState = rememberUpdatedState(isScrolling)
    val behaviorState = rememberUpdatedState(behavior)
    val instantScrollState = rememberUpdatedState(instantScrollToTop)
    val animateScrollState = rememberUpdatedState(animateScrollToTop)

    var pinnedToTop by remember {
        mutableStateOf(firstIndex() == 0 && firstOffset() == 0)
    }

    // Prepend drift and requestScrollToItem bypass the mutator; only a settled isScrollInProgress marks real scrolls.
    LaunchedEffect(Unit) {
        var lastWasScrolling = isScrollingState.value()
        snapshotFlow {
            ScrollSnapshot(
                firstIndex = firstIndexState.value(),
                firstOffset = firstOffsetState.value(),
                isScrolling = isScrollingState.value(),
            )
        }
            .distinctUntilChanged()
            .collect { snapshot ->
                val justSettled = lastWasScrolling && !snapshot.isScrolling
                lastWasScrolling = snapshot.isScrolling

                if (snapshot.isScrolling) return@collect
                val atTop = snapshot.firstIndex == 0 && snapshot.firstOffset == 0

                when {
                    atTop -> pinnedToTop = true
                    justSettled -> pinnedToTop = false
                    pinnedToTop -> when (behaviorState.value) {
                        AnchorScrollBehavior.Instant -> instantScrollState.value()
                        AnchorScrollBehavior.Animated -> animateScrollState.value()
                    }
                }
            }
    }
}

@Immutable
private data class ScrollSnapshot(
    val firstIndex: Int,
    val firstOffset: Int,
    val isScrolling: Boolean,
)
