package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Calendar: ImageVector
    get() {
        if (_Calendar != null) {
            return _Calendar!!
        }
        _Calendar = ImageVector.Builder(
            name = "Calendar",
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
                moveTo(19f, 4f)
                horizontalLineTo(5f)
                curveTo(3.895f, 4f, 3f, 4.895f, 3f, 6f)
                verticalLineTo(20f)
                curveTo(3f, 21.105f, 3.895f, 22f, 5f, 22f)
                horizontalLineTo(19f)
                curveTo(20.105f, 22f, 21f, 21.105f, 21f, 20f)
                verticalLineTo(6f)
                curveTo(21f, 4.895f, 20.105f, 4f, 19f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 2f)
                verticalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 2f)
                verticalLineTo(6f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 10f)
                horizontalLineTo(21f)
            }
        }.build()

        return _Calendar!!
    }

@Suppress("ObjectPropertyName")
private var _Calendar: ImageVector? = null
