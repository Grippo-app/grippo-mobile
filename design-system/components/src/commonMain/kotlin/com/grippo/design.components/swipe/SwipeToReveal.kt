package com.grippo.design.components.swipe

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.components.training.ExerciseCard
import com.grippo.design.components.training.ExerciseCardStyle
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.icons.Trash
import kotlin.math.roundToInt

@Immutable
private enum class Slot {
    Actions,
    Content
}

/**
 * Keep the swipe viewport at screen width. Use [horizontalPadding] for card gutters
 * so actions enter from the screen edge and settle within those gutters.
 */
@Composable
public fun SwipeToReveal(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 0.dp,
    actions: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var maxRevealPx by remember { mutableFloatStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-maxRevealPx, 0f)
    }
    val layoutDirection = LocalLayoutDirection.current
    val isPartiallyRevealed by remember {
        derivedStateOf { offsetX < 0f && offsetX > -maxRevealPx }
    }

    LaunchedEffect(maxRevealPx) {
        dragState.drag {
            dragBy(offsetX.coerceIn(-maxRevealPx, 0f) - offsetX)
        }
    }

    // Actions keep their natural size; swiping never changes the row's height.
    SubcomposeLayout(
        modifier
            .clipToBounds()
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                reverseDirection = layoutDirection == LayoutDirection.Rtl,
                startDragImmediately = isPartiallyRevealed,
                onDragStopped = {
                    // A new drag cancels settling through DraggableState's mutation lock.
                    dragState.drag {
                        val target = if (offsetX <= -maxRevealPx / 2f) -maxRevealPx else 0f
                        animate(
                            initialValue = offsetX,
                            targetValue = target,
                            animationSpec = tween(220, easing = LinearOutSlowInEasing),
                        ) { value, _ ->
                            dragBy(value - offsetX)
                        }
                    }
                },
            )
    ) { constraints ->
        // The card defines the height, including for actions using fillMaxHeight().
        val contentPlaceable = subcompose(Slot.Content) {
            Box(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding)) { content() }
        }.single().measure(constraints)
        val layoutWidth = contentPlaceable.width
        val layoutHeight = contentPlaceable.height

        val actionsPlaceable = subcompose(Slot.Actions) {
            Row(
                modifier = Modifier.padding(
                    start = AppTokens.dp.swipe.actionSpacing,
                    end = horizontalPadding,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.swipe.actionSpacing),
                content = actions,
            )
        }.single().measure(
            Constraints(
                maxWidth = layoutWidth,
                minHeight = layoutHeight,
                maxHeight = layoutHeight,
            )
        )
        val actionsWidth = actionsPlaceable.width
        // The trailing gutter belongs to the screen, not to the card's travel distance.
        val revealWidth = (actionsWidth - horizontalPadding.roundToPx()).coerceAtLeast(0).toFloat()
        if (maxRevealPx != revealWidth) {
            maxRevealPx = revealWidth
        }

        layout(layoutWidth, layoutHeight) {
            // Read the animated offset in placement, without remeasuring every frame.
            val clampedOffset = offsetX.coerceIn(-maxRevealPx, 0f)
            val revealFraction = if (maxRevealPx > 0f) -clampedOffset / maxRevealPx else 0f
            val actionsX = layoutWidth - (actionsWidth * revealFraction).roundToInt()
            actionsPlaceable.placeRelative(actionsX, 0)
            contentPlaceable.placeRelative(clampedOffset.roundToInt(), 0)
        }
    }
}

@AppPreview
@Composable
private fun SwipeToRevealPreview() {
    PreviewContainer {
        SwipeToReveal(
            horizontalPadding = AppTokens.dp.screen.horizontalPadding,
            actions = {
                SwipeRevealAction(
                    label = AppTokens.strings.res(Res.string.delete_btn),
                    icon = AppTokens.icons.Trash,
                    onClick = {},
                )
            },
        ) {
            IterationCard(
                modifier = Modifier.fillMaxWidth(),
                value = stubIteration(),
                style = IterationCardStyle.Editable(
                    label = "1",
                    onVolumeClick = {},
                    onRepetitionClick = {},
                    volumeDecorator = {},
                    repetitionDecorator = {},
                ),
            )
        }
        SwipeToReveal(
            horizontalPadding = AppTokens.dp.screen.horizontalPadding,
            actions = {
                SwipeRevealAction(
                    label = AppTokens.strings.res(Res.string.delete_btn),
                    icon = AppTokens.icons.Trash,
                    onClick = {},
                )
            },
        ) {
            ExerciseCard(
                modifier = Modifier.fillMaxWidth(),
                value = stubExercise(),
                style = ExerciseCardStyle.Large(onClick = {}),
            )
        }
    }
}
