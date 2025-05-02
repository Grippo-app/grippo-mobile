package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Inbox: ImageVector
    get() {
        if (_Inbox != null) {
            return _Inbox!!
        }
        _Inbox = ImageVector.Builder(
            name = "Inbox",
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
                moveTo(22f, 12f)
                horizontalLineTo(16f)
                lineTo(14f, 15f)
                horizontalLineTo(10f)
                lineTo(8f, 12f)
                horizontalLineTo(2f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.45f, 5.11f)
                lineTo(2f, 12f)
                verticalLineTo(18f)
                curveTo(2f, 18.53f, 2.211f, 19.039f, 2.586f, 19.414f)
                curveTo(2.961f, 19.789f, 3.47f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(20.53f, 20f, 21.039f, 19.789f, 21.414f, 19.414f)
                curveTo(21.789f, 19.039f, 22f, 18.53f, 22f, 18f)
                verticalLineTo(12f)
                lineTo(18.55f, 5.11f)
                curveTo(18.384f, 4.777f, 18.129f, 4.496f, 17.813f, 4.3f)
                curveTo(17.497f, 4.104f, 17.132f, 4f, 16.76f, 4f)
                horizontalLineTo(7.24f)
                curveTo(6.868f, 4f, 6.503f, 4.104f, 6.187f, 4.3f)
                curveTo(5.871f, 4.496f, 5.616f, 4.777f, 5.45f, 5.11f)
                verticalLineTo(5.11f)
                close()
            }
        }.build()

        return _Inbox!!
    }

@Suppress("ObjectPropertyName")
private var _Inbox: ImageVector? = null
