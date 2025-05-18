package com.grippo.segment.control

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import com.grippo.segment.control.internal.SelectedBackground
import com.grippo.segment.control.internal.SelectedBackgroundId
import com.grippo.segment.control.internal.SelectedButtonId

/**
 * Defines how segments are sized in the UI.
 *
 * - *Unspecified*: Each segment sizes itself to its content.
 * - *EqualFill*: All segments share the available width equally.
 */

@Immutable
public enum class SegmentSizing {
    Unspecified,
    EqualFill,
}

@Composable
public fun SegmentedFrame(
    modifier: Modifier = Modifier,
    segmentSizing: SegmentSizing = SegmentSizing.Unspecified,
    content: @Composable () -> Unit,
    thumb: @Composable BoxScope.() -> Unit,
) {
    LookaheadScope {
        Layout(
            modifier = modifier
                .height(IntrinsicSize.Max)
                .selectableGroup(),
            content = {
                SelectedBackground(
                    modifier = Modifier.animateBounds(this),
                    content = thumb,
                )
                content()
            },
        ) { measurables, constraints ->
            require(measurables.count { it.layoutId == SelectedButtonId } <= 1) {
                "Segmented control must have at most one selected button"
            }

            val buttonMeasurables = measurables.filter { it.layoutId != SelectedBackgroundId }
            val buttonCount = buttonMeasurables.size

            val buttonPlaceables: List<Placeable>
            val totalWidth: Int
            val maxHeight: Int

            if (segmentSizing == SegmentSizing.EqualFill) {
                val equalWidth = constraints.maxWidth / buttonCount
                val buttonConstraints =
                    constraints.copy(minWidth = equalWidth, maxWidth = equalWidth)
                buttonPlaceables = buttonMeasurables.map { measurable ->
                    measurable.measure(buttonConstraints)
                }
                totalWidth = equalWidth * buttonCount
                maxHeight = buttonPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
            } else {
                buttonPlaceables = buttonMeasurables.map { measurable ->
                    measurable.measure(constraints.copy(minWidth = 0))
                }
                totalWidth = buttonPlaceables.sumOf { it.width }
                maxHeight = buttonPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
            }

            val selectedButtonIndex =
                buttonMeasurables.indexOfFirst { it.layoutId == SelectedButtonId }

            val selectedButtonX = if (selectedButtonIndex >= 0) {
                buttonPlaceables.take(selectedButtonIndex).sumOf { it.width }
            } else {
                0
            }
            val selectedButtonWidth = if (selectedButtonIndex >= 0) {
                buttonPlaceables[selectedButtonIndex].width
            } else {
                0
            }

            val selectedBackgroundMeasurable =
                measurables.first { it.layoutId == SelectedBackgroundId }
            val selectedBackgroundPlaceable = if (selectedButtonIndex >= 0) {
                selectedBackgroundMeasurable.measure(
                    Constraints.fixed(selectedButtonWidth, maxHeight),
                )
            } else {
                null
            }

            layout(totalWidth, maxHeight) {
                selectedBackgroundPlaceable?.placeRelative(x = selectedButtonX, y = 0)

                var currentX = 0
                buttonPlaceables.forEach { placeable ->
                    placeable.placeRelative(x = currentX, y = 0)
                    currentX += placeable.width
                }
            }
        }
    }
}
