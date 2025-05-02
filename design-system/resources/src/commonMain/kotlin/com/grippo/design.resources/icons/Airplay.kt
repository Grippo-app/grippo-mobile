package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Airplay: ImageVector
    get() {
        if (_Airplay != null) {
            return _Airplay!!
        }
        _Airplay = ImageVector.Builder(
            name = "Airplay",
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
                moveTo(5f, 17f)
                horizontalLineTo(4f)
                curveTo(3.47f, 17f, 2.961f, 16.789f, 2.586f, 16.414f)
                curveTo(2.211f, 16.039f, 2f, 15.53f, 2f, 15f)
                verticalLineTo(5f)
                curveTo(2f, 4.47f, 2.211f, 3.961f, 2.586f, 3.586f)
                curveTo(2.961f, 3.211f, 3.47f, 3f, 4f, 3f)
                horizontalLineTo(20f)
                curveTo(20.53f, 3f, 21.039f, 3.211f, 21.414f, 3.586f)
                curveTo(21.789f, 3.961f, 22f, 4.47f, 22f, 5f)
                verticalLineTo(15f)
                curveTo(22f, 15.53f, 21.789f, 16.039f, 21.414f, 16.414f)
                curveTo(21.039f, 16.789f, 20.53f, 17f, 20f, 17f)
                horizontalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                lineTo(17f, 21f)
                horizontalLineTo(7f)
                lineTo(12f, 15f)
                close()
            }
        }.build()

        return _Airplay!!
    }

@Suppress("ObjectPropertyName")
private var _Airplay: ImageVector? = null
