package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BoxingGlove: ImageVector
    get() {
        if (_BoxingGlove != null) {
            return _BoxingGlove!!
        }
        _BoxingGlove = ImageVector.Builder(
            name = "BoxingGlove",
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
                moveTo(8.489f, 17.727f)
                curveTo(5.2f, 15.545f, 3.556f, 10.091f, 4.104f, 8.455f)
                curveTo(4.542f, 7.145f, 6.479f, 7.545f, 7.393f, 7.909f)
                curveTo(7.393f, 4.091f, 9.037f, 3f, 13.422f, 3f)
                curveTo(17.807f, 3f, 20f, 4.091f, 20f, 9.545f)
                curveTo(20f, 13.909f, 18.904f, 16.818f, 18.356f, 17.727f)
                moveTo(8.489f, 17.727f)
                horizontalLineTo(18.356f)
                horizontalLineTo(8.489f)
                close()
                moveTo(8.489f, 17.727f)
                verticalLineTo(21f)
                horizontalLineTo(18.356f)
                verticalLineTo(17.727f)
                horizontalLineTo(8.489f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.393f, 7.909f)
                curveTo(7.758f, 8.273f, 8.818f, 9f, 10.134f, 9f)
                curveTo(11.449f, 9f, 13.97f, 9f, 15.067f, 9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.393f, 7.909f)
                curveTo(7.393f, 11.727f, 9.037f, 12.273f, 10.134f, 12.273f)
            }
        }.build()

        return _BoxingGlove!!
    }

@Suppress("ObjectPropertyName")
private var _BoxingGlove: ImageVector? = null
