package com.grippo.design.components.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCard
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlin.math.max

@Composable
public fun OverlayContainer(
    modifier: Modifier = Modifier,
    overlay: Color,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val fade = AppTokens.dp.contentPadding.block
    val slotModifier = Modifier
        .fillMaxWidth()
        // As in Material's Surface: without these, touches and accessibility focus reach hidden content.
        .semantics { isTraversalGroup = true }
        .pointerInput(Unit) {}

    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val fadePx = fade.roundToPx()

        val topPlaceables: List<Placeable> = if (top == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.TOP) {
                Column(
                    modifier = slotModifier,
                    content = top,
                )
            }.map { it.measure(looseConstraints) }
        }

        val bottomPlaceables: List<Placeable> = if (bottom == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.BOTTOM) {
                Column(
                    modifier = slotModifier,
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
                EdgeOverlay(edge = OverlayEdge.Top, fadePx = fadePx, color = overlay)
            }.map { it.measure(constraints.copy(minWidth = 0, minHeight = topHeight, maxHeight = topHeight)) }
        }

        val bottomOverlayPlaceables: List<Placeable> = if (bottom == null) {
            emptyList()
        } else {
            subcompose(OverlaySlots.BOTTOM_OVERLAY) {
                EdgeOverlay(edge = OverlayEdge.Bottom, fadePx = fadePx, color = overlay)
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

private object OverlaySlots {
    const val TOP = "top"
    const val BOTTOM = "bottom"
    const val CONTENT = "content"
    const val TOP_OVERLAY = "top_overlay"
    const val BOTTOM_OVERLAY = "bottom_overlay"
}

@Composable
private fun EdgeOverlay(
    edge: OverlayEdge,
    fadePx: Int,
    color: Color,
) {
    Spacer(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val brush = edge.brush(color = color, fadeFraction = fadePx / size.height)

                onDrawBehind { drawRect(brush = brush) }
            },
    )
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

@AppPreview
@Composable
private fun OverlayContainerPreview() {
    PreviewContainer {
        OverlayContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            overlay = AppTokens.colors.background.screen,
            contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
            top = {
                Spacer(modifier = Modifier.statusBarsPadding())
            },
            bottom = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
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

                Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

                Spacer(modifier = Modifier.navigationBarsPadding())
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberLazyListState(initialFirstVisibleItemIndex = 2),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    items(count = 20) { index ->
                        CheckSelectableCard(
                            modifier = Modifier.fillMaxWidth(),
                            style = CheckSelectableCardStyle.Medium(
                                title = "Option ${index + 1}",
                                description = "Scrolls under both pinned edges",
                            ),
                            isSelected = false,
                            onSelect = {},
                        )
                    }
                }
            }
        )
    }
}

@AppPreview
@Composable
private fun OverlayContainerBottomOnlyPreview() {
    PreviewContainer {
        OverlayContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            overlay = AppTokens.colors.background.screen,
            contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
            bottom = {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                    content = ButtonContent.Text(text = "Primary action"),
                    style = ButtonStyle.Primary,
                    onClick = {},
                )

                Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

                Spacer(modifier = Modifier.navigationBarsPadding())
            },
            content = { padding ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                ) {
                    items(count = 20) { index ->
                        CheckSelectableCard(
                            modifier = Modifier.fillMaxWidth(),
                            style = CheckSelectableCardStyle.Medium(
                                title = "Option ${index + 1}",
                                description = "Scrolls under the pinned bottom edge",
                            ),
                            isSelected = false,
                            onSelect = {},
                        )
                    }
                }
            }
        )
    }
}
