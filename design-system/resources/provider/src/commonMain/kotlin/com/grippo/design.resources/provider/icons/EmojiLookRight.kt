package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiLookRight: ImageVector
    get() {
        if (_EmojiLookRight != null) {
            return _EmojiLookRight!!
        }
        _EmojiLookRight = ImageVector.Builder(
            name = "EmojiLookRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 9f)
                curveTo(15.776f, 9f, 16f, 8.776f, 16f, 8.5f)
                curveTo(16f, 8.224f, 15.776f, 8f, 15.5f, 8f)
                curveTo(15.224f, 8f, 15f, 8.224f, 15f, 8.5f)
                curveTo(15f, 8.776f, 15.224f, 9f, 15.5f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.542f, 15f)
                horizontalLineTo(17f)
                moveTo(21.542f, 15f)
                curveTo(21.84f, 14.053f, 22f, 13.045f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(16.478f, 22f, 20.268f, 19.057f, 21.542f, 15f)
                close()
            }
        }.build()

        return _EmojiLookRight!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiLookRight: ImageVector? = null
