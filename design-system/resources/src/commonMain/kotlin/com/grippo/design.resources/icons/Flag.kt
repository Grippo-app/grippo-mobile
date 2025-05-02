package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Flag: ImageVector
    get() {
        if (_Flag != null) {
            return _Flag!!
        }
        _Flag = ImageVector.Builder(
            name = "Flag",
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
                moveTo(4f, 15f)
                curveTo(4f, 15f, 5f, 14f, 8f, 14f)
                curveTo(11f, 14f, 13f, 16f, 16f, 16f)
                curveTo(19f, 16f, 20f, 15f, 20f, 15f)
                verticalLineTo(3f)
                curveTo(20f, 3f, 19f, 4f, 16f, 4f)
                curveTo(13f, 4f, 11f, 2f, 8f, 2f)
                curveTo(5f, 2f, 4f, 3f, 4f, 3f)
                verticalLineTo(15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 22f)
                verticalLineTo(15f)
            }
        }.build()

        return _Flag!!
    }

@Suppress("ObjectPropertyName")
private var _Flag: ImageVector? = null
