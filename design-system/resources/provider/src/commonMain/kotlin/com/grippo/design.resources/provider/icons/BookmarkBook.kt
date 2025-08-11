package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BookmarkBook: ImageVector
    get() {
        if (_BookmarkBook != null) {
            return _BookmarkBook!!
        }
        _BookmarkBook = ImageVector.Builder(
            name = "BookmarkBook",
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
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 3f)
                verticalLineTo(11f)
                lineTo(10.5f, 9.4f)
                lineTo(13f, 11f)
                verticalLineTo(3f)
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

        return _BookmarkBook!!
    }

@Suppress("ObjectPropertyName")
private var _BookmarkBook: ImageVector? = null
