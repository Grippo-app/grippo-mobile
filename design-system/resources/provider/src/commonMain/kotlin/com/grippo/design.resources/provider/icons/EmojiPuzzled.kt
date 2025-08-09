package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiPuzzled: ImageVector
    get() {
        if (_EmojiPuzzled != null) {
            return _EmojiPuzzled!!
        }
        _EmojiPuzzled = ImageVector.Builder(
            name = "EmojiPuzzled",
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
                moveTo(2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.5f, 15.5f)
                curveTo(11.5f, 15.5f, 13f, 13.5f, 16f, 13.5f)
                curveTo(19f, 13.5f, 20.5f, 15.5f, 20.5f, 15.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 4f)
                curveTo(3f, 1.246f, 7f, 1.246f, 7f, 4f)
                curveTo(7f, 5.967f, 5f, 5.639f, 5f, 8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 11.01f)
                lineTo(5.01f, 10.999f)
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 9f)
                curveTo(17.224f, 9f, 17f, 8.776f, 17f, 8.5f)
                curveTo(17f, 8.224f, 17.224f, 8f, 17.5f, 8f)
                curveTo(17.776f, 8f, 18f, 8.224f, 18f, 8.5f)
                curveTo(18f, 8.776f, 17.776f, 9f, 17.5f, 9f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 9f)
                curveTo(10.224f, 9f, 10f, 8.776f, 10f, 8.5f)
                curveTo(10f, 8.224f, 10.224f, 8f, 10.5f, 8f)
                curveTo(10.776f, 8f, 11f, 8.224f, 11f, 8.5f)
                curveTo(11f, 8.776f, 10.776f, 9f, 10.5f, 9f)
                close()
            }
        }.build()

        return _EmojiPuzzled!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiPuzzled: ImageVector? = null
