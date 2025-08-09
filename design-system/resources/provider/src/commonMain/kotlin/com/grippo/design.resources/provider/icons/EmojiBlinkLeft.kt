package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiBlinkLeft: ImageVector
    get() {
        if (_EmojiBlinkLeft != null) {
            return _EmojiBlinkLeft!!
        }
        _EmojiBlinkLeft = ImageVector.Builder(
            name = "EmojiBlinkLeft",
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
                moveTo(10f, 9f)
                horizontalLineTo(8f)
                horizontalLineTo(10f)
                close()
                moveTo(2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 14.5f)
                curveTo(16.5f, 14.5f, 15f, 16.5f, 12f, 16.5f)
                curveTo(9f, 16.5f, 7.5f, 14.5f, 7.5f, 14.5f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 9f)
                curveTo(15.224f, 9f, 15f, 8.776f, 15f, 8.5f)
                curveTo(15f, 8.224f, 15.224f, 8f, 15.5f, 8f)
                curveTo(15.776f, 8f, 16f, 8.224f, 16f, 8.5f)
                curveTo(16f, 8.776f, 15.776f, 9f, 15.5f, 9f)
                close()
            }
        }.build()

        return _EmojiBlinkLeft!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiBlinkLeft: ImageVector? = null
