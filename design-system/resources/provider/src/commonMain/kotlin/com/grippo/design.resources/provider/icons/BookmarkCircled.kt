package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BookmarkCircled: ImageVector
    get() {
        if (_BookmarkCircled != null) {
            return _BookmarkCircled!!
        }
        _BookmarkCircled = ImageVector.Builder(
            name = "BookmarkCircled",
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
                moveTo(9f, 16f)
                verticalLineTo(10f)
                curveTo(9f, 8.895f, 9.895f, 8f, 11f, 8f)
                horizontalLineTo(13f)
                curveTo(14.105f, 8f, 15f, 8.895f, 15f, 10f)
                verticalLineTo(16f)
                lineTo(13.109f, 14.74f)
                curveTo(12.438f, 14.292f, 11.562f, 14.292f, 10.891f, 14.74f)
                lineTo(9f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(17.523f, 22f, 22f, 17.523f, 22f, 12f)
                curveTo(22f, 6.477f, 17.523f, 2f, 12f, 2f)
                curveTo(6.477f, 2f, 2f, 6.477f, 2f, 12f)
                curveTo(2f, 17.523f, 6.477f, 22f, 12f, 22f)
                close()
            }
        }.build()

        return _BookmarkCircled!!
    }

@Suppress("ObjectPropertyName")
private var _BookmarkCircled: ImageVector? = null
