package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ShareIos: ImageVector
    get() {
        if (_ShareIos != null) {
            return _ShareIos!!
        }
        _ShareIos = ImageVector.Builder(
            name = "ShareIos",
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
                moveTo(20f, 13f)
                verticalLineTo(19f)
                curveTo(20f, 20.105f, 19.105f, 21f, 18f, 21f)
                horizontalLineTo(6f)
                curveTo(4.895f, 21f, 4f, 20.105f, 4f, 19f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3f)
                lineTo(15.5f, 6.5f)
                moveTo(12f, 15f)
                verticalLineTo(3f)
                verticalLineTo(15f)
                close()
                moveTo(12f, 3f)
                lineTo(8.5f, 6.5f)
                lineTo(12f, 3f)
                close()
            }
        }.build()

        return _ShareIos!!
    }

@Suppress("ObjectPropertyName")
private var _ShareIos: ImageVector? = null
