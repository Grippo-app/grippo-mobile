package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Type: ImageVector
    get() {
        if (_Type != null) {
            return _Type!!
        }
        _Type = ImageVector.Builder(
            name = "Type",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(7f, 16.249f)
                curveTo(7f, 16.089f, 6.937f, 15.937f, 6.824f, 15.824f)
                lineTo(3.176f, 12.176f)
                curveTo(3.063f, 12.063f, 3f, 11.911f, 3f, 11.752f)
                verticalLineTo(4f)
                curveTo(3f, 2.895f, 3.895f, 2f, 5f, 2f)
                horizontalLineTo(12f)
                horizontalLineTo(19f)
                curveTo(20.105f, 2f, 21f, 2.895f, 21f, 4f)
                verticalLineTo(11.752f)
                curveTo(21f, 11.911f, 20.937f, 12.063f, 20.824f, 12.176f)
                lineTo(17.176f, 15.824f)
                curveTo(17.063f, 15.937f, 17f, 16.089f, 17f, 16.249f)
                verticalLineTo(20f)
                curveTo(17f, 21.105f, 16.105f, 22f, 15f, 22f)
                horizontalLineTo(9f)
                curveTo(7.895f, 22f, 7f, 21.105f, 7f, 20f)
                verticalLineTo(16.249f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 10.4f)
                horizontalLineTo(10f)
                moveTo(9.5f, 11.5f)
                lineTo(10f, 10.4f)
                lineTo(9.5f, 11.5f)
                close()
                moveTo(14.5f, 11.5f)
                lineTo(14f, 10.4f)
                lineTo(14.5f, 11.5f)
                close()
                moveTo(14f, 10.4f)
                lineTo(12f, 6f)
                lineTo(10f, 10.4f)
                horizontalLineTo(14f)
                close()
            }
        }.build()

        return _Type!!
    }

@Suppress("ObjectPropertyName")
private var _Type: ImageVector? = null
