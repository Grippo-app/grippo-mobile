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
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18.01f)
                lineTo(6.01f, 17.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6.01f)
                lineTo(6.01f, 5.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 9.4f)
                verticalLineTo(2.6f)
                curveTo(2f, 2.269f, 2.269f, 2f, 2.6f, 2f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 2f, 22f, 2.269f, 22f, 2.6f)
                verticalLineTo(9.4f)
                curveTo(22f, 9.731f, 21.731f, 10f, 21.4f, 10f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 10f, 2f, 9.731f, 2f, 9.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2f, 21.4f)
                verticalLineTo(14.6f)
                curveTo(2f, 14.269f, 2.269f, 14f, 2.6f, 14f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 14f, 22f, 14.269f, 22f, 14.6f)
                verticalLineTo(21.4f)
                curveTo(22f, 21.731f, 21.731f, 22f, 21.4f, 22f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 22f, 2f, 21.731f, 2f, 21.4f)
                close()
            }
        }.build()

        return _Server!!
    }

@Suppress("ObjectPropertyName")
private var _Server: ImageVector? = null
