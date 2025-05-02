package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Server: ImageVector
    get() {
        if (_Server != null) {
            return _Server!!
        }
        _Server = ImageVector.Builder(
            name = "Server",
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
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveTo(2.895f, 2f, 2f, 2.895f, 2f, 4f)
                verticalLineTo(8f)
                curveTo(2f, 9.105f, 2.895f, 10f, 4f, 10f)
                horizontalLineTo(20f)
                curveTo(21.105f, 10f, 22f, 9.105f, 22f, 8f)
                verticalLineTo(4f)
                curveTo(22f, 2.895f, 21.105f, 2f, 20f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 14f)
                horizontalLineTo(4f)
                curveTo(2.895f, 14f, 2f, 14.895f, 2f, 16f)
                verticalLineTo(20f)
                curveTo(2f, 21.105f, 2.895f, 22f, 4f, 22f)
                horizontalLineTo(20f)
                curveTo(21.105f, 22f, 22f, 21.105f, 22f, 20f)
                verticalLineTo(16f)
                curveTo(22f, 14.895f, 21.105f, 14f, 20f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                horizontalLineTo(6.01f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18f)
                horizontalLineTo(6.01f)
            }
        }.build()

        return _Server!!
    }

@Suppress("ObjectPropertyName")
private var _Server: ImageVector? = null
