package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RoundedMirror: ImageVector
    get() {
        if (_RoundedMirror != null) {
            return _RoundedMirror!!
        }
        _RoundedMirror = ImageVector.Builder(
            name = "RoundedMirror",
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
                moveTo(20f, 10f)
                verticalLineTo(14f)
                curveTo(20f, 18.418f, 16.418f, 22f, 12f, 22f)
                curveTo(7.582f, 22f, 4f, 18.418f, 4f, 14f)
                verticalLineTo(10f)
                curveTo(4f, 5.582f, 7.582f, 2f, 12f, 2f)
                curveTo(16.418f, 2f, 20f, 5.582f, 20f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17.5f, 4.5f)
                lineTo(13f, 8f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 7f)
                lineTo(11.5f, 13f)
            }
        }.build()

        return _RoundedMirror!!
    }

@Suppress("ObjectPropertyName")
private var _RoundedMirror: ImageVector? = null
