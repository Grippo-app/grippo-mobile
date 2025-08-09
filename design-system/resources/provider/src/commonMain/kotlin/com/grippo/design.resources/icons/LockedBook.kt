package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LockedBook: ImageVector
    get() {
        if (_LockedBook != null) {
            return _LockedBook!!
        }
        _LockedBook = ImageVector.Builder(
            name = "LockedBook",
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
                moveTo(14f, 10f)
                horizontalLineTo(10f)
                moveTo(14f, 10f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 10f, 15f, 10.269f, 15f, 10.6f)
                verticalLineTo(13.4f)
                curveTo(15f, 13.731f, 14.731f, 14f, 14.4f, 14f)
                horizontalLineTo(9.6f)
                curveTo(9.269f, 14f, 9f, 13.731f, 9f, 13.4f)
                verticalLineTo(10.6f)
                curveTo(9f, 10.269f, 9.269f, 10f, 9.6f, 10f)
                horizontalLineTo(10f)
                horizontalLineTo(14f)
                close()
                moveTo(14f, 10f)
                verticalLineTo(8f)
                curveTo(14f, 7.333f, 13.6f, 6f, 12f, 6f)
                curveTo(10.4f, 6f, 10f, 7.333f, 10f, 8f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
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

        return _LockedBook!!
    }

@Suppress("ObjectPropertyName")
private var _LockedBook: ImageVector? = null
