package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Compass: ImageVector
    get() {
        if (_Compass != null) {
            return _Compass!!
        }
        _Compass = ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                close()
                moveTo(15.8f, 8.2f)
                lineTo(13.5f, 13.5f)
                lineTo(8.2f, 15.8f)
                lineTo(10.5f, 10.5f)
                close()
                moveTo(10.5f, 10.5f)
                lineTo(13.5f, 13.5f)
            }
        }.build()
        return _Compass!!
    }

@Suppress("ObjectPropertyName")
private var _Compass: ImageVector? = null
