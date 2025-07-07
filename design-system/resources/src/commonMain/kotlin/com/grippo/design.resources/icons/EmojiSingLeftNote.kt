package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EmojiSingLeftNote: ImageVector
    get() {
        if (_EmojiSingLeftNote != null) {
            return _EmojiSingLeftNote!!
        }
        _EmojiSingLeftNote = ImageVector.Builder(
            name = "EmojiSingLeftNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(2.8f, 8.1f)
                curveTo(2.8f, 8.597f, 2.397f, 9f, 1.9f, 9f)
                curveTo(1.403f, 9f, 1f, 8.597f, 1f, 8.1f)
                curveTo(1f, 7.603f, 1.403f, 7.2f, 1.9f, 7.2f)
                curveTo(2.397f, 7.2f, 2.8f, 7.603f, 2.8f, 8.1f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.8f, 8.1f)
                verticalLineTo(3.6f)
                curveTo(2.8f, 3.269f, 3.069f, 3f, 3.4f, 3f)
                horizontalLineTo(5f)
                moveTo(2.8f, 8.1f)
                curveTo(2.8f, 8.597f, 2.397f, 9f, 1.9f, 9f)
                curveTo(1.403f, 9f, 1f, 8.597f, 1f, 8.1f)
                curveTo(1f, 7.603f, 1.403f, 7.2f, 1.9f, 7.2f)
                curveTo(2.397f, 7.2f, 2.8f, 7.603f, 2.8f, 8.1f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 17f)
                curveTo(9.105f, 17f, 10f, 16.105f, 10f, 15f)
                curveTo(10f, 13.895f, 9.105f, 13f, 8f, 13f)
                curveTo(6.895f, 13f, 6f, 13.895f, 6f, 15f)
                curveTo(6f, 16.105f, 6.895f, 17f, 8f, 17f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.049f, 13f)
                curveTo(2.551f, 18.053f, 6.815f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(10.578f, 2f, 9.225f, 2.297f, 8f, 2.832f)
            }
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
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 9f)
                curveTo(8.776f, 9f, 9f, 8.776f, 9f, 8.5f)
                curveTo(9f, 8.224f, 8.776f, 8f, 8.5f, 8f)
                curveTo(8.224f, 8f, 8f, 8.224f, 8f, 8.5f)
                curveTo(8f, 8.776f, 8.224f, 9f, 8.5f, 9f)
                close()
            }
        }.build()

        return _EmojiSingLeftNote!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiSingLeftNote: ImageVector? = null
