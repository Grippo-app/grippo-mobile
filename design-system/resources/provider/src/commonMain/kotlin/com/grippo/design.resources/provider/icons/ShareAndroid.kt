package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ShareAndroid: ImageVector
    get() {
        if (_ShareAndroid != null) {
            return _ShareAndroid!!
        }
        _ShareAndroid = ImageVector.Builder(
            name = "ShareAndroid",
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
                moveTo(18f, 22f)
                curveTo(19.657f, 22f, 21f, 20.657f, 21f, 19f)
                curveTo(21f, 17.343f, 19.657f, 16f, 18f, 16f)
                curveTo(16.343f, 16f, 15f, 17.343f, 15f, 19f)
                curveTo(15f, 20.657f, 16.343f, 22f, 18f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 8f)
                curveTo(19.657f, 8f, 21f, 6.657f, 21f, 5f)
                curveTo(21f, 3.343f, 19.657f, 2f, 18f, 2f)
                curveTo(16.343f, 2f, 15f, 3.343f, 15f, 5f)
                curveTo(15f, 6.657f, 16.343f, 8f, 18f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 15f)
                curveTo(7.657f, 15f, 9f, 13.657f, 9f, 12f)
                curveTo(9f, 10.343f, 7.657f, 9f, 6f, 9f)
                curveTo(4.343f, 9f, 3f, 10.343f, 3f, 12f)
                curveTo(3f, 13.657f, 4.343f, 15f, 6f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(15.5f, 6.5f)
                lineTo(8.5f, 10.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(8.5f, 13.5f)
                lineTo(15.5f, 17.5f)
            }
        }.build()

        return _ShareAndroid!!
    }

@Suppress("ObjectPropertyName")
private var _ShareAndroid: ImageVector? = null
