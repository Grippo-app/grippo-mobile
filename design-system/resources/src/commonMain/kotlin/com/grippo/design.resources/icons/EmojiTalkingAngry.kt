package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.EmojiTalkingAngry: ImageVector
    get() {
        if (_EmojiTalkingAngry != null) {
            return _EmojiTalkingAngry!!
        }
        _EmojiTalkingAngry = ImageVector.Builder(
            name = "EmojiTalkingAngry",
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
                moveTo(16f, 9f)
                horizontalLineTo(14f)
                horizontalLineTo(16f)
                close()
                moveTo(2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                close()
                moveTo(14f, 18f)
                horizontalLineTo(10f)
                verticalLineTo(15f)
                curveTo(10f, 14.333f, 10.4f, 13f, 12f, 13f)
                curveTo(13.6f, 13f, 14f, 14.333f, 14f, 15f)
                verticalLineTo(18f)
                close()
            }
        }.build()

        return _EmojiTalkingAngry!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiTalkingAngry: ImageVector? = null
