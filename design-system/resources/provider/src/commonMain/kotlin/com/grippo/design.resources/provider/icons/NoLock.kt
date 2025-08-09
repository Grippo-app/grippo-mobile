package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.NoLock: ImageVector
    get() {
        if (_NoLock != null) {
            return _NoLock!!
        }
        _NoLock = ImageVector.Builder(
            name = "NoLock",
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
                moveTo(11.5f, 12f)
                horizontalLineTo(6.6f)
                curveTo(6.269f, 12f, 6f, 12.269f, 6f, 12.6f)
                verticalLineTo(19.4f)
                curveTo(6f, 19.731f, 6.269f, 20f, 6.6f, 20f)
                horizontalLineTo(17.4f)
                curveTo(17.731f, 20f, 18f, 19.731f, 18f, 19.4f)
                verticalLineTo(18.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 12f)
                verticalLineTo(8f)
                curveTo(16f, 6.667f, 15.2f, 4f, 12f, 4f)
                curveTo(11.253f, 4f, 10.637f, 4.145f, 10.131f, 4.385f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 12f)
                horizontalLineTo(17.4f)
                curveTo(17.731f, 12f, 18f, 12.269f, 18f, 12.6f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 8f)
                verticalLineTo(8.5f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 3f)
                lineTo(21f, 21f)
            }
        }.build()

        return _NoLock!!
    }

@Suppress("ObjectPropertyName")
private var _NoLock: ImageVector? = null
