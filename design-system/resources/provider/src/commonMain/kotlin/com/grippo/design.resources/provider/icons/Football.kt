package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Football: ImageVector
    get() {
        if (_Football != null) {
            return _Football!!
        }
        _Football = ImageVector.Builder(
            name = "Football",
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
                moveTo(12f, 13.828f)
                verticalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 3f)
                verticalLineTo(13.828f)
                horizontalLineTo(19f)
                verticalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 6.732f)
                curveTo(14.071f, 6.114f, 14.434f, 4.618f, 14.549f, 3.899f)
                curveTo(14.587f, 3.659f, 14.455f, 3.43f, 14.228f, 3.343f)
                curveTo(13.547f, 3.083f, 12.071f, 2.65f, 11f, 3.268f)
                curveTo(9.929f, 3.886f, 9.567f, 5.381f, 9.452f, 6.1f)
                curveTo(9.413f, 6.341f, 9.545f, 6.57f, 9.773f, 6.656f)
                curveTo(10.453f, 6.916f, 11.929f, 7.35f, 13f, 6.732f)
                close()
            }
        }.build()

        return _Football!!
    }

@Suppress("ObjectPropertyName")
private var _Football: ImageVector? = null
