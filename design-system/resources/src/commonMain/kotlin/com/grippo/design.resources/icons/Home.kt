package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Home: ImageVector
    get() {
        if (_Home != null) {
            return _Home!!
        }
        _Home = ImageVector.Builder(
            name = "Home",
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
                moveTo(3f, 9f)
                lineTo(12f, 2f)
                lineTo(21f, 9f)
                verticalLineTo(20f)
                curveTo(21f, 20.53f, 20.789f, 21.039f, 20.414f, 21.414f)
                curveTo(20.039f, 21.789f, 19.53f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(4.47f, 22f, 3.961f, 21.789f, 3.586f, 21.414f)
                curveTo(3.211f, 21.039f, 3f, 20.53f, 3f, 20f)
                verticalLineTo(9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 22f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                verticalLineTo(22f)
            }
        }.build()

        return _Home!!
    }

@Suppress("ObjectPropertyName")
private var _Home: ImageVector? = null
