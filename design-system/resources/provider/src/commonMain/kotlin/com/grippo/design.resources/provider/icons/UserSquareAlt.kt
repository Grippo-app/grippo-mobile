package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.UserSquareAlt: ImageVector
    get() {
        if (_UserSquareAlt != null) {
            return _UserSquareAlt!!
        }
        _UserSquareAlt = ImageVector.Builder(
            name = "UserSquareAlt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 18f)
                verticalLineTo(17f)
                curveTo(7f, 14.239f, 9.239f, 12f, 12f, 12f)
                curveTo(14.761f, 12f, 17f, 14.239f, 17f, 17f)
                verticalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12f)
                curveTo(13.657f, 12f, 15f, 10.657f, 15f, 9f)
                curveTo(15f, 7.343f, 13.657f, 6f, 12f, 6f)
                curveTo(10.343f, 6f, 9f, 7.343f, 9f, 9f)
                curveTo(9f, 10.657f, 10.343f, 12f, 12f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                close()
            }
        }.build()

        return _UserSquareAlt!!
    }

@Suppress("ObjectPropertyName")
private var _UserSquareAlt: ImageVector? = null
