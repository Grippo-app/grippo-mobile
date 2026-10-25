package com.grippo.design.components.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.max

private object OverlaySlots {
    const val TOP = "top"
    const val BOTTOM = "bottom"
    const val CONTENT = "content"
    const val TOP_OVERLAY = "top_overlay"
    const val BOTTOM_OVERLAY = "bottom_overlay"
}

private enum class OverlayEdge {
    Top,
    Bottom;

    fun brush(color: Color, fadeFraction: Float): Brush {
        val clear = color.copy(alpha = 0f)

        return when (this) {
            Top -> Brush.verticalGradient(0f to color, 1f - fadeFraction to color, 1f to clear)
            Bottom -> Brush.verticalGradient(0f to clear, fadeFraction to color, 1f to color)
        }
    }
}

@Composable
public fun OverlayContainer(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    overlay: Color,
    content: @Composable (PaddingValues) -> Unit
) {
    val fade = AppTokens.dp.contentPadding.block

    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val fadePx = fade.roundToPx()

        val topPlaceables: List<Placeable> = if (top == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.TOP) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = top,
                )
            }.map { it.measure(looseConstraints) }
        }

        val bottomPlaceables: List<Placeable> = if (bottom == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.BOTTOM) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = bottom,
                )
            }.map { it.measure(looseConstraints) }
        }

        val topHeight: Int = if (top == null) 0 else (topPlaceables.maxOfOrNull { it.height } ?: 0) + fadePx
        val bottomHeight: Int = if (bottom == null) 0 else (bottomPlaceables.maxOfOrNull { it.height } ?: 0) + fadePx

        val resolvedPadding = PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + topHeight.toDp(),
            end = contentPadding.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding() + bottomHeight.toDp(),
        )

        val contentPlaceables: List<Placeable> = subcompose(OverlaySlots.CONTENT) {
            content(resolvedPadding)
        }.map { it.measure(constraints.copy(minHeight = 0)) }

        val topOverlayPlaceables: List<Placeable> = if (top == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.TOP_OVERLAY) {
                EdgeOverlay(edge = OverlayEdge.Top, fade = fadePx, color = overlay)
            }.map { it.measure(constraints.copy(minWidth = 0, minHeight = topHeight, maxHeight = topHeight)) }
        }

        val bottomOverlayPlaceables: List<Placeable> = if (bottom == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.BOTTOM_OVERLAY) {
                EdgeOverlay(edge = OverlayEdge.Bottom, fade = fadePx, color = overlay)
            }.map { it.measure(constraints.copy(minWidth = 0, minHeight = bottomHeight, maxHeight = bottomHeight)) }
        }

        val width = constraints.maxWidth
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: 0
        val layoutHeight = max(contentHeight, topHeight + bottomHeight)
            .coerceAtLeast(constraints.minHeight)
            .coerceAtMost(constraints.maxHeight)

        layout(width, layoutHeight) {
            contentPlaceables.forEach { it.placeRelative(0, 0) }

            topOverlayPlaceables.forEach { it.placeRelative(0, 0) }
            topPlaceables.forEach { it.placeRelative(0, 0) }

            val bottomY = (layoutHeight - bottomHeight).coerceAtLeast(0)
            bottomOverlayPlaceables.forEach { it.placeRelative(0, bottomY) }
            bottomPlaceables.forEach { it.placeRelative(0, bottomY + fadePx) }
        }
    }
}

@Composable
private fun EdgeOverlay(
    edge: OverlayEdge,
    fade: Int,
    color: Color,
) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = edge.brush(color = color, fadeFraction = fade / size.height)

                onDrawBehind { drawRect(brush = brush) }
            },
    )
}

@AppPreview
@Composable
private fun OverlayContainerPreview() {
    PreviewContainer {
        OverlayContainer(
            overlay = AppTokens.colors.background.screen,
            top = {
                Text(
                    modifier = Modifier.padding(
                        start = AppTokens.dp.contentPadding.content,
                        end = AppTokens.dp.contentPadding.content,
                        top = AppTokens.dp.contentPadding.content,
                    ),
                    text = "Top content",
                    color = AppTokens.colors.text.primary,
                    style = AppTokens.typography.h5(),
                )
            },
            bottom = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(text = "Primary action"),
                        style = ButtonStyle.Primary,
                        onClick = {},
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        content = ButtonContent.Text(text = "Secondary action"),
                        style = ButtonStyle.Secondary,
                        onClick = {},
                    )
                }
            },
            content = { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTokens.colors.background.dialog)
                        .padding(padding)
                ) {
                    Text(
                        text = "Main content between pinned edges",
                        color = AppTokens.colors.text.primary,
                        style = AppTokens.typography.b14Med(),
                    )
                }
            }
        )
    }
}
