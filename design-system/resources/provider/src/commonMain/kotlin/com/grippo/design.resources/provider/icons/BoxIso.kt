package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BoxIso: ImageVector
    get() {
        if (_BoxIso != null) {
            return _BoxIso!!
        }
        _BoxIso = ImageVector.Builder(
            name = "BoxIso",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(2.695f, 7.185f)
                lineTo(11.695f, 11.186f)
                lineTo(12.304f, 9.815f)
                lineTo(3.305f, 5.815f)
                lineTo(2.695f, 7.185f)
                close()
                moveTo(12.75f, 21.5f)
                verticalLineTo(10.5f)
                horizontalLineTo(11.25f)
                verticalLineTo(21.5f)
                horizontalLineTo(12.75f)
                close()
                moveTo(12.304f, 11.186f)
                lineTo(21.305f, 7.185f)
                lineTo(20.695f, 5.815f)
                lineTo(11.695f, 9.815f)
                lineTo(12.304f, 11.186f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 17.11f)
                verticalLineTo(6.89f)
                curveTo(3f, 6.653f, 3.14f, 6.438f, 3.356f, 6.342f)
                lineTo(11.756f, 2.608f)
                curveTo(11.911f, 2.539f, 12.089f, 2.539f, 12.244f, 2.608f)
                lineTo(20.644f, 6.342f)
                curveTo(20.86f, 6.438f, 21f, 6.653f, 21f, 6.89f)
                verticalLineTo(17.11f)
                curveTo(21f, 17.347f, 20.86f, 17.562f, 20.644f, 17.659f)
                lineTo(12.244f, 21.392f)
                curveTo(12.089f, 21.461f, 11.911f, 21.461f, 11.756f, 21.392f)
                lineTo(3.356f, 17.659f)
                curveTo(3.14f, 17.562f, 3f, 17.347f, 3f, 17.11f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 4.5f)
                lineTo(16.144f, 8.342f)
                curveTo(16.36f, 8.438f, 16.5f, 8.653f, 16.5f, 8.89f)
                verticalLineTo(12.5f)
            }
        }.build()

        return _BoxIso!!
    }

@Suppress("ObjectPropertyName")
private var _BoxIso: ImageVector? = null
