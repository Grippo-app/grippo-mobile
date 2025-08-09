package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.OpenInBrowser: ImageVector
    get() {
        if (_OpenInBrowser != null) {
            return _OpenInBrowser!!
        }
        _OpenInBrowser = ImageVector.Builder(
            name = "OpenInBrowser",
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
                moveTo(8f, 21f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 21f, 21f, 20.731f, 21f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(21f, 3.269f, 20.731f, 3f, 20.4f, 3f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 3f, 3f, 3.269f, 3f, 3.6f)
                verticalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 6f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                horizontalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 12f)
                horizontalLineTo(8f)
                moveTo(3.5f, 20.5f)
                lineTo(12f, 12f)
                lineTo(3.5f, 20.5f)
                close()
                moveTo(12f, 12f)
                verticalLineTo(16f)
                verticalLineTo(12f)
                close()
            }
        }.build()

        return _OpenInBrowser!!
    }

@Suppress("ObjectPropertyName")
private var _OpenInBrowser: ImageVector? = null
