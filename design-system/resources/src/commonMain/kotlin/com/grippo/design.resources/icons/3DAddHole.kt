package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.`3DAddHole`: ImageVector
    get() {
        if (_3DAddHole != null) {
            return _3DAddHole!!
        }
        _3DAddHole = ImageVector.Builder(
            name = "3DAddHole",
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
                moveTo(12f, 16f)
                curveTo(14.209f, 16f, 16f, 14.209f, 16f, 12f)
                curveTo(16f, 9.791f, 14.209f, 8f, 12f, 8f)
                curveTo(9.791f, 8f, 8f, 9.791f, 8f, 12f)
                curveTo(8f, 14.209f, 9.791f, 16f, 12f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 7.353f)
                verticalLineTo(16.647f)
                curveTo(21f, 16.865f, 20.882f, 17.066f, 20.691f, 17.171f)
                lineTo(12.291f, 21.838f)
                curveTo(12.11f, 21.939f, 11.89f, 21.939f, 11.709f, 21.838f)
                lineTo(3.309f, 17.171f)
                curveTo(3.118f, 17.066f, 3f, 16.865f, 3f, 16.647f)
                lineTo(3f, 7.353f)
                curveTo(3f, 7.135f, 3.118f, 6.934f, 3.309f, 6.829f)
                lineTo(11.709f, 2.162f)
                curveTo(11.89f, 2.061f, 12.11f, 2.061f, 12.291f, 2.162f)
                lineTo(20.691f, 6.829f)
                curveTo(20.882f, 6.934f, 21f, 7.135f, 21f, 7.353f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 7.278f)
                lineTo(15.6f, 10f)
                moveTo(3.528f, 7.294f)
                lineTo(8.4f, 10f)
                lineTo(3.528f, 7.294f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21f)
                verticalLineTo(16f)
            }
        }.build()

        return _3DAddHole!!
    }

@Suppress("ObjectPropertyName")
private var _3DAddHole: ImageVector? = null
