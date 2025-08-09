package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.UserCircleAlt: ImageVector
    get() {
        if (_UserCircleAlt != null) {
            return _UserCircleAlt!!
        }
        _UserCircleAlt = ImageVector.Builder(
            name = "UserCircleAlt",
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
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _UserCircleAlt!!
    }

@Suppress("ObjectPropertyName")
private var _UserCircleAlt: ImageVector? = null
