package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.MessageCircle: ImageVector
    get() {
        if (_MessageCircle != null) {
            return _MessageCircle!!
        }
        _MessageCircle = ImageVector.Builder(
            name = "MessageCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 11.5f)
                curveTo(21.003f, 12.82f, 20.695f, 14.122f, 20.1f, 15.3f)
                curveTo(19.394f, 16.712f, 18.31f, 17.899f, 16.967f, 18.729f)
                curveTo(15.625f, 19.559f, 14.078f, 19.999f, 12.5f, 20f)
                curveTo(11.18f, 20.003f, 9.878f, 19.695f, 8.7f, 19.1f)
                lineTo(3f, 21f)
                lineTo(4.9f, 15.3f)
                curveTo(4.305f, 14.122f, 3.997f, 12.82f, 4f, 11.5f)
                curveTo(4.001f, 9.922f, 4.441f, 8.375f, 5.271f, 7.033f)
                curveTo(6.101f, 5.69f, 7.288f, 4.606f, 8.7f, 3.9f)
                curveTo(9.878f, 3.305f, 11.18f, 2.997f, 12.5f, 3f)
                horizontalLineTo(13f)
                curveTo(15.084f, 3.115f, 17.053f, 3.995f, 18.529f, 5.471f)
                curveTo(20.005f, 6.947f, 20.885f, 8.916f, 21f, 11f)
                verticalLineTo(11.5f)
                close()
            }
        }.build()

        return _MessageCircle!!
    }

@Suppress("ObjectPropertyName")
private var _MessageCircle: ImageVector? = null
