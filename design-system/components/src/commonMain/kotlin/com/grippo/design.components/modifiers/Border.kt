package com.grippo.design.components.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.grippo.design.components.modifiers.models.Side
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Deprecated("avoid borders in project")
public fun Modifier.border(
    width: Dp,
    color: Color,
    shape: Shape = RectangleShape,
    sides: ImmutableList<Side> = Side.entries.toPersistentList(),
): Modifier = this.then(
    Modifier.drawWithContent {
        drawContent()

        val strokeWidthPx = width.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        val roundRect = (outline as? Outline.Rounded)?.roundRect

        if (roundRect != null) {
            if (Side.TOP in sides) {
                drawLine(
                    color = color,
                    start = Offset(roundRect.left + roundRect.topLeftCornerRadius.x, roundRect.top),
                    end = Offset(roundRect.right - roundRect.topRightCornerRadius.x, roundRect.top),
                    strokeWidth = strokeWidthPx
                )
            }
            if (Side.BOTTOM in sides) {
                drawLine(
                    color = color,
                    start = Offset(
                        roundRect.left + roundRect.bottomLeftCornerRadius.x,
                        roundRect.bottom
                    ),
                    end = Offset(
                        roundRect.right - roundRect.bottomRightCornerRadius.x,
                        roundRect.bottom
                    ),
                    strokeWidth = strokeWidthPx
                )
            }
            if (Side.LEFT in sides) {
                drawLine(
                    color = color,
                    start = Offset(roundRect.left, roundRect.top + roundRect.topLeftCornerRadius.y),
                    end = Offset(
                        roundRect.left,
                        roundRect.bottom - roundRect.bottomLeftCornerRadius.y
                    ),
                    strokeWidth = strokeWidthPx
                )
            }
            if (Side.RIGHT in sides) {
                drawLine(
                    color = color,
                    start = Offset(
                        roundRect.right,
                        roundRect.top + roundRect.topRightCornerRadius.y
                    ),
                    end = Offset(
                        roundRect.right,
                        roundRect.bottom - roundRect.bottomRightCornerRadius.y
                    ),
                    strokeWidth = strokeWidthPx
                )
            }

            if (Side.TOP in sides && Side.LEFT in sides && roundRect.topLeftCornerRadius.x > 0f) {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(roundRect.left, roundRect.top),
                    size = Size(
                        width = roundRect.topLeftCornerRadius.x * 2f,
                        height = roundRect.topLeftCornerRadius.y * 2f
                    ),
                    style = Stroke(width = strokeWidthPx)
                )
            }

            if (Side.TOP in sides && Side.RIGHT in sides && roundRect.topRightCornerRadius.x > 0f) {
                drawArc(
                    color = color,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(
                        roundRect.right - roundRect.topRightCornerRadius.x * 2f,
                        roundRect.top
                    ),
                    size = Size(
                        width = roundRect.topRightCornerRadius.x * 2f,
                        height = roundRect.topRightCornerRadius.y * 2f
                    ),
                    style = Stroke(width = strokeWidthPx)
                )
            }

            if (Side.BOTTOM in sides && Side.LEFT in sides && roundRect.bottomLeftCornerRadius.x > 0f) {
                drawArc(
                    color = color,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(
                        roundRect.left,
                        roundRect.bottom - roundRect.bottomLeftCornerRadius.y * 2f
                    ),
                    size = Size(
                        width = roundRect.bottomLeftCornerRadius.x * 2f,
                        height = roundRect.bottomLeftCornerRadius.y * 2f
                    ),
                    style = Stroke(width = strokeWidthPx)
                )
            }

            if (Side.BOTTOM in sides && Side.RIGHT in sides && roundRect.bottomRightCornerRadius.x > 0f) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(
                        roundRect.right - roundRect.bottomRightCornerRadius.x * 2f,
                        roundRect.bottom - roundRect.bottomRightCornerRadius.y * 2f
                    ),
                    size = Size(
                        width = roundRect.bottomRightCornerRadius.x * 2f,
                        height = roundRect.bottomRightCornerRadius.y * 2f
                    ),
                    style = Stroke(width = strokeWidthPx)
                )
            }

        } else if (outline is Outline.Rectangle) {
            if (Side.TOP in sides) {
                drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = strokeWidthPx)
            }
            if (Side.BOTTOM in sides) {
                drawLine(
                    color,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    strokeWidth = strokeWidthPx
                )
            }
            if (Side.LEFT in sides) {
                drawLine(
                    color,
                    Offset(0f, 0f),
                    Offset(0f, size.height),
                    strokeWidth = strokeWidthPx
                )
            }
            if (Side.RIGHT in sides) {
                drawLine(
                    color,
                    Offset(size.width, 0f),
                    Offset(size.width, size.height),
                    strokeWidth = strokeWidthPx
                )
            }
        }
    }
)