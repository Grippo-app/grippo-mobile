package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EmojiSurpriseAlt: ImageVector
    get() {
        if (_EmojiSurpriseAlt != null) {
            return _EmojiSurpriseAlt!!
        }
        _EmojiSurpriseAlt = ImageVector.Builder(
            name = "EmojiSurpriseAlt",
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
                moveTo(12f, 17f)
                curveTo(10.895f, 17f, 10f, 16.105f, 10f, 15f)
                curveTo(10f, 13.895f, 10.895f, 13f, 12f, 13f)
                curveTo(13.105f, 13f, 14f, 13.895f, 14f, 15f)
                curveTo(14f, 16.105f, 13.105f, 17f, 12f, 17f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 9f)
                curveTo(8.224f, 9f, 8f, 8.776f, 8f, 8.5f)
                curveTo(8f, 8.224f, 8.224f, 8f, 8.5f, 8f)
                curveTo(8.776f, 8f, 9f, 8.224f, 9f, 8.5f)
                curveTo(9f, 8.776f, 8.776f, 9f, 8.5f, 9f)
                close()
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

        return _EmojiSurpriseAlt!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiSurpriseAlt: ImageVector? = null
