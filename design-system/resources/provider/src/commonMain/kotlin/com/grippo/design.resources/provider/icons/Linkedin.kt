package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Linkedin: ImageVector
    get() {
        if (_Linkedin != null) {
            return _Linkedin!!
        }
        _Linkedin = ImageVector.Builder(
            name = "Linkedin",
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
                moveTo(21f, 8f)
                verticalLineTo(16f)
                curveTo(21f, 18.761f, 18.761f, 21f, 16f, 21f)
                horizontalLineTo(8f)
                curveTo(5.239f, 21f, 3f, 18.761f, 3f, 16f)
                verticalLineTo(8f)
                curveTo(3f, 5.239f, 5.239f, 3f, 8f, 3f)
                horizontalLineTo(16f)
                curveTo(18.761f, 3f, 21f, 5.239f, 21f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 17f)
                verticalLineTo(13.5f)
                verticalLineTo(10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 13.75f)
                curveTo(11f, 10f, 17f, 10f, 17f, 13.75f)
                verticalLineTo(17f)
                moveTo(11f, 17f)
                verticalLineTo(13.75f)
                verticalLineTo(17f)
                close()
                moveTo(11f, 10f)
                verticalLineTo(13.75f)
                verticalLineTo(10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7.01f)
                lineTo(7.01f, 6.999f)
            }
        }.build()

        return _Linkedin!!
    }

@Suppress("ObjectPropertyName")
private var _Linkedin: ImageVector? = null
