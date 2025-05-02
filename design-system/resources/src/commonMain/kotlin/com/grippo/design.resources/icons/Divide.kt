package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Divide: ImageVector
    get() {
        if (_Divide != null) {
            return _Divide!!
        }
        _Divide = ImageVector.Builder(
            name = "Divide",
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
                moveTo(12f, 8f)
                curveTo(13.105f, 8f, 14f, 7.105f, 14f, 6f)
                curveTo(14f, 4.895f, 13.105f, 4f, 12f, 4f)
                curveTo(10.895f, 4f, 10f, 4.895f, 10f, 6f)
                curveTo(10f, 7.105f, 10.895f, 8f, 12f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 20f)
                curveTo(13.105f, 20f, 14f, 19.105f, 14f, 18f)
                curveTo(14f, 16.895f, 13.105f, 16f, 12f, 16f)
                curveTo(10.895f, 16f, 10f, 16.895f, 10f, 18f)
                curveTo(10f, 19.105f, 10.895f, 20f, 12f, 20f)
                close()
            }
        }.build()

        return _Divide!!
    }

@Suppress("ObjectPropertyName")
private var _Divide: ImageVector? = null
