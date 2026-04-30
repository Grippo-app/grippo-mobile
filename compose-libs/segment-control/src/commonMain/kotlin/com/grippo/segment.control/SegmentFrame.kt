package com.grippo.segment.control

import androidx.compose.animation.animateBounds
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import com.grippo.segment.control.internal.SelectedBackground
import com.grippo.segment.control.internal.SelectedBackgroundId
import com.grippo.segment.control.internal.SelectedButtonId

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
    var animateChanges by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateChanges = true }

    LookaheadScope {
        Layout(
            modifier = modifier
                .height(IntrinsicSize.Max)
                .selectableGroup(),
            content = {
                SelectedBackground(
                    modifier = if (animateChanges) Modifier.animateBounds(this) else Modifier,
                    content = thumb,
                )
                content()
            },
        ) { measurables, constraints ->
            var selectedBackgroundMeasurable: Measurable? = null
            var selectedButtonIndex = -1
            val buttonMeasurables = ArrayList<Measurable>(measurables.size)

            for (measurable in measurables) {
                when (measurable.layoutId) {
                    SelectedBackgroundId -> selectedBackgroundMeasurable = measurable
                    SelectedButtonId -> {
                        require(selectedButtonIndex == -1) {
                            "Segmented control must have at most one selected button"
                        }
                        selectedButtonIndex = buttonMeasurables.size
                        buttonMeasurables += measurable
                    }

                    else -> buttonMeasurables += measurable
                }
            }

            val buttonCount = buttonMeasurables.size
            if (buttonCount == 0) {
                return@Layout layout(0, 0) {}
            }

            val buttonConstraints = if (segmentSizing == SegmentSizing.EqualFill) {
                val equalWidth = constraints.maxWidth / buttonCount
                constraints.copy(minWidth = equalWidth, maxWidth = equalWidth)
            } else {
                constraints.copy(minWidth = 0)
            }

            // Single pass: measure children, accumulate width and max height
            // instead of running map -> sumOf -> maxOfOrNull separately.
            val buttonPlaceables = ArrayList<Placeable>(buttonCount)
            var totalWidth = 0
            var maxHeight = 0
            for (measurable in buttonMeasurables) {
                val placeable = measurable.measure(buttonConstraints)
                buttonPlaceables += placeable
                totalWidth += placeable.width
                if (placeable.height > maxHeight) maxHeight = placeable.height
            }
            if (maxHeight == 0) maxHeight = constraints.minHeight

            // Sum widths of buttons preceding the selected one to get its X.
            // A while-loop is used instead of `0 until selectedButtonIndex`
            // because the IDE flags that range as potentially empty (it is,
            // by design, when no selection or the first item is selected).
            var selectedButtonX = 0
            var i = 0
            while (i < selectedButtonIndex) {
                selectedButtonX += buttonPlaceables[i].width
                i++
            }

            val selectedBackgroundPlaceable =
                if (selectedButtonIndex >= 0 && selectedBackgroundMeasurable != null) {
                    val selectedButtonWidth = buttonPlaceables[selectedButtonIndex].width
                    selectedBackgroundMeasurable.measure(
                        Constraints.fixed(selectedButtonWidth, maxHeight),
                    )
                } else {
                    null
                }

            layout(totalWidth, maxHeight) {
                selectedBackgroundPlaceable?.placeRelative(x = selectedButtonX, y = 0)

                var currentX = 0
                for (placeable in buttonPlaceables) {
                    placeable.placeRelative(x = currentX, y = 0)
                    currentX += placeable.width
                }
            }
        }
    }
}
