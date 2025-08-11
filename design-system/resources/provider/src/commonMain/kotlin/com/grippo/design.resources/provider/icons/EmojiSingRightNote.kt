package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmojiSingRightNote: ImageVector
    get() {
        if (_EmojiSingRightNote != null) {
            return _EmojiSingRightNote!!
        }
        _EmojiSingRightNote = ImageVector.Builder(
            name = "EmojiSingRightNote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(20.8f, 8.1f)
                curveTo(20.8f, 8.597f, 20.397f, 9f, 19.9f, 9f)
                curveTo(19.403f, 9f, 19f, 8.597f, 19f, 8.1f)
                curveTo(19f, 7.603f, 19.403f, 7.2f, 19.9f, 7.2f)
                curveTo(20.397f, 7.2f, 20.8f, 7.603f, 20.8f, 8.1f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20.8f, 8.1f)
                verticalLineTo(3.6f)
                curveTo(20.8f, 3.269f, 21.069f, 3f, 21.4f, 3f)
                horizontalLineTo(23f)
                moveTo(20.8f, 8.1f)
                curveTo(20.8f, 8.597f, 20.397f, 9f, 19.9f, 9f)
                curveTo(19.403f, 9f, 19f, 8.597f, 19f, 8.1f)
                curveTo(19f, 7.603f, 19.403f, 7.2f, 19.9f, 7.2f)
                curveTo(20.397f, 7.2f, 20.8f, 7.603f, 20.8f, 8.1f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 17f)
                curveTo(14.895f, 17f, 14f, 16.105f, 14f, 15f)
                curveTo(14f, 13.895f, 14.895f, 13f, 16f, 13f)
                curveTo(17.105f, 13f, 18f, 13.895f, 18f, 15f)
                curveTo(18f, 16.105f, 17.105f, 17f, 16f, 17f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.951f, 13f)
                curveTo(21.449f, 18.053f, 17.185f, 22f, 12f, 22f)
                curveTo(6.477f, 22f, 2f, 17.523f, 2f, 12f)
                curveTo(2f, 6.477f, 6.477f, 2f, 12f, 2f)
                curveTo(13.422f, 2f, 14.775f, 2.297f, 16f, 2.832f)
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

        return _EmojiSingRightNote!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiSingRightNote: ImageVector? = null
