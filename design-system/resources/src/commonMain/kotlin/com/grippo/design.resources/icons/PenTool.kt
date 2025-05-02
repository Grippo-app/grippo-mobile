package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PenTool: ImageVector
    get() {
        if (_PenTool != null) {
            return _PenTool!!
        }
        _PenTool = ImageVector.Builder(
            name = "PenTool",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 19f)
                lineTo(19f, 12f)
                lineTo(22f, 15f)
                lineTo(15f, 22f)
                lineTo(12f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 13f)
                lineTo(16.5f, 5.5f)
                lineTo(2f, 2f)
                lineTo(5.5f, 16.5f)
                lineTo(13f, 18f)
                lineTo(18f, 13f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 2f)
                lineTo(9.586f, 9.586f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 13f)
                curveTo(12.105f, 13f, 13f, 12.105f, 13f, 11f)
                curveTo(13f, 9.895f, 12.105f, 9f, 11f, 9f)
                curveTo(9.895f, 9f, 9f, 9.895f, 9f, 11f)
                curveTo(9f, 12.105f, 9.895f, 13f, 11f, 13f)
                close()
            }
        }.build()

        return _PenTool!!
    }

@Suppress("ObjectPropertyName")
private var _PenTool: ImageVector? = null
