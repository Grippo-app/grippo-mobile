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
            background = AppTokens.colors.semantic.success.copy(alpha = 0.15f),
            border = AppTokens.colors.semantic.success.copy(alpha = 0.40f),
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.success,
        )

        TooltipVariant.Error -> TooltipColorTokens(
            background = AppTokens.colors.semantic.error.copy(alpha = 0.15f),
            border = AppTokens.colors.semantic.error.copy(alpha = 0.40f),
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.error,
        )

        TooltipVariant.Warning -> TooltipColorTokens(
            background = AppTokens.colors.semantic.warning.copy(alpha = 0.15f),
            border = AppTokens.colors.semantic.warning.copy(alpha = 0.40f),
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.warning,
        )

        TooltipVariant.Info -> TooltipColorTokens(
            background = AppTokens.colors.semantic.info.copy(alpha = 0.10f),
            border = AppTokens.colors.semantic.info.copy(alpha = 0.30f),
            text = AppTokens.colors.text.primary,
            subtext = AppTokens.colors.text.secondary,
            icon = AppTokens.colors.semantic.info,
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Unified bubble + arrow GenericShape
//
// Single continuous path: rounded rect + directional arrow.
// Border and fill share the same shape — no seam, no gap.
//
//  aD  = arrow depth  (protrusion from the edge, px)
//  aHB = half arrow base (px)
//  r   = corner radius (px)
// ─────────────────────────────────────────────────────────────

internal fun tooltipShape(
    placement: TooltipPlacement,
    arrowDepthPx: Float,
    arrowBasePx: Float,
    cornerRadiusPx: Float,
): GenericShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val aD = arrowDepthPx
    val aHB = arrowBasePx / 2f
    val r = cornerRadiusPx
    val cx = w / 2f
    val cy = h / 2f

    when (placement) {
        // Arrow at bottom, tip pointing DOWN
        TooltipPlacement.Top -> {
            val bH = h - aD
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
            lineTo(w, bH - r)
            arcTo(Rect(w - 2 * r, bH - 2 * r, w, bH), 0f, 90f, false)
            lineTo(cx + aHB, bH)
            lineTo(cx, h)
            lineTo(cx - aHB, bH)
            lineTo(r, bH)
            arcTo(Rect(0f, bH - 2 * r, 2 * r, bH), 90f, 90f, false)
            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            close()
        }

        // Arrow at top, tip pointing UP
        TooltipPlacement.Bottom -> {
            val bY = aD
            moveTo(cx - aHB, bY)
            lineTo(cx, 0f)
            lineTo(cx + aHB, bY)
            lineTo(w - r, bY)
            arcTo(Rect(w - 2 * r, bY, w, bY + 2 * r), -90f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            lineTo(0f, bY + r)
            arcTo(Rect(0f, bY, 2 * r, bY + 2 * r), 180f, 90f, false)
            close()
        }

        // Arrow at right, tip pointing RIGHT
        TooltipPlacement.Start -> {
            val bW = w - aD
            moveTo(r, 0f)
            lineTo(bW - r, 0f)
            arcTo(Rect(bW - 2 * r, 0f, bW, 2 * r), -90f, 90f, false)
            lineTo(bW, cy - aHB)
            lineTo(w, cy)
            lineTo(bW, cy + aHB)
            lineTo(bW, h - r)
            arcTo(Rect(bW - 2 * r, h - 2 * r, bW, h), 0f, 90f, false)
            lineTo(r, h)
            arcTo(Rect(0f, h - 2 * r, 2 * r, h), 90f, 90f, false)
            lineTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            close()
        }

        // Arrow at left, tip pointing LEFT
        TooltipPlacement.End -> {
            val bX = aD
            moveTo(bX + r, 0f)
            lineTo(w - r, 0f)
            arcTo(Rect(w - 2 * r, 0f, w, 2 * r), -90f, 90f, false)
            lineTo(w, h - r)
            arcTo(Rect(w - 2 * r, h - 2 * r, w, h), 0f, 90f, false)
            lineTo(bX + r, h)
            arcTo(Rect(bX, h - 2 * r, bX + 2 * r, h), 90f, 90f, false)
            lineTo(bX, cy + aHB)
            lineTo(0f, cy)
            lineTo(bX, cy - aHB)
            lineTo(bX, r)
            arcTo(Rect(bX, 0f, bX + 2 * r, 2 * r), 180f, 90f, false)
            close()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Popup position provider
//
// Positions the tooltip popup relative to the anchor, with
// a small gap offset so the arrow tip touches the anchor edge.
// ─────────────────────────────────────────────────────────────

internal fun tooltipPositionProvider(placement: TooltipPlacement): PopupPositionProvider =
    object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
        ): IntOffset {
            val cx = anchorBounds.left + anchorBounds.width / 2
            val cy = anchorBounds.top + anchorBounds.height / 2
            return when (placement) {
                TooltipPlacement.Top -> IntOffset(
                    x = (cx - popupContentSize.width / 2).coerceIn(
                        0,
                        windowSize.width - popupContentSize.width
                    ),
                    y = anchorBounds.top - popupContentSize.height,
                )

                TooltipPlacement.Bottom -> IntOffset(
                    x = (cx - popupContentSize.width / 2).coerceIn(
                        0,
                        windowSize.width - popupContentSize.width
                    ),
                    y = anchorBounds.bottom,
                )

                TooltipPlacement.Start -> IntOffset(
                    x = anchorBounds.left - popupContentSize.width,
                    y = (cy - popupContentSize.height / 2).coerceIn(
                        0,
                        windowSize.height - popupContentSize.height
                    ),
                )

                TooltipPlacement.End -> IntOffset(
                    x = anchorBounds.right,
                    y = (cy - popupContentSize.height / 2).coerceIn(
                        0,
                        windowSize.height - popupContentSize.height
                    ),
                )
            }
        }
    }
