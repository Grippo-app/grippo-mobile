package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiLookTop: ImageVector
    get() {
        if (_EmojiLookTop != null) {
            return _EmojiLookTop!!
        }
        _EmojiLookTop = ImageVector.Builder(
            name = "EmojiLookTop",
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
                moveTo(8.5f, 7f)
                curveTo(8.224f, 7f, 8f, 6.776f, 8f, 6.5f)
                curveTo(8f, 6.224f, 8.224f, 6f, 8.5f, 6f)
                curveTo(8.776f, 6f, 9f, 6.224f, 9f, 6.5f)
                curveTo(9f, 6.776f, 8.776f, 7f, 8.5f, 7f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 7f)
                curveTo(15.224f, 7f, 15f, 6.776f, 15f, 6.5f)
                curveTo(15f, 6.224f, 15.224f, 6f, 15.5f, 6f)
                curveTo(15.776f, 6f, 16f, 6.224f, 16f, 6.5f)
                curveTo(16f, 6.776f, 15.776f, 7f, 15.5f, 7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 11f)
                horizontalLineTo(14f)
                horizontalLineTo(10f)
                close()
                moveTo(22f, 12f)
                curveTo(22f, 17.523f, 17.523f, 22f, 12f, 22f)
                curveTo(6.477f, 22f, 2f, 17.523f, 2f, 12f)
                curveTo(2f, 6.477f, 6.477f, 2f, 12f, 2f)
                curveTo(17.523f, 2f, 22f, 6.477f, 22f, 12f)
                close()
            }
        }.build()

        return _EmojiLookTop!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiLookTop: ImageVector? = null
