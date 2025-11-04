package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp

@Composable
public fun BottomOverlayLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    reverseLayout: Boolean = false,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: LazyListScope.() -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        // 1) measure bottom only if provided
        val bottomPlaceables: List<Placeable>
        val bottomHeight: Int
        if (bottom != null) {
            bottomPlaceables = subcompose("bottom") {
                Column(modifier = Modifier.fillMaxWidth(), content = bottom)
            }.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
            bottomHeight = bottomPlaceables.maxOfOrNull { it.height } ?: 0
        } else {
            bottomPlaceables = emptyList()
            bottomHeight = 0
        }

        // 2) overlay height equals bottom height
        val overlayHeightPx = bottomHeight

        // 3) list paddings = original + bottom (no extra for overlay)
        val startPad = contentPadding.calculateStartPadding(layoutDirection)
        val endPad = contentPadding.calculateEndPadding(layoutDirection)
        val topPad = contentPadding.calculateTopPadding()
        val bottomPad = contentPadding.calculateBottomPadding()
        val extraBottomPadDp = bottomHeight.toDp()

        val listPlaceables = subcompose("list") {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
        }.map { it.measure(constraints) }

        // 4) overlay: pinned to container bottom (under bottom content), dark at bottom → transparent upward
        val overlayPlaceables: List<Placeable> =
            if (overlayHeightPx > 0) {
                subcompose("overlay") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(overlayHeightPx.toDp())
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        overlay.copy(alpha = 0f),
                                        overlay.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                }.map {
                    it.measure(
                        constraints.copy(minWidth = 0, minHeight = 0, maxHeight = overlayHeightPx)
                    )
                }
            } else {
                emptyList()
            }

        val width = constraints.maxWidth
        val height = constraints.maxHeight

        layout(width, height) {
            // list (backmost)
            listPlaceables.forEach { it.placeRelative(0, 0) }

            // overlay at absolute bottom
            if (overlayPlaceables.isNotEmpty()) {
                overlayPlaceables.forEach {
                    it.placeRelative(0, height - overlayHeightPx)
                }
            }

            // bottom content on top
            if (bottomPlaceables.isNotEmpty()) {
                bottomPlaceables.forEach {
                    it.placeRelative(0, height - it.height)
                }
            }
        }
    }
}