package com.grippo.design.components.dragdrop

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val SETTLE_DURATION_MS = 220
private const val AUTO_SCROLL_TICK_MS = 16L
private const val AUTO_SCROLL_FACTOR = 0.4f

@Stable
public class DragDropListState<K : Any> internal constructor(
    public val listState: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (fromKey: K, toKey: K) -> Unit,
    private val onDragEnd: () -> Unit,
) {
    public var draggingKey: K? by mutableStateOf(null)
        private set

    public var settlingKey: K? by mutableStateOf(null)
        private set

    private var initialOffset by mutableIntStateOf(0)
    private var draggingDelta by mutableFloatStateOf(0f)
    private var settleTranslation by mutableFloatStateOf(0f)
    private val draggableKeys = mutableSetOf<K>()
    private var didMove = false
    private var settleJob: Job? = null

    private val draggingItem: LazyListItemInfo?
        get() = draggingKey?.let { key ->
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
        }

    private fun draggingTranslation(): Float = draggingItem
        ?.let { (initialOffset + draggingDelta) - it.offset.toFloat() }
        ?: 0f

    public fun isDragging(key: K): Boolean = key == draggingKey

    public fun isInteracting(key: K): Boolean = key == draggingKey || key == settlingKey

    public fun translationFor(key: K): Float = when (key) {
        draggingKey -> draggingTranslation()
        settlingKey -> settleTranslation
        else -> 0f
    }

    internal fun registerKey(key: K) {
        draggableKeys += key
    }

    internal fun unregisterKey(key: K) {
        draggableKeys -= key
    }

    internal fun startDrag(key: K): Boolean {
        if (draggingKey != null) return false
        if (key !in draggableKeys) return false

        val item = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == key } ?: return false

        settleJob?.cancel()
        settleJob = null
        settlingKey = null
        settleTranslation = 0f

        draggingKey = key
        initialOffset = item.offset
        draggingDelta = 0f
        didMove = false
        return true
    }

    internal fun drag(amountY: Float) {
        val draggedKey = draggingKey ?: return

        draggingDelta += amountY

        val item = draggingItem ?: return
        val translation = (initialOffset + draggingDelta) - item.offset.toFloat()
        val itemMiddle = item.offset + translation + item.size / 2f

        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { other ->
            other.key != draggedKey &&
                    other.key in draggableKeys &&
                    itemMiddle in other.offset.toFloat()..(other.offset + other.size).toFloat()
        } ?: return

        @Suppress("UNCHECKED_CAST")
        val targetKey = target.key as K

        didMove = true
        onMove(draggedKey, targetKey)
    }

    internal fun endDrag() {
        val key = draggingKey ?: return
        val translation = draggingTranslation()

        draggingKey = null
        initialOffset = 0
        draggingDelta = 0f

        settleTranslation = translation
        settlingKey = key

        val shouldNotify = didMove
        didMove = false

        settleJob?.cancel()
        settleJob = scope.launch {
            try {
                animate(
                    initialValue = translation,
                    targetValue = 0f,
                    animationSpec = tween(SETTLE_DURATION_MS, easing = LinearOutSlowInEasing),
                ) { value, _ -> settleTranslation = value }
            } finally {
                if (settlingKey == key) {
                    settlingKey = null
                    settleTranslation = 0f
                }
            }
        }

        if (shouldNotify) onDragEnd()
    }

    internal fun overscrollAmount(): Float {
        val item = draggingItem ?: return 0f
        val translation = draggingTranslation()
        val top = item.offset + translation
        val bottom = top + item.size
        val viewportTop = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportBottom = listState.layoutInfo.viewportEndOffset.toFloat()
        return when {
            top < viewportTop -> top - viewportTop
            bottom > viewportBottom -> bottom - viewportBottom
            else -> 0f
        }
    }

    internal fun launchAutoScroll(): Job = scope.launch {
        while (isActive && draggingKey != null) {
            val amount = overscrollAmount()
            if (amount != 0f) {
                listState.scrollBy(amount * AUTO_SCROLL_FACTOR)
            }
            delay(AUTO_SCROLL_TICK_MS)
        }
    }
}

@Composable
public fun <K : Any> rememberDragDropListState(
    listState: LazyListState = rememberLazyListState(),
    onMove: (fromKey: K, toKey: K) -> Unit,
    onDragEnd: () -> Unit = {},
): DragDropListState<K> {
    val scope = rememberCoroutineScope()
    val moveCallback by rememberUpdatedState(onMove)
    val endCallback by rememberUpdatedState(onDragEnd)
    return remember(listState) {
        DragDropListState(
            listState = listState,
            scope = scope,
            onMove = { from, to -> moveCallback(from, to) },
            onDragEnd = { endCallback() },
        )
    }
}

@Composable
public fun <K : Any> LazyItemScope.DraggableItem(
    state: DragDropListState<K>,
    key: K,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(isDragging: Boolean) -> Unit,
) {
    DisposableEffect(state, key) {
        state.registerKey(key)
        onDispose { state.unregisterKey(key) }
    }

    val haptic = LocalHapticFeedback.current
    val isDragging = state.isDragging(key)
    val isInteracting = state.isInteracting(key)

    val gestureModifier = Modifier.pointerInput(state, key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val longPress = awaitLongPressOrCancellation(down.id)
                ?: return@awaitEachGesture

            if (!state.startDrag(key)) return@awaitEachGesture
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            longPress.consume()
            val autoScroll = state.launchAutoScroll()

            try {
                var pointerId = longPress.id
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (change.changedToUpIgnoreConsumed()) {
                        change.consume()
                        break
                    }
                    state.drag(change.positionChange().y)
                    change.consume()
                    pointerId = change.id
                }
            } finally {
                autoScroll.cancel()
                state.endDrag()
            }
        }
    }

    val visualModifier = if (isInteracting) {
        Modifier
            .zIndex(1f)
            .graphicsLayer { translationY = state.translationFor(key) }
    } else {
        Modifier.animateItem()
    }

    Box(modifier = modifier.then(gestureModifier).then(visualModifier)) {
        content(isDragging)
    }
}
