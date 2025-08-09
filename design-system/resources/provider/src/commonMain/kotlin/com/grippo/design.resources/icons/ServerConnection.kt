package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.ServerConnection: ImageVector
    get() {
        if (_ServerConnection != null) {
            return _ServerConnection!!
        }
        _ServerConnection = ImageVector.Builder(
            name = "ServerConnection",
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
                moveTo(3f, 19f)
                horizontalLineTo(12f)
                horizontalLineTo(3f)
                close()
                moveTo(21f, 19f)
                horizontalLineTo(12f)
                horizontalLineTo(21f)
                close()
                moveTo(12f, 19f)
                verticalLineTo(13f)
                verticalLineTo(19f)
                close()
                moveTo(12f, 13f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(13f)
                horizontalLineTo(12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 9.01f)
                lineTo(9.01f, 8.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9.01f)
                lineTo(12.01f, 8.999f)
            }
        }.build()

        return _ServerConnection!!
    }

@Suppress("ObjectPropertyName")
private var _ServerConnection: ImageVector? = null
