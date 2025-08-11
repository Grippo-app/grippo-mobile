package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AppleImac2021Side: ImageVector
    get() {
        if (_AppleImac2021Side != null) {
            return _AppleImac2021Side!!
        }
        _AppleImac2021Side = ImageVector.Builder(
            name = "AppleImac2021Side",
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
                moveTo(10f, 13.5f)
                lineTo(11.5f, 19f)
                moveTo(6f, 22f)
                horizontalLineTo(8f)
                horizontalLineTo(6f)
                close()
                moveTo(14f, 22f)
                horizontalLineTo(8f)
                horizontalLineTo(14f)
                close()
                moveTo(8f, 22f)
                lineTo(10f, 13.5f)
                lineTo(8f, 22f)
                close()
                moveTo(10f, 13.5f)
                lineTo(7f, 2f)
                lineTo(10f, 13.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 22f)
                horizontalLineTo(18f)
            }
        }.build()

        return _AppleImac2021Side!!
    }

@Suppress("ObjectPropertyName")
private var _AppleImac2021Side: ImageVector? = null
