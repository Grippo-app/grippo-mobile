package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Packages: ImageVector
    get() {
        if (_Packages != null) {
            return _Packages!!
        }
        _Packages = ImageVector.Builder(
            name = "Packages",
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
                moveTo(10f, 15f)
                lineTo(10f, 19f)
                curveTo(10f, 20.105f, 9.105f, 21f, 8f, 21f)
                horizontalLineTo(4f)
                curveTo(2.895f, 21f, 2f, 20.105f, 2f, 19f)
                verticalLineTo(15f)
                curveTo(2f, 13.895f, 2.895f, 13f, 4f, 13f)
                horizontalLineTo(8f)
                curveTo(9.105f, 13f, 10f, 13.895f, 10f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5f)
                verticalLineTo(9f)
                curveTo(16f, 10.105f, 15.105f, 11f, 14f, 11f)
                horizontalLineTo(10f)
                curveTo(8.895f, 11f, 8f, 10.105f, 8f, 9f)
                verticalLineTo(5f)
                curveTo(8f, 3.895f, 8.895f, 3f, 10f, 3f)
                horizontalLineTo(14f)
                curveTo(15.104f, 3f, 16f, 3.895f, 16f, 5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 15f)
                verticalLineTo(19f)
                curveTo(22f, 20.105f, 21.105f, 21f, 20f, 21f)
                horizontalLineTo(16f)
                curveTo(14.895f, 21f, 14f, 20.105f, 14f, 19f)
                verticalLineTo(15f)
                curveTo(14f, 13.895f, 14.895f, 13f, 16f, 13f)
                horizontalLineTo(20f)
                curveTo(21.104f, 13f, 22f, 13.895f, 22f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 16f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 6f)
                verticalLineTo(3f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 16f)
                verticalLineTo(13f)
            }
        }.build()

        return _Packages!!
    }

@Suppress("ObjectPropertyName")
private var _Packages: ImageVector? = null
