package com.grippo.design.components.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.icons.Cancel
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Immutable
private enum class Slot {
    Actions,
    Content
}

@Composable
public fun SwipeToReveal(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    // Slide offset of the foreground content: [0f .. -maxRevealPx]
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Will be measured from the actions slot (in px)
    var maxRevealPx by remember { mutableStateOf(0f) }

    // Keep offset clamped if actions width changes dynamically
    LaunchedEffect(maxRevealPx) {
        offsetX.snapTo(offsetX.value.coerceIn(-maxRevealPx, 0f))
    }

    // Velocity-agnostic settle with 50% threshold
    fun settle() {
        scope.launch {
            val target = if (offsetX.value <= -maxRevealPx / 2f) -maxRevealPx else 0f
            offsetX.animateTo(target, animationSpec = tween(220, easing = LinearOutSlowInEasing))
        }
    }

    // Pointer modifier depends on current reveal width
    val dragModifier = Modifier.pointerInput(maxRevealPx) {
        detectHorizontalDragGestures(
            onHorizontalDrag = { change, dragAmount ->
                change.consume()
                val newOffset = (offsetX.value + dragAmount).coerceIn(-maxRevealPx, 0f)
                scope.launch { offsetX.snapTo(newOffset) }
            },
            onDragCancel = { settle() },
            onDragEnd = { settle() }
        )
    }

    // Actions keep their natural size; swiping never changes the row's height.
    SubcomposeLayout(
        modifier
            .clipToBounds()
            .then(dragModifier)
    ) { constraints ->
        // Measure actions with loose constraints to get their natural width
        val actionsMeasurables = subcompose(Slot.Actions) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.swipe.actionSpacing, Alignment.End),
                content = actions
            )
        }
        val actionsPlaceables = actionsMeasurables.map {
            it.measure(
                Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth,
                    minHeight = 0,
                    maxHeight = constraints.maxHeight
                )
            )
        }
        val actionsWidth = actionsPlaceables.maxOfOrNull { it.width } ?: 0
        val actionsHeight = actionsPlaceables.maxOfOrNull { it.height } ?: 0

        // Update reveal width if changed (px)
        if (maxRevealPx != actionsWidth.toFloat()) {
            maxRevealPx = actionsWidth.toFloat()
        }

        // Measure foreground content with parent constraints
        val contentPlaceables = subcompose(Slot.Content) { content() }
            .map { it.measure(constraints) }

        val layoutWidth = contentPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val layoutHeight = max(
            contentPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight,
            actionsHeight
        )

        // Compute current offsets (px)
        val contentOffsetX = offsetX.value.roundToInt()
        // When content at 0f -> actions are entirely off-screen at x = layoutWidth
        // When content at -maxRevealPx -> actions flush to right edge at x = layoutWidth - actionsWidth
        val actionsSlidePx = (offsetX.value + maxRevealPx).coerceIn(0f, maxRevealPx).roundToInt()
        val actionsX = layoutWidth - (actionsWidth - actionsSlidePx)

        layout(layoutWidth, layoutHeight) {
            // Place actions (behind content), sliding in from the right, vertically centered
            var accX = actionsX
            actionsPlaceables.forEach { placeable ->
                val y = ((layoutHeight - placeable.height) / 2).coerceAtLeast(0)
                placeable.placeRelative(accX, y)
                accX += placeable.width
            }

            // Foreground content stays the same
            contentPlaceables.forEach { placeable ->
                placeable.placeRelative(contentOffsetX, 0)
            }
        }
    }
}

@AppPreview
@Composable
private fun SwipeToRevealPreview() {
    PreviewContainer {
        SwipeToReveal(
            actions = {
                SwipeRevealAction(
                    label = AppTokens.strings.res(Res.string.delete_btn),
                    icon = AppTokens.icons.Cancel,
                    onClick = {},
                )
            },
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTokens.colors.background.card)
                        .padding(AppTokens.dp.contentPadding.content)
                ) {
                    Text("Swipe left to reveal actions", color = AppTokens.colors.text.primary, style = AppTokens.typography.b14Med())
                }
            }
        )
    }
}
