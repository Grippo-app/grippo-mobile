package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Zap: ImageVector
    get() {
        if (_Zap != null) {
            return _Zap!!
        }
        _Zap = ImageVector.Builder(
            name = "Zap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 2f)
                lineTo(3f, 14f)
                horizontalLineTo(12f)
                lineTo(11f, 22f)
                lineTo(21f, 10f)
                horizontalLineTo(12f)
                lineTo(13f, 2f)
                close()
            }
        }.build()

        return _Zap!!
    }

@Suppress("ObjectPropertyName")
private var _Zap: ImageVector? = null
