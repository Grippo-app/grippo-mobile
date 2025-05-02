package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.UserX: ImageVector
    get() {
        if (_UserX != null) {
            return _UserX!!
        }
        _UserX = ImageVector.Builder(
            name = "UserX",
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
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 17.939f, 15.579f, 16.922f, 14.828f, 16.172f)
                curveTo(14.078f, 15.421f, 13.061f, 15f, 12f, 15f)
                horizontalLineTo(5f)
                curveTo(3.939f, 15f, 2.922f, 15.421f, 2.172f, 16.172f)
                curveTo(1.421f, 16.922f, 1f, 17.939f, 1f, 19f)
                verticalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 11f)
                curveTo(10.709f, 11f, 12.5f, 9.209f, 12.5f, 7f)
                curveTo(12.5f, 4.791f, 10.709f, 3f, 8.5f, 3f)
                curveTo(6.291f, 3f, 4.5f, 4.791f, 4.5f, 7f)
                curveTo(4.5f, 9.209f, 6.291f, 11f, 8.5f, 11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                lineTo(23f, 13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(23f, 8f)
                lineTo(18f, 13f)
            }
        }.build()

        return _UserX!!
    }

@Suppress("ObjectPropertyName")
private var _UserX: ImageVector? = null
