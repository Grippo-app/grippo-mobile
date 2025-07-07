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

public val AppIcon.Calculator: ImageVector
    get() {
        if (_Calculator != null) {
            return _Calculator!!
        }
        _Calculator = ImageVector.Builder(
            name = "Calculator",
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
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f
                ) {
                    moveTo(1f, 21f)
                    verticalLineTo(3f)
                    curveTo(1f, 1.895f, 1.895f, 1f, 3f, 1f)
                    horizontalLineTo(21f)
                    curveTo(22.105f, 1f, 23f, 1.895f, 23f, 3f)
                    verticalLineTo(21f)
                    curveTo(23f, 22.105f, 22.105f, 23f, 21f, 23f)
                    horizontalLineTo(3f)
                    curveTo(1.895f, 23f, 1f, 22.105f, 1f, 21f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(15f, 7f)
                    horizontalLineTo(17f)
                    horizontalLineTo(19f)
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(15f, 15.5f)
                    horizontalLineTo(17f)
                    horizontalLineTo(19f)
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(15f, 18.5f)
                    horizontalLineTo(17f)
                    horizontalLineTo(19f)
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(7f, 7f)
                    verticalLineTo(9f)
                    moveTo(5f, 7f)
                    horizontalLineTo(7f)
                    horizontalLineTo(5f)
                    close()
                    moveTo(9f, 7f)
                    horizontalLineTo(7f)
                    horizontalLineTo(9f)
                    close()
                    moveTo(7f, 7f)
                    verticalLineTo(5f)
                    verticalLineTo(7f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(7f, 17f)
                    lineTo(8.415f, 18.414f)
                    moveTo(5.586f, 18.414f)
                    lineTo(7f, 17f)
                    lineTo(5.586f, 18.414f)
                    close()
                    moveTo(8.415f, 15.586f)
                    lineTo(7f, 17f)
                    lineTo(8.415f, 15.586f)
                    close()
                    moveTo(7f, 17f)
                    lineTo(5.586f, 15.586f)
                    lineTo(7f, 17f)
                    close()
                }
            }
        }.build()

        return _Calculator!!
    }

@Suppress("ObjectPropertyName")
private var _Calculator: ImageVector? = null
