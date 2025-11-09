package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
public fun BottomOverlayLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: androidx.compose.foundation.layout.Arrangement.Vertical = androidx.compose.foundation.layout.Arrangement.Top,
    reverseLayout: Boolean = false,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: LazyListScope.() -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        // Measure bottom content (if provided)
        val bottomPlaceables: List<androidx.compose.ui.layout.Placeable>
        val bottomHeight: Int
        if (bottom != null) {
            bottomPlaceables = subcompose("bottom") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layoutId("bottom"), content = bottom
                )
            }.map { measurable ->
                measurable.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0
                    )
                )
            }
            bottomHeight = bottomPlaceables.maxOfOrNull { it.height } ?: 0
        } else {
            bottomPlaceables = emptyList()
            bottomHeight = 0
        }

        // Overlay height equals bottom height
        val overlayHeightPx = bottomHeight

        // Compute list paddings = original + bottom height (no extra for overlay)
        val layoutDirection: LayoutDirection = this.layoutDirection
        val startPad = contentPadding.calculateStartPadding(layoutDirection)
        val endPad = contentPadding.calculateEndPadding(layoutDirection)
        val topPad = contentPadding.calculateTopPadding()
        val bottomPad = contentPadding.calculateBottomPadding()
        val extraBottomPadDp = with(this) { overlayHeightPx.toDp() }

        // Measure list with "soft" height (do not force max height)
        val listPlaceables = subcompose("list") {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .layoutId("list"),
                state = state,
                contentPadding = PaddingValues(
                    start = startPad,
                    top = topPad,
                    end = endPad,
                    bottom = bottomPad + extraBottomPadDp
                ),
                verticalArrangement = verticalArrangement,
                reverseLayout = reverseLayout,
                flingBehavior = flingBehavior,
                userScrollEnabled = userScrollEnabled,
                content = content
            )
        }.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minHeight = 0
                )
            )
        }

        // Measure overlay box (pinned to container bottom, dark at bottom → transparent upward)
        val overlayPlaceables: List<androidx.compose.ui.layout.Placeable> =
            if (overlayHeightPx > 0) {
                subcompose("overlay") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(this) { overlayHeightPx.toDp() })
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        overlay.copy(alpha = 0f),   // top (transparent)
                                        overlay.copy(alpha = 0.9f)  // bottom (dark)
                                    )
                                )
                            )
                    )
                }.map { measurable ->
                    measurable.measure(
                        constraints.copy(
                            minWidth = 0,
                            minHeight = 0,
                            maxHeight = overlayHeightPx
                        )
                    )
                }
            } else {
                emptyList()
            }

        val width = constraints.maxWidth

        // Desired layout height: at least what's needed by list or bottom, but respect parent's constraints.
        val listHeight = listPlaceables.maxOfOrNull { it.height } ?: 0
        val desiredHeight = max(listHeight, bottomHeight)
        val layoutHeight = desiredHeight
            .coerceAtLeast(constraints.minHeight)
            .coerceAtMost(constraints.maxHeight)

        layout(width, layoutHeight) {
            // Place list at back
            listPlaceables.forEach { it.placeRelative(0, 0) }

            // Place overlay at absolute bottom (under bottom content visually)
            if (overlayPlaceables.isNotEmpty()) {
                val overlayTop = (layoutHeight - overlayHeightPx).coerceAtLeast(0)
                overlayPlaceables.forEach { it.placeRelative(0, overlayTop) }
            }

            // Place bottom content on top
            if (bottomPlaceables.isNotEmpty()) {
                val y = (layoutHeight - bottomHeight).coerceAtLeast(0)
                bottomPlaceables.forEach { it.placeRelative(0, y) }
            }
        }
    }
}