package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Asana: ImageVector
    get() {
        if (_Asana != null) {
            return _Asana!!
        }
        _Asana = ImageVector.Builder(
            name = "Asana",
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
                moveTo(12f, 11.5f)
                curveTo(14.209f, 11.5f, 16f, 9.709f, 16f, 7.5f)
                curveTo(16f, 5.291f, 14.209f, 3.5f, 12f, 3.5f)
                curveTo(9.791f, 3.5f, 8f, 5.291f, 8f, 7.5f)
                curveTo(8f, 9.709f, 9.791f, 11.5f, 12f, 11.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 20.5f)
                curveTo(9.209f, 20.5f, 11f, 18.709f, 11f, 16.5f)
                curveTo(11f, 14.291f, 9.209f, 12.5f, 7f, 12.5f)
                curveTo(4.791f, 12.5f, 3f, 14.291f, 3f, 16.5f)
                curveTo(3f, 18.709f, 4.791f, 20.5f, 7f, 20.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 20.5f)
                curveTo(19.209f, 20.5f, 21f, 18.709f, 21f, 16.5f)
                curveTo(21f, 14.291f, 19.209f, 12.5f, 17f, 12.5f)
                curveTo(14.791f, 12.5f, 13f, 14.291f, 13f, 16.5f)
                curveTo(13f, 18.709f, 14.791f, 20.5f, 17f, 20.5f)
                close()
            }
        }.build()

        return _Asana!!
    }

@Suppress("ObjectPropertyName")
private var _Asana: ImageVector? = null
