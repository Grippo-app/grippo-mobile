package com.grippo.design.components.tooltip.internal

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.grippo.design.components.tooltip.TooltipPlacement
import com.grippo.design.components.tooltip.TooltipVariant
import com.grippo.design.core.AppTokens

@Immutable
internal data class TooltipColorTokens(
    val background: Color,
    val border: Color,
    val text: Color,
    val subtext: Color,
    val icon: Color,
)

@Composable
internal fun resolveTooltipColors(variant: TooltipVariant): TooltipColorTokens {
    return when (variant) {
        TooltipVariant.Default -> TooltipColorTokens(
            background = AppTokens.colors.background.card,
            border = AppTokens.colors.border.default,
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.icon.primary,
        )

        TooltipVariant.Success -> TooltipColorTokens(
            background = AppTokens.colors.semantic.successBackground,
            border = AppTokens.colors.border.default,
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.success,
        )

        TooltipVariant.Error -> TooltipColorTokens(
            background = AppTokens.colors.semantic.errorBackground,
            border = AppTokens.colors.border.default,
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.error,
        )

        TooltipVariant.Warning -> TooltipColorTokens(
            background = AppTokens.colors.semantic.warningBackground,
            border = AppTokens.colors.border.default,
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.warning,
        )

        TooltipVariant.Info -> TooltipColorTokens(
            background = AppTokens.colors.semantic.infoBackground,
            border = AppTokens.colors.border.default,
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.info,
        )
    }
}

internal fun tooltipShape(
    placement: TooltipPlacement,
    arrowDepthPx: Float,
    arrowBasePx: Float,
    cornerRadiusPx: Float,
    arrowPositionPx: Float? = null,
): GenericShape = GenericShape { size, layoutDirection ->
    val width = size.width
    val height = size.height
    val absolutePlacement = placement.toAbsolute(layoutDirection)
    val isVertical = absolutePlacement == TooltipPlacement.Top ||
        absolutePlacement == TooltipPlacement.Bottom
    val arrowDepth = arrowDepthPx.coerceIn(0f, if (isVertical) height else width)
    val bodyWidth = if (isVertical) width else width - arrowDepth
    val bodyHeight = if (isVertical) height - arrowDepth else height
    val radius = cornerRadiusPx.coerceIn(0f, minOf(bodyWidth, bodyHeight) / 2f)
    val edgeLength = if (isVertical) width else height
    val halfArrowBase = (arrowBasePx / 2f).coerceIn(0f, (edgeLength / 2f - radius).coerceAtLeast(0f))
    val edgeInset = radius + halfArrowBase
    val arrowCenter = (arrowPositionPx ?: edgeLength / 2f).coerceIn(edgeInset, edgeLength - edgeInset)

    when (absolutePlacement) {
        TooltipPlacement.Top -> {
            val bodyBottom = height - arrowDepth
            moveTo(radius, 0f)
            lineTo(width - radius, 0f)
            arcTo(Rect(width - 2 * radius, 0f, width, 2 * radius), -90f, 90f, false)
            lineTo(width, bodyBottom - radius)
            arcTo(Rect(width - 2 * radius, bodyBottom - 2 * radius, width, bodyBottom), 0f, 90f, false)
            lineTo(arrowCenter + halfArrowBase, bodyBottom)
            lineTo(arrowCenter, height)
            lineTo(arrowCenter - halfArrowBase, bodyBottom)
            lineTo(radius, bodyBottom)
            arcTo(Rect(0f, bodyBottom - 2 * radius, 2 * radius, bodyBottom), 90f, 90f, false)
            lineTo(0f, radius)
            arcTo(Rect(0f, 0f, 2 * radius, 2 * radius), 180f, 90f, false)
            close()
        }

        TooltipPlacement.Bottom -> {
            val bodyTop = arrowDepth
            moveTo(arrowCenter - halfArrowBase, bodyTop)
            lineTo(arrowCenter, 0f)
            lineTo(arrowCenter + halfArrowBase, bodyTop)
            lineTo(width - radius, bodyTop)
            arcTo(Rect(width - 2 * radius, bodyTop, width, bodyTop + 2 * radius), -90f, 90f, false)
            lineTo(width, height - radius)
            arcTo(Rect(width - 2 * radius, height - 2 * radius, width, height), 0f, 90f, false)
            lineTo(radius, height)
            arcTo(Rect(0f, height - 2 * radius, 2 * radius, height), 90f, 90f, false)
            lineTo(0f, bodyTop + radius)
            arcTo(Rect(0f, bodyTop, 2 * radius, bodyTop + 2 * radius), 180f, 90f, false)
            close()
        }

        TooltipPlacement.Start -> {
            val bodyRight = width - arrowDepth
            moveTo(radius, 0f)
            lineTo(bodyRight - radius, 0f)
            arcTo(Rect(bodyRight - 2 * radius, 0f, bodyRight, 2 * radius), -90f, 90f, false)
            lineTo(bodyRight, arrowCenter - halfArrowBase)
            lineTo(width, arrowCenter)
            lineTo(bodyRight, arrowCenter + halfArrowBase)
            lineTo(bodyRight, height - radius)
            arcTo(Rect(bodyRight - 2 * radius, height - 2 * radius, bodyRight, height), 0f, 90f, false)
            lineTo(radius, height)
            arcTo(Rect(0f, height - 2 * radius, 2 * radius, height), 90f, 90f, false)
            lineTo(0f, radius)
            arcTo(Rect(0f, 0f, 2 * radius, 2 * radius), 180f, 90f, false)
            close()
        }

        TooltipPlacement.End -> {
            val bodyLeft = arrowDepth
            moveTo(bodyLeft + radius, 0f)
            lineTo(width - radius, 0f)
            arcTo(Rect(width - 2 * radius, 0f, width, 2 * radius), -90f, 90f, false)
            lineTo(width, height - radius)
            arcTo(Rect(width - 2 * radius, height - 2 * radius, width, height), 0f, 90f, false)
            lineTo(bodyLeft + radius, height)
            arcTo(Rect(bodyLeft, height - 2 * radius, bodyLeft + 2 * radius, height), 90f, 90f, false)
            lineTo(bodyLeft, arrowCenter + halfArrowBase)
            lineTo(0f, arrowCenter)
            lineTo(bodyLeft, arrowCenter - halfArrowBase)
            lineTo(bodyLeft, radius)
            arcTo(Rect(bodyLeft, 0f, bodyLeft + 2 * radius, 2 * radius), 180f, 90f, false)
            close()
        }
    }
}

internal fun tooltipPositionProvider(
    placement: TooltipPlacement,
    onArrowPositionResolved: (Float) -> Unit,
): PopupPositionProvider =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val centerX = anchorBounds.left + anchorBounds.width / 2
            val centerY = anchorBounds.top + anchorBounds.height / 2
            val desiredPosition = when (placement.toAbsolute(layoutDirection)) {
                TooltipPlacement.Top -> IntOffset(
                    x = centerX - popupContentSize.width / 2,
                    y = anchorBounds.top - popupContentSize.height,
                )
                TooltipPlacement.Bottom -> IntOffset(
                    x = centerX - popupContentSize.width / 2,
                    y = anchorBounds.bottom,
                )
                TooltipPlacement.Start -> IntOffset(
                    x = anchorBounds.left - popupContentSize.width,
                    y = centerY - popupContentSize.height / 2,
                )
                TooltipPlacement.End -> IntOffset(
                    x = anchorBounds.right,
                    y = centerY - popupContentSize.height / 2,
                )
            }
            val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
            val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
            val position = IntOffset(
                x = desiredPosition.x.coerceIn(0, maxX),
                y = desiredPosition.y.coerceIn(0, maxY),
            )
            val arrowPosition = when (placement) {
                TooltipPlacement.Top, TooltipPlacement.Bottom ->
                    anchorBounds.left + anchorBounds.width / 2f - position.x
                TooltipPlacement.Start, TooltipPlacement.End ->
                    anchorBounds.top + anchorBounds.height / 2f - position.y
            }
            onArrowPositionResolved(arrowPosition)
            return position
        }
    }

private fun TooltipPlacement.toAbsolute(layoutDirection: LayoutDirection): TooltipPlacement {
    return if (layoutDirection == LayoutDirection.Rtl) {
        when (this) {
            TooltipPlacement.Start -> TooltipPlacement.End
            TooltipPlacement.End -> TooltipPlacement.Start
            TooltipPlacement.Top, TooltipPlacement.Bottom -> this
        }
    } else {
        this
    }
}
