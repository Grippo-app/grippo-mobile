package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.GoogleHome: ImageVector
    get() {
        if (_GoogleHome != null) {
            return _GoogleHome!!
        }
        _GoogleHome = ImageVector.Builder(
            name = "GoogleHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(17.708f, 17f)
                horizontalLineTo(19.5f)
                curveTo(20.881f, 17f, 22f, 18.119f, 22f, 19.5f)
                curveTo(22f, 20.881f, 20.881f, 22f, 19.5f, 22f)
                horizontalLineTo(17f)
                moveTo(17.708f, 17f)
                curveTo(19.133f, 15.408f, 20f, 13.305f, 20f, 11f)
                curveTo(20f, 6.029f, 15.971f, 2f, 11f, 2f)
                curveTo(6.029f, 2f, 2f, 6.029f, 2f, 11f)
                curveTo(2f, 15.971f, 6.029f, 20f, 11f, 20f)
                curveTo(13.665f, 20f, 16.06f, 18.841f, 17.708f, 17f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 11.01f)
                lineTo(11.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 11.01f)
                lineTo(8.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 11.01f)
                lineTo(14.01f, 10.999f)
            }
        }.build()

        return _GoogleHome!!
    }

@Suppress("ObjectPropertyName")
private var _GoogleHome: ImageVector? = null
