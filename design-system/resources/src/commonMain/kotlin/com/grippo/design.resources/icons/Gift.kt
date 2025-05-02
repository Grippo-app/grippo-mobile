package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Gift: ImageVector
    get() {
        if (_Gift != null) {
            return _Gift!!
        }
        _Gift = ImageVector.Builder(
            name = "Gift",
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
                moveTo(20f, 12f)
                verticalLineTo(22f)
                horizontalLineTo(4f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 7f)
                horizontalLineTo(2f)
                verticalLineTo(12f)
                horizontalLineTo(22f)
                verticalLineTo(7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                verticalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 7f)
                horizontalLineTo(7.5f)
                curveTo(6.837f, 7f, 6.201f, 6.737f, 5.732f, 6.268f)
                curveTo(5.263f, 5.799f, 5f, 5.163f, 5f, 4.5f)
                curveTo(5f, 3.837f, 5.263f, 3.201f, 5.732f, 2.732f)
                curveTo(6.201f, 2.263f, 6.837f, 2f, 7.5f, 2f)
                curveTo(11f, 2f, 12f, 7f, 12f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 7f)
                horizontalLineTo(16.5f)
                curveTo(17.163f, 7f, 17.799f, 6.737f, 18.268f, 6.268f)
                curveTo(18.737f, 5.799f, 19f, 5.163f, 19f, 4.5f)
                curveTo(19f, 3.837f, 18.737f, 3.201f, 18.268f, 2.732f)
                curveTo(17.799f, 2.263f, 17.163f, 2f, 16.5f, 2f)
                curveTo(13f, 2f, 12f, 7f, 12f, 7f)
                close()
            }
        }.build()

        return _Gift!!
    }

@Suppress("ObjectPropertyName")
private var _Gift: ImageVector? = null
