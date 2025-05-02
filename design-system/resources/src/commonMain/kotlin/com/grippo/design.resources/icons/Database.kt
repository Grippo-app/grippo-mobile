package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Database: ImageVector
    get() {
        if (_Database != null) {
            return _Database!!
        }
        _Database = ImageVector.Builder(
            name = "Database",
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
                curveTo(16.971f, 8f, 21f, 6.657f, 21f, 5f)
                curveTo(21f, 3.343f, 16.971f, 2f, 12f, 2f)
                curveTo(7.029f, 2f, 3f, 3.343f, 3f, 5f)
                curveTo(3f, 6.657f, 7.029f, 8f, 12f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 13.66f, 17f, 15f, 12f, 15f)
                curveTo(7f, 15f, 3f, 13.66f, 3f, 12f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.66f, 7f, 22f, 12f, 22f)
                curveTo(17f, 22f, 21f, 20.66f, 21f, 19f)
                verticalLineTo(5f)
            }
        }.build()

        return _Database!!
    }

@Suppress("ObjectPropertyName")
private var _Database: ImageVector? = null
