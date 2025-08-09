package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.FavouriteBook: ImageVector
    get() {
        if (_FavouriteBook != null) {
            return _FavouriteBook!!
        }
        _FavouriteBook = ImageVector.Builder(
            name = "FavouriteBook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 19f)
                verticalLineTo(5f)
                curveTo(4f, 3.895f, 4.895f, 3f, 6f, 3f)
                horizontalLineTo(19.4f)
                curveTo(19.731f, 3f, 20f, 3.269f, 20f, 3.6f)
                verticalLineTo(16.714f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 8.78f)
                curveTo(16f, 9.381f, 15.762f, 9.959f, 15.338f, 10.386f)
                curveTo(14.362f, 11.37f, 13.415f, 12.396f, 12.402f, 13.344f)
                curveTo(12.17f, 13.559f, 11.802f, 13.551f, 11.58f, 13.327f)
                lineTo(8.661f, 10.386f)
                curveTo(7.78f, 9.497f, 7.78f, 8.062f, 8.661f, 7.173f)
                curveTo(9.552f, 6.276f, 11.003f, 6.276f, 11.894f, 7.173f)
                lineTo(12f, 7.28f)
                lineTo(12.106f, 7.173f)
                curveTo(12.533f, 6.743f, 13.115f, 6.5f, 13.722f, 6.5f)
                curveTo(14.33f, 6.5f, 14.911f, 6.743f, 15.338f, 7.173f)
                curveTo(15.762f, 7.601f, 16f, 8.178f, 16f, 8.78f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 17f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 21f)
                horizontalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 21f)
                curveTo(4.895f, 21f, 4f, 20.105f, 4f, 19f)
                curveTo(4f, 17.895f, 4.895f, 17f, 6f, 17f)
            }
        }.build()

        return _FavouriteBook!!
    }

@Suppress("ObjectPropertyName")
private var _FavouriteBook: ImageVector? = null
