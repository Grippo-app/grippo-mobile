package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BookOpen: ImageVector
    get() {
        if (_BookOpen != null) {
            return _BookOpen!!
        }
        _BookOpen = ImageVector.Builder(
            name = "BookOpen",
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
                moveTo(2f, 3f)
                horizontalLineTo(8f)
                curveTo(9.061f, 3f, 10.078f, 3.421f, 10.828f, 4.172f)
                curveTo(11.579f, 4.922f, 12f, 5.939f, 12f, 7f)
                verticalLineTo(21f)
                curveTo(12f, 20.204f, 11.684f, 19.441f, 11.121f, 18.879f)
                curveTo(10.559f, 18.316f, 9.796f, 18f, 9f, 18f)
                horizontalLineTo(2f)
                verticalLineTo(3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 3f)
                horizontalLineTo(16f)
                curveTo(14.939f, 3f, 13.922f, 3.421f, 13.172f, 4.172f)
                curveTo(12.421f, 4.922f, 12f, 5.939f, 12f, 7f)
                verticalLineTo(21f)
                curveTo(12f, 20.204f, 12.316f, 19.441f, 12.879f, 18.879f)
                curveTo(13.441f, 18.316f, 14.204f, 18f, 15f, 18f)
                horizontalLineTo(22f)
                verticalLineTo(3f)
                close()
            }
        }.build()

        return _BookOpen!!
    }

@Suppress("ObjectPropertyName")
private var _BookOpen: ImageVector? = null
