package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.max

private const val OverlayEndAlpha = 0.9f

private object BottomOverlaySlots {
    const val BOTTOM = "bottom"
    const val CONTENT = "content"
    const val OVERLAY = "overlay"
}

@Composable
public fun BottomOverlayContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: @Composable (Modifier, PaddingValues) -> Unit
) {
    // Fast-path: when there's no bottom slot we don't need SubcomposeLayout at all.
    // SubcomposeLayout is the most expensive piece here — skip it when possible.
    if (bottom == null) {
        Box(modifier = modifier) {
            content(Modifier, contentPadding)
        }
        return
    }

    // Hoist the gradient brush. Without this, every measure pass re-allocates a
    // new ShaderBrush + Color instances (the overlay subcompose lambda captures
    // bottomHeight, so it re-runs on each measure), which forces the overlay
    // layer to redraw on every frame and pressures the iOS render thread.
    val overlayBrush = remember(overlay) {
        Brush.verticalGradient(
            colors = listOf(
                overlay.copy(alpha = 0f),
                overlay.copy(alpha = OverlayEndAlpha),
            )
        )
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        // 1) Measure the bottom slot first so we know how much vertical space it takes.
        val bottomPlaceables: List<Placeable> = subcompose(BottomOverlaySlots.BOTTOM) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = bottom
            )
        }.map { it.measure(looseConstraints) }

        val bottomHeight: Int = bottomPlaceables.maxOfOrNull { it.height } ?: 0

        // 2) Build padding for the content slot — user padding + bottom inset.
        val layoutDirection: LayoutDirection = this.layoutDirection
        val startPad = contentPadding.calculateStartPadding(layoutDirection)
        val endPad = contentPadding.calculateEndPadding(layoutDirection)
        val topPad = contentPadding.calculateTopPadding()
        val bottomPad = contentPadding.calculateBottomPadding()
        val overlayInset = bottomHeight.toDp()
        val resolvedPadding = PaddingValues(
            start = startPad,
            top = topPad,
            end = endPad,
            bottom = bottomPad + overlayInset
        )

        // 3) Measure content with the resolved padding propagated to it.
        val contentPlaceables: List<Placeable> = subcompose(BottomOverlaySlots.CONTENT) {
            content(Modifier, resolvedPadding)
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        // 4) Measure overlay only if there's a bottom slot to fade behind.
        // Constraints pin the overlay height exactly — no extra `.height(...)` modifier needed.
        val overlayPlaceables: List<Placeable> = if (bottomHeight > 0) {
            subcompose(BottomOverlaySlots.OVERLAY) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(overlayBrush)
                )
            }.map {
                it.measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = bottomHeight,
                        maxHeight = bottomHeight
                    )
                )
            }
        } else {
            emptyList()
        }

        val width = constraints.maxWidth
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0
        val desiredHeight = max(contentHeight, bottomHeight)
        val layoutHeight = desiredHeight
            .coerceAtLeast(constraints.minHeight)
            .coerceAtMost(constraints.maxHeight)

        layout(width, layoutHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }

            val anchorY = (layoutHeight - bottomHeight).coerceAtLeast(0)
            if (overlayPlaceables.isNotEmpty()) {
                overlayPlaceables.forEach { it.placeRelative(0, anchorY) }
            }
            bottomPlaceables.forEach { it.placeRelative(0, anchorY) }
        }
    }
}

@AppPreview
@Composable
private fun BottomOverlayContainerPreview() {
    PreviewContainer {
        BottomOverlayContainer(
            overlay = Color.Black,
            bottom = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Text("Bottom content")
                }
            },
            content = { modifier, padding ->
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .padding(padding)
                ) {
                    Text("Main content with bottom overlay")
                }
            }
        )
    }
}
