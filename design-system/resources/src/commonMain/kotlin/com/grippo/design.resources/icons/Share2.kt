package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Share2: ImageVector
    get() {
        if (_Share2 != null) {
            return _Share2!!
        }
        _Share2 = ImageVector.Builder(
            name = "Share2",
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
                moveTo(18f, 8f)
                curveTo(19.657f, 8f, 21f, 6.657f, 21f, 5f)
                curveTo(21f, 3.343f, 19.657f, 2f, 18f, 2f)
                curveTo(16.343f, 2f, 15f, 3.343f, 15f, 5f)
                curveTo(15f, 6.657f, 16.343f, 8f, 18f, 8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 15f)
                curveTo(7.657f, 15f, 9f, 13.657f, 9f, 12f)
                curveTo(9f, 10.343f, 7.657f, 9f, 6f, 9f)
                curveTo(4.343f, 9f, 3f, 10.343f, 3f, 12f)
                curveTo(3f, 13.657f, 4.343f, 15f, 6f, 15f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 22f)
                curveTo(19.657f, 22f, 21f, 20.657f, 21f, 19f)
                curveTo(21f, 17.343f, 19.657f, 16f, 18f, 16f)
                curveTo(16.343f, 16f, 15f, 17.343f, 15f, 19f)
                curveTo(15f, 20.657f, 16.343f, 22f, 18f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.59f, 13.51f)
                lineTo(15.42f, 17.49f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.41f, 6.51f)
                lineTo(8.59f, 10.49f)
            }
        }.build()

        return _Share2!!
    }

@Suppress("ObjectPropertyName")
private var _Share2: ImageVector? = null
