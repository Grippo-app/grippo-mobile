package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PrivateWifi: ImageVector
    get() {
        if (_PrivateWifi != null) {
            return _PrivateWifi!!
        }
        _PrivateWifi = ImageVector.Builder(
            name = "PrivateWifi",
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
                moveTo(12f, 18.51f)
                lineTo(12.01f, 18.499f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 7f)
                curveTo(8f, 2.5f, 16f, 2.5f, 22f, 7f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 11f)
                curveTo(9f, 8f, 15f, 8f, 19f, 11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 14.5f)
                curveTo(10.75f, 13.1f, 13.25f, 13.1f, 15.5f, 14.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21.167f, 18.5f)
                horizontalLineTo(17.833f)
                moveTo(21.167f, 18.5f)
                horizontalLineTo(21.4f)
                curveTo(21.731f, 18.5f, 22f, 18.769f, 22f, 19.1f)
                verticalLineTo(21.4f)
                curveTo(22f, 21.731f, 21.731f, 22f, 21.4f, 22f)
                horizontalLineTo(17.6f)
                curveTo(17.269f, 22f, 17f, 21.731f, 17f, 21.4f)
                verticalLineTo(19.1f)
                curveTo(17f, 18.769f, 17.269f, 18.5f, 17.6f, 18.5f)
                horizontalLineTo(17.833f)
                horizontalLineTo(21.167f)
                close()
                moveTo(21.167f, 18.5f)
                verticalLineTo(16.75f)
                curveTo(21.167f, 16.167f, 20.833f, 15f, 19.5f, 15f)
                curveTo(18.167f, 15f, 17.833f, 16.167f, 17.833f, 16.75f)
                verticalLineTo(18.5f)
                horizontalLineTo(21.167f)
                close()
            }
        }.build()

        return _PrivateWifi!!
    }

@Suppress("ObjectPropertyName")
private var _PrivateWifi: ImageVector? = null
