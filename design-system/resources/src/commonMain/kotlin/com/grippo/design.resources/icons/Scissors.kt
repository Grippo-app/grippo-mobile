package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Scissors: ImageVector
    get() {
        if (_Scissors != null) {
            return _Scissors!!
        }
        _Scissors = ImageVector.Builder(
            name = "Scissors",
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
                moveTo(6f, 9f)
                curveTo(7.657f, 9f, 9f, 7.657f, 9f, 6f)
                curveTo(9f, 4.343f, 7.657f, 3f, 6f, 3f)
                curveTo(4.343f, 3f, 3f, 4.343f, 3f, 6f)
                curveTo(3f, 7.657f, 4.343f, 9f, 6f, 9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                curveTo(7.657f, 21f, 9f, 19.657f, 9f, 18f)
                curveTo(9f, 16.343f, 7.657f, 15f, 6f, 15f)
                curveTo(4.343f, 15f, 3f, 16.343f, 3f, 18f)
                curveTo(3f, 19.657f, 4.343f, 21f, 6f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20f, 4f)
                lineTo(8.12f, 15.88f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.47f, 14.48f)
                lineTo(20f, 20f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.12f, 8.12f)
                lineTo(12f, 12f)
            }
        }.build()

        return _Scissors!!
    }

@Suppress("ObjectPropertyName")
private var _Scissors: ImageVector? = null
