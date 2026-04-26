package com.grippo.design.components.scroll

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

/**
 * Drop-in replacement for [rememberLazyGridState] that keeps the grid pinned
 * to the top while the user is sitting there.
 *
 * Default Compose behavior anchors the scroll position to the *first visible
 * item key*. When new items are prepended (e.g. async data trickles in above
 * what is currently rendered), Compose keeps the previously-first-visible
 * item at the same offset, and the newly inserted items end up rendered above
 * the viewport. Visually it looks like the screen opened already scrolled
 * down.
 *
 * This helper detects that case and snaps back to the top using [behavior].
 * As soon as the user scrolls down themselves the snap disengages — Compose's
 * default anchor preserves their position. When they return to `(0, 0)`, the
 * snap re-engages.
 */
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

/**
 * Same as [rememberAnchoredLazyGridState] but for [LazyListState]
 * (`LazyColumn` / `LazyRow`).
 */
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

/**
 * Pure side-effect that implements the "stay pinned to the top while at the
 * top" behavior on top of any lazy scrollable. Both `Lazy*State` flavors
 * share the same logic — it only needs a handful of operations to do its
 * job, so we pass them in as lambdas instead of inventing a common interface.
 */
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

    // Whether the auto-snap-to-top behavior is currently engaged.
    // Initialized from the actual current position so that:
    //  - a freshly opened screen at (0, 0) is pinned by default,
    //  - a screen restored from a non-top scroll position is NOT pinned,
    //    which prevents us from wiping the user's saved scroll on return.
    var pinnedToTop by remember {
        mutableStateOf(firstIndex() == 0 && firstOffset() == 0)
    }

    // A single flow handles all transitions — no cross-coroutine race.
    //
    // We discriminate two reasons we may end up off the top:
    //
    //  - Silent drift: Compose's anchor logic moved `firstVisibleItemIndex`
    //    away from 0 during a measure pass after items were prepended. This
    //    never goes through the scroll mutator, so `isScrolling` stays
    //    `false` throughout. This is what we want to correct.
    //
    //  - Active scroll: a user gesture or an external programmatic call like
    //    `state.scrollToItem(N)` / `animateScrollToItem(N)`. Both of those
    //    DO go through the scroll mutator, so we observe a `isScrolling`
    //    transition `true → false`. We respect their destination and
    //    un-pin, otherwise we'd silently undo external scroll-to logic.
    //
    // `lastWasScrolling` is the discriminator. Our own `requestScrollToItem`
    // (Instant) bypasses the mutator and looks identical to silent drift,
    // which is fine because it always converges to (0, 0) and the
    // `if (atTop)` branch wins. Our own `animateScrollToItem` (Animated)
    // does run through the mutator, but it also converges to (0, 0), so
    // the `if (atTop)` branch wins before `justSettled` matters.
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
                    // else: already un-pinned and settled off the top — leave
                    // the user where they are, default Compose anchor handles
                    // their position when items change.
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
