package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Speaker: ImageVector
    get() {
        if (_Speaker != null) {
            return _Speaker!!
        }
        _Speaker = ImageVector.Builder(
            name = "Speaker",
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
                moveTo(18f, 2f)
                horizontalLineTo(6f)
                curveTo(4.895f, 2f, 4f, 2.895f, 4f, 4f)
                verticalLineTo(20f)
                curveTo(4f, 21.105f, 4.895f, 22f, 6f, 22f)
                horizontalLineTo(18f)
                curveTo(19.105f, 22f, 20f, 21.105f, 20f, 20f)
                verticalLineTo(4f)
                curveTo(20f, 2.895f, 19.105f, 2f, 18f, 2f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                curveTo(14.209f, 18f, 16f, 16.209f, 16f, 14f)
                curveTo(16f, 11.791f, 14.209f, 10f, 12f, 10f)
                curveTo(9.791f, 10f, 8f, 11.791f, 8f, 14f)
                curveTo(8f, 16.209f, 9.791f, 18f, 12f, 18f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6f)
                horizontalLineTo(12.01f)
            }
        }.build()

        return _Speaker!!
    }

@Suppress("ObjectPropertyName")
private var _Speaker: ImageVector? = null
