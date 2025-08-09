package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Settings: ImageVector
    get() {
        if (_Settings != null) {
            return _Settings!!
        }
        _Settings = ImageVector.Builder(
            name = "Settings",
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
                moveTo(12f, 15f)
                curveTo(13.657f, 15f, 15f, 13.657f, 15f, 12f)
                curveTo(15f, 10.343f, 13.657f, 9f, 12f, 9f)
                curveTo(10.343f, 9f, 9f, 10.343f, 9f, 12f)
                curveTo(9f, 13.657f, 10.343f, 15f, 12f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19.622f, 10.395f)
                lineTo(18.525f, 7.745f)
                lineTo(20f, 6f)
                lineTo(18f, 4f)
                lineTo(16.265f, 5.483f)
                lineTo(13.558f, 4.37f)
                lineTo(12.935f, 2f)
                horizontalLineTo(10.981f)
                lineTo(10.349f, 4.401f)
                lineTo(7.704f, 5.516f)
                lineTo(6f, 4f)
                lineTo(4f, 6f)
                lineTo(5.453f, 7.789f)
                lineTo(4.372f, 10.446f)
                lineTo(2f, 11f)
                verticalLineTo(13f)
                lineTo(4.401f, 13.656f)
                lineTo(5.516f, 16.3f)
                lineTo(4f, 18f)
                lineTo(6f, 20f)
                lineTo(7.791f, 18.54f)
                lineTo(10.397f, 19.612f)
                lineTo(11f, 22f)
                horizontalLineTo(13f)
                lineTo(13.604f, 19.613f)
                lineTo(16.255f, 18.515f)
                curveTo(16.697f, 18.831f, 18f, 20f, 18f, 20f)
                lineTo(20f, 18f)
                lineTo(18.516f, 16.249f)
                lineTo(19.614f, 13.598f)
                lineTo(22f, 12.977f)
                lineTo(22f, 11f)
                lineTo(19.622f, 10.395f)
                close()
            }
        }.build()

        return _Settings!!
    }

@Suppress("ObjectPropertyName")
private var _Settings: ImageVector? = null
