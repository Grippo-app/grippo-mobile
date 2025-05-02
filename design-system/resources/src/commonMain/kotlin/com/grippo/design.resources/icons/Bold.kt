package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Bold: ImageVector
    get() {
        if (_Bold != null) {
            return _Bold!!
        }
        _Bold = ImageVector.Builder(
            name = "Bold",
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
                moveTo(6f, 4f)
                horizontalLineTo(14f)
                curveTo(15.061f, 4f, 16.078f, 4.421f, 16.828f, 5.172f)
                curveTo(17.579f, 5.922f, 18f, 6.939f, 18f, 8f)
                curveTo(18f, 9.061f, 17.579f, 10.078f, 16.828f, 10.828f)
                curveTo(16.078f, 11.579f, 15.061f, 12f, 14f, 12f)
                horizontalLineTo(6f)
                verticalLineTo(4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 12f)
                horizontalLineTo(15f)
                curveTo(16.061f, 12f, 17.078f, 12.421f, 17.828f, 13.172f)
                curveTo(18.579f, 13.922f, 19f, 14.939f, 19f, 16f)
                curveTo(19f, 17.061f, 18.579f, 18.078f, 17.828f, 18.828f)
                curveTo(17.078f, 19.579f, 16.061f, 20f, 15f, 20f)
                horizontalLineTo(6f)
                verticalLineTo(12f)
                close()
            }
        }.build()

        return _Bold!!
    }

@Suppress("ObjectPropertyName")
private var _Bold: ImageVector? = null
