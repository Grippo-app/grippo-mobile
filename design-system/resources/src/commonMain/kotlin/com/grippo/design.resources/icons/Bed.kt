package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Bed: ImageVector
    get() {
        if (_Bed != null) {
            return _Bed!!
        }
        _Bed = ImageVector.Builder(
            name = "Bed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 4f)
                verticalLineTo(20f)
                curveTo(21f, 21.105f, 20.105f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.895f, 22f, 3f, 21.105f, 3f, 20f)
                verticalLineTo(4f)
                curveTo(3f, 2.895f, 3.895f, 2f, 5f, 2f)
                horizontalLineTo(19f)
                curveTo(20.105f, 2f, 21f, 2.895f, 21f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 8f)
                horizontalLineTo(11f)
                verticalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 8f)
                horizontalLineTo(13f)
                verticalLineTo(6f)
            }
        }.build()

        return _Bed!!
    }

@Suppress("ObjectPropertyName")
private var _Bed: ImageVector? = null
