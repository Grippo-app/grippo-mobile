package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.MessageAlert: ImageVector
    get() {
        if (_MessageAlert != null) {
            return _MessageAlert!!
        }
        _MessageAlert = ImageVector.Builder(
            name = "MessageAlert",
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
                moveTo(12f, 7f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 13.01f)
                lineTo(12.01f, 12.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 20.289f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(15f)
                curveTo(21f, 16.105f, 20.105f, 17f, 19f, 17f)
                horizontalLineTo(7.961f)
                curveTo(7.354f, 17f, 6.779f, 17.276f, 6.4f, 17.751f)
                lineTo(4.069f, 20.664f)
                curveTo(3.714f, 21.107f, 3f, 20.857f, 3f, 20.289f)
                close()
            }
        }.build()

        return _MessageAlert!!
    }

@Suppress("ObjectPropertyName")
private var _MessageAlert: ImageVector? = null
