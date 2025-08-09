package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.BookmarkEmpty: ImageVector
    get() {
        if (_BookmarkEmpty != null) {
            return _BookmarkEmpty!!
        }
        _BookmarkEmpty = ImageVector.Builder(
            name = "BookmarkEmpty",
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
                moveTo(5f, 21f)
                verticalLineTo(5f)
                curveTo(5f, 3.895f, 5.895f, 3f, 7f, 3f)
                horizontalLineTo(17f)
                curveTo(18.105f, 3f, 19f, 3.895f, 19f, 5f)
                verticalLineTo(21f)
                lineTo(13.082f, 17.195f)
                curveTo(12.423f, 16.772f, 11.577f, 16.772f, 10.918f, 17.195f)
                lineTo(5f, 21f)
                close()
            }
        }.build()

        return _BookmarkEmpty!!
    }

@Suppress("ObjectPropertyName")
private var _BookmarkEmpty: ImageVector? = null
