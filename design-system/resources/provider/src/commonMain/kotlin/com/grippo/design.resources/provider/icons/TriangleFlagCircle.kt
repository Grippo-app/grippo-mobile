package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.TriangleFlagCircle: ImageVector
    get() {
        if (_TriangleFlagCircle != null) {
            return _TriangleFlagCircle!!
        }
        _TriangleFlagCircle = ImageVector.Builder(
            name = "TriangleFlagCircle",
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
                moveTo(9f, 21.5f)
                verticalLineTo(15.5f)
                verticalLineTo(21.5f)
                close()
                moveTo(9f, 15.5f)
                verticalLineTo(6.997f)
                curveTo(9f, 6.544f, 9.481f, 6.255f, 9.881f, 6.466f)
                lineTo(16.551f, 9.997f)
                curveTo(16.965f, 10.217f, 16.979f, 10.807f, 16.574f, 11.045f)
                lineTo(9f, 15.5f)
                close()
                moveTo(22f, 12f)
                curveTo(22f, 17.523f, 17.523f, 22f, 12f, 22f)
                curveTo(6.477f, 22f, 2f, 17.523f, 2f, 12f)
                curveTo(2f, 6.477f, 6.477f, 2f, 12f, 2f)
                curveTo(17.523f, 2f, 22f, 6.477f, 22f, 12f)
                close()
            }
        }.build()

        return _TriangleFlagCircle!!
    }

@Suppress("ObjectPropertyName")
private var _TriangleFlagCircle: ImageVector? = null
