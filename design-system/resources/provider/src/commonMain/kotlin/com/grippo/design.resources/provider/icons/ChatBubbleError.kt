package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.ChatBubbleError: ImageVector
    get() {
        if (_ChatBubbleError != null) {
            return _ChatBubbleError!!
        }
        _ChatBubbleError = ImageVector.Builder(
            name = "ChatBubbleError",
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
                moveTo(11.993f, 12f)
                lineTo(14.5f, 14.5f)
                moveTo(9.5f, 14.5f)
                lineTo(11.993f, 12f)
                lineTo(9.5f, 14.5f)
                close()
                moveTo(14.5f, 9.5f)
                lineTo(11.993f, 12f)
                lineTo(14.5f, 9.5f)
                close()
                moveTo(11.993f, 12f)
                lineTo(9.5f, 9.5f)
                lineTo(11.993f, 12f)
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
                curveTo(2f, 13.821f, 2.487f, 15.529f, 3.338f, 17f)
                lineTo(2.5f, 21.5f)
                lineTo(7f, 20.662f)
                curveTo(8.471f, 21.513f, 10.179f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _ChatBubbleError!!
    }

@Suppress("ObjectPropertyName")
private var _ChatBubbleError: ImageVector? = null
