package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Playlist: ImageVector
    get() {
        if (_Playlist != null) {
            return _Playlist!!
        }
        _Playlist = ImageVector.Builder(
            name = "Playlist",
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
                moveTo(2f, 11f)
                horizontalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 17f)
                horizontalLineTo(13f)
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
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(20f, 18.5f)
                curveTo(20f, 19.328f, 19.328f, 20f, 18.5f, 20f)
                curveTo(17.672f, 20f, 17f, 19.328f, 17f, 18.5f)
                curveTo(17f, 17.672f, 17.672f, 17f, 18.5f, 17f)
                curveTo(19.328f, 17f, 20f, 17.672f, 20f, 18.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(20f, 18.5f)
                verticalLineTo(10.6f)
                curveTo(20f, 10.269f, 20.269f, 10f, 20.6f, 10f)
                horizontalLineTo(22f)
                moveTo(20f, 18.5f)
                curveTo(20f, 19.328f, 19.328f, 20f, 18.5f, 20f)
                curveTo(17.672f, 20f, 17f, 19.328f, 17f, 18.5f)
                curveTo(17f, 17.672f, 17.672f, 17f, 18.5f, 17f)
                curveTo(19.328f, 17f, 20f, 17.672f, 20f, 18.5f)
                close()
            }
        }.build()

        return _Playlist!!
    }

@Suppress("ObjectPropertyName")
private var _Playlist: ImageVector? = null
