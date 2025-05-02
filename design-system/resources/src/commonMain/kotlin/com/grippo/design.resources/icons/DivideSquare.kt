package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.DivideSquare: ImageVector
    get() {
        if (_DivideSquare != null) {
            return _DivideSquare!!
        }
        _DivideSquare = ImageVector.Builder(
            name = "DivideSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19f, 3f)
                    horizontalLineTo(5f)
                    curveTo(3.895f, 3f, 3f, 3.895f, 3f, 5f)
                    verticalLineTo(19f)
                    curveTo(3f, 20.105f, 3.895f, 21f, 5f, 21f)
                    horizontalLineTo(19f)
                    curveTo(20.105f, 21f, 21f, 20.105f, 21f, 19f)
                    verticalLineTo(5f)
                    curveTo(21f, 3.895f, 20.105f, 3f, 19f, 3f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(8f, 12f)
                    horizontalLineTo(16f)
                }
            }
        }.build()

        return _DivideSquare!!
    }

@Suppress("ObjectPropertyName")
private var _DivideSquare: ImageVector? = null
