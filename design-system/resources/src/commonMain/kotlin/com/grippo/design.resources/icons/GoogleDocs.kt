package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.GoogleDocs: ImageVector
    get() {
        if (_GoogleDocs != null) {
            return _GoogleDocs!!
        }
        _GoogleDocs = ImageVector.Builder(
            name = "GoogleDocs",
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
                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveTo(3.895f, 3f, 3f, 3.895f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.105f, 3.895f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.105f, 21f, 21f, 20.105f, 21f, 19f)
                verticalLineTo(5f)
                curveTo(21f, 3.895f, 20.105f, 3f, 19f, 3f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 12f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 17f)
                horizontalLineTo(13f)
            }
        }.build()

        return _GoogleDocs!!
    }

@Suppress("ObjectPropertyName")
private var _GoogleDocs: ImageVector? = null
