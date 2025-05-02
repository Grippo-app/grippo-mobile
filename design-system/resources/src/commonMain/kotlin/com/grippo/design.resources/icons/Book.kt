package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Book: ImageVector
    get() {
        if (_Book != null) {
            return _Book!!
        }
        _Book = ImageVector.Builder(
            name = "Book",
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
                moveTo(4f, 19.5f)
                curveTo(4f, 18.837f, 4.263f, 18.201f, 4.732f, 17.732f)
                curveTo(5.201f, 17.263f, 5.837f, 17f, 6.5f, 17f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 2f)
                horizontalLineTo(20f)
                verticalLineTo(22f)
                horizontalLineTo(6.5f)
                curveTo(5.837f, 22f, 5.201f, 21.737f, 4.732f, 21.268f)
                curveTo(4.263f, 20.799f, 4f, 20.163f, 4f, 19.5f)
                verticalLineTo(4.5f)
                curveTo(4f, 3.837f, 4.263f, 3.201f, 4.732f, 2.732f)
                curveTo(5.201f, 2.263f, 5.837f, 2f, 6.5f, 2f)
                verticalLineTo(2f)
                close()
            }
        }.build()

        return _Book!!
    }

@Suppress("ObjectPropertyName")
private var _Book: ImageVector? = null
