package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ChatBubbleTranslate: ImageVector
    get() {
        if (_ChatBubbleTranslate != null) {
            return _ChatBubbleTranslate!!
        }
        _ChatBubbleTranslate = ImageVector.Builder(
            name = "ChatBubbleTranslate",
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
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 13.821f, 2.487f, 15.529f, 3.338f, 17f)
                lineTo(2.5f, 21.5f)
                lineTo(7f, 20.662f)
                curveTo(8.471f, 21.513f, 10.179f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14.277f)
                lineTo(14.143f, 16.483f)
                moveTo(7f, 8.517f)
                horizontalLineTo(12f)
                horizontalLineTo(7f)
                close()
                moveTo(17f, 8.517f)
                horizontalLineTo(15.214f)
                horizontalLineTo(17f)
                close()
                moveTo(12f, 8.517f)
                horizontalLineTo(15.214f)
                horizontalLineTo(12f)
                close()
                moveTo(12f, 8.517f)
                verticalLineTo(7f)
                verticalLineTo(8.517f)
                close()
                moveTo(15.214f, 8.517f)
                curveTo(14.628f, 10.592f, 13.401f, 12.554f, 12f, 14.277f)
                lineTo(15.214f, 8.517f)
                close()
                moveTo(8.429f, 18f)
                curveTo(9.561f, 16.969f, 10.84f, 15.705f, 12f, 14.277f)
                lineTo(8.429f, 18f)
                close()
                moveTo(12f, 14.277f)
                curveTo(11.286f, 13.448f, 10.286f, 12.107f, 10f, 11.5f)
                lineTo(12f, 14.277f)
                close()
            }
        }.build()

        return _ChatBubbleTranslate!!
    }

@Suppress("ObjectPropertyName")
private var _ChatBubbleTranslate: ImageVector? = null
