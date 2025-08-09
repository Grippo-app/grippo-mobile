package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.RefreshCircular: ImageVector
    get() {
        if (_RefreshCircular != null) {
            return _RefreshCircular!!
        }
        _RefreshCircular = ImageVector.Builder(
            name = "RefreshCircular",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.583f, 9.667f)
                curveTo(15.809f, 8.097f, 14.043f, 7f, 11.988f, 7f)
                curveTo(9.389f, 7f, 7.251f, 8.754f, 7f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.494f, 9.722f)
                horizontalLineTo(16.4f)
                curveTo(16.732f, 9.722f, 17f, 9.454f, 17f, 9.122f)
                verticalLineTo(7.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.417f, 13.667f)
                curveTo(8.19f, 15.629f, 9.957f, 17f, 12.012f, 17f)
                curveTo(14.611f, 17f, 16.748f, 14.807f, 17f, 12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.506f, 13.622f)
                horizontalLineTo(7.6f)
                curveTo(7.269f, 13.622f, 7f, 13.891f, 7f, 14.222f)
                verticalLineTo(16.4f)
            }
        }.build()

        return _RefreshCircular!!
    }

@Suppress("ObjectPropertyName")
private var _RefreshCircular: ImageVector? = null
