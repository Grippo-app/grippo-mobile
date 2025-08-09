package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Archive: ImageVector
    get() {
        if (_Archive != null) {
            return _Archive!!
        }
        _Archive = ImageVector.Builder(
            name = "Archive",
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
                moveTo(7f, 6f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 9f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 17f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 12f)
                horizontalLineTo(21f)
                moveTo(3f, 12f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 12f, 2f, 12.269f, 2f, 12.6f)
                verticalLineTo(21.4f)
                curveTo(2f, 21.731f, 2.269f, 22f, 2.6f, 22f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 22f, 22f, 21.731f, 22f, 21.4f)
                verticalLineTo(12.6f)
                curveTo(22f, 12.269f, 21.731f, 12f, 21.4f, 12f)
                horizontalLineTo(21f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 12f)
                verticalLineTo(2.6f)
                curveTo(3f, 2.269f, 3.269f, 2f, 3.6f, 2f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 2f, 21f, 2.269f, 21f, 2.6f)
                verticalLineTo(12f)
                horizontalLineTo(3f)
                close()
            }
        }.build()

        return _Archive!!
    }

@Suppress("ObjectPropertyName")
private var _Archive: ImageVector? = null
