package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.`3DBridge`: ImageVector
    get() {
        if (_3DBridge != null) {
            return _3DBridge!!
        }
        _3DBridge = ImageVector.Builder(
            name = "3DBridge",
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
                moveTo(18f, 4f)
                horizontalLineTo(21f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 21f)
                curveTo(10.552f, 21f, 11f, 20.552f, 11f, 20f)
                curveTo(11f, 19.448f, 10.552f, 19f, 10f, 19f)
                curveTo(9.448f, 19f, 9f, 19.448f, 9f, 20f)
                curveTo(9f, 20.552f, 9.448f, 21f, 10f, 21f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 5f)
                curveTo(14.552f, 5f, 15f, 4.552f, 15f, 4f)
                curveTo(15f, 3.448f, 14.552f, 3f, 14f, 3f)
                curveTo(13.448f, 3f, 13f, 3.448f, 13f, 4f)
                curveTo(13f, 4.552f, 13.448f, 5f, 14f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 20f)
                curveTo(10f, 20f, 16.5f, 17.5f, 12f, 12f)
                curveTo(7.5f, 6.5f, 14f, 4f, 14f, 4f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 20f)
                horizontalLineTo(6f)
            }
        }.build()

        return _3DBridge!!
    }

@Suppress("ObjectPropertyName")
private var _3DBridge: ImageVector? = null
