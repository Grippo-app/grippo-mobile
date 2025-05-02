package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Briefcase: ImageVector
    get() {
        if (_Briefcase != null) {
            return _Briefcase!!
        }
        _Briefcase = ImageVector.Builder(
            name = "Briefcase",
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
                moveTo(20f, 7f)
                horizontalLineTo(4f)
                curveTo(2.895f, 7f, 2f, 7.895f, 2f, 9f)
                verticalLineTo(19f)
                curveTo(2f, 20.105f, 2.895f, 21f, 4f, 21f)
                horizontalLineTo(20f)
                curveTo(21.105f, 21f, 22f, 20.105f, 22f, 19f)
                verticalLineTo(9f)
                curveTo(22f, 7.895f, 21.105f, 7f, 20f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 21f)
                verticalLineTo(5f)
                curveTo(16f, 4.47f, 15.789f, 3.961f, 15.414f, 3.586f)
                curveTo(15.039f, 3.211f, 14.53f, 3f, 14f, 3f)
                horizontalLineTo(10f)
                curveTo(9.47f, 3f, 8.961f, 3.211f, 8.586f, 3.586f)
                curveTo(8.211f, 3.961f, 8f, 4.47f, 8f, 5f)
                verticalLineTo(21f)
            }
        }.build()

        return _Briefcase!!
    }

@Suppress("ObjectPropertyName")
private var _Briefcase: ImageVector? = null
