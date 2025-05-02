package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AtSign: ImageVector
    get() {
        if (_AtSign != null) {
            return _AtSign!!
        }
        _AtSign = ImageVector.Builder(
            name = "AtSign",
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
                moveTo(12f, 16f)
                curveTo(14.209f, 16f, 16f, 14.209f, 16f, 12f)
                curveTo(16f, 9.791f, 14.209f, 8f, 12f, 8f)
                curveTo(9.791f, 8f, 8f, 9.791f, 8f, 12f)
                curveTo(8f, 14.209f, 9.791f, 16f, 12f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8f)
                verticalLineTo(13f)
                curveTo(16f, 13.796f, 16.316f, 14.559f, 16.879f, 15.121f)
                curveTo(17.441f, 15.684f, 18.204f, 16f, 19f, 16f)
                curveTo(19.796f, 16f, 20.559f, 15.684f, 21.121f, 15.121f)
                curveTo(21.684f, 14.559f, 22f, 13.796f, 22f, 13f)
                verticalLineTo(12f)
                curveTo(22f, 9.743f, 21.236f, 7.552f, 19.833f, 5.785f)
                curveTo(18.43f, 4.017f, 16.471f, 2.775f, 14.273f, 2.262f)
                curveTo(12.075f, 1.749f, 9.768f, 1.995f, 7.727f, 2.959f)
                curveTo(5.687f, 3.924f, 4.032f, 5.55f, 3.033f, 7.574f)
                curveTo(2.034f, 9.597f, 1.749f, 11.9f, 2.224f, 14.106f)
                curveTo(2.699f, 16.313f, 3.907f, 18.293f, 5.651f, 19.726f)
                curveTo(7.394f, 21.159f, 9.571f, 21.96f, 11.828f, 21.999f)
                curveTo(14.085f, 22.038f, 16.288f, 21.312f, 18.08f, 19.94f)
            }
        }.build()

        return _AtSign!!
    }

@Suppress("ObjectPropertyName")
private var _AtSign: ImageVector? = null
