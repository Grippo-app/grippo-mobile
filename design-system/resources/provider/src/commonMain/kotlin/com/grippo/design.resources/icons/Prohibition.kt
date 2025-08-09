package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Prohibition: ImageVector
    get() {
        if (_Prohibition != null) {
            return _Prohibition!!
        }
        _Prohibition = ImageVector.Builder(
            name = "Prohibition",
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
                moveTo(19.141f, 5f)
                lineTo(4.859f, 19f)
                moveTo(19.141f, 5f)
                curveTo(17.326f, 3.149f, 14.797f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 14.726f, 3.09f, 17.196f, 4.859f, 19f)
                lineTo(19.141f, 5f)
                close()
                moveTo(19.141f, 5f)
                curveTo(20.91f, 6.804f, 22f, 9.275f, 22f, 12f)
                curveTo(22f, 17.523f, 17.523f, 22f, 12f, 22f)
                curveTo(9.203f, 22f, 6.673f, 20.851f, 4.859f, 19f)
                lineTo(19.141f, 5f)
                close()
            }
        }.build()

        return _Prohibition!!
    }

@Suppress("ObjectPropertyName")
private var _Prohibition: ImageVector? = null
