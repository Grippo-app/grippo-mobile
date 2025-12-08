package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Advanced: ImageVector
    get() {
        if (_EmojiSatisfied != null) {
            return _EmojiSatisfied!!
        }
        _EmojiSatisfied = ImageVector.Builder(
            name = "EmojiSatisfied",
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
        }.build()

        return _EmojiSatisfied!!
    }

@Suppress("ObjectPropertyName")
private var _EmojiSatisfied: ImageVector? = null
