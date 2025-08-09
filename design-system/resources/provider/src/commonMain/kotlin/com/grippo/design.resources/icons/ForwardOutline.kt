package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ForwardOutline: ImageVector
    get() {
        if (_ForwardOutline != null) {
            return _ForwardOutline!!
        }
        _ForwardOutline = ImageVector.Builder(
            name = "ForwardOutline",
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
                moveTo(2.956f, 5.704f)
                curveTo(2.56f, 5.413f, 2f, 5.695f, 2f, 6.188f)
                verticalLineTo(17.813f)
                curveTo(2f, 18.305f, 2.56f, 18.588f, 2.956f, 18.296f)
                lineTo(10.844f, 12.483f)
                curveTo(11.17f, 12.243f, 11.17f, 11.757f, 10.844f, 11.517f)
                lineTo(2.956f, 5.704f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.956f, 5.704f)
                curveTo(13.56f, 5.413f, 13f, 5.695f, 13f, 6.188f)
                verticalLineTo(17.813f)
                curveTo(13f, 18.305f, 13.56f, 18.588f, 13.956f, 18.296f)
                lineTo(21.844f, 12.483f)
                curveTo(22.17f, 12.243f, 22.17f, 11.757f, 21.844f, 11.517f)
                lineTo(13.956f, 5.704f)
                close()
            }
        }.build()

        return _ForwardOutline!!
    }

@Suppress("ObjectPropertyName")
private var _ForwardOutline: ImageVector? = null
