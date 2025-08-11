package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiLookBottom: ImageVector
    get() {
        if (_EmojiLookBottom != null) {
            return _EmojiLookBottom!!
        }
        _EmojiLookBottom = ImageVector.Builder(
            name = "EmojiLookBottom",
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
                moveTo(8.5f, 14f)
                curveTo(8.224f, 14f, 8f, 13.776f, 8f, 13.5f)
                curveTo(8f, 13.224f, 8.224f, 13f, 8.5f, 13f)
                curveTo(8.776f, 13f, 9f, 13.224f, 9f, 13.5f)
                curveTo(9f, 13.776f, 8.776f, 14f, 8.5f, 14f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 14f)
                curveTo(15.224f, 14f, 15f, 13.776f, 15f, 13.5f)
                curveTo(15f, 13.224f, 15.224f, 13f, 15.5f, 13f)
                curveTo(15.776f, 13f, 16f, 13.224f, 16f, 13.5f)
                curveTo(16f, 13.776f, 15.776f, 14f, 15.5f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 18f)
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

        return _EmojiLookBottom!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiLookBottom: ImageVector? = null
