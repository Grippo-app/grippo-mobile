package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.max

@Composable
public fun BottomOverlayContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: @Composable (Modifier, PaddingValues) -> Unit
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val bottomPlaceables: List<androidx.compose.ui.layout.Placeable>
        val bottomHeight: Int
        if (bottom != null) {
            bottomPlaceables = subcompose("bottom") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layoutId("bottom"),
                    content = bottom
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

        val overlayInset = with(this) { bottomHeight.toDp() }
        val layoutDirection: LayoutDirection = this.layoutDirection
        val startPad = contentPadding.calculateStartPadding(layoutDirection)
        val endPad = contentPadding.calculateEndPadding(layoutDirection)
        val topPad = contentPadding.calculateTopPadding()
        val bottomPad = contentPadding.calculateBottomPadding()
        val resolvedPadding = PaddingValues(
            start = startPad,
            top = topPad,
            end = endPad,
            bottom = bottomPad + overlayInset
        )

        val contentPlaceables = subcompose("content") {
            content(
                Modifier.layoutId("content"),
                resolvedPadding
            )
        }.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minHeight = 0
                )
            )
        }

        val overlayPlaceables: List<androidx.compose.ui.layout.Placeable> =
            if (bottomHeight > 0) {
                subcompose("overlay") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(with(this) { bottomHeight.toDp() })
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        overlay.copy(alpha = 0f),
                                        overlay.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                }.map { measurable ->
                    measurable.measure(
                        constraints.copy(
                            minWidth = 0,
                            minHeight = 0,
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

            if (overlayPlaceables.isNotEmpty()) {
                val overlayTop = (layoutHeight - bottomHeight).coerceAtLeast(0)
                overlayPlaceables.forEach { it.placeRelative(0, overlayTop) }
            }

            if (bottomPlaceables.isNotEmpty()) {
                val y = (layoutHeight - bottomHeight).coerceAtLeast(0)
                bottomPlaceables.forEach { it.placeRelative(0, y) }
            }
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
