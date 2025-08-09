package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PlaylistAdd: ImageVector
    get() {
        if (_PlaylistAdd != null) {
            return _PlaylistAdd!!
        }
        _PlaylistAdd = ImageVector.Builder(
            name = "PlaylistAdd",
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
                moveTo(20f, 18f)
                verticalLineTo(20f)
                moveTo(18f, 18f)
                horizontalLineTo(20f)
                horizontalLineTo(18f)
                close()
                moveTo(22f, 18f)
                horizontalLineTo(20f)
                horizontalLineTo(22f)
                close()
                moveTo(20f, 18f)
                verticalLineTo(16f)
                verticalLineTo(18f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 11f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 17f)
                horizontalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 5f)
                horizontalLineTo(20f)
            }
        }.build()

        return _PlaylistAdd!!
    }

@Suppress("ObjectPropertyName")
private var _PlaylistAdd: ImageVector? = null
