package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Voicemail: ImageVector
    get() {
        if (_Voicemail != null) {
            return _Voicemail!!
        }
        _Voicemail = ImageVector.Builder(
            name = "Voicemail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 16f)
                curveTo(7.985f, 16f, 10f, 13.985f, 10f, 11.5f)
                curveTo(10f, 9.015f, 7.985f, 7f, 5.5f, 7f)
                curveTo(3.015f, 7f, 1f, 9.015f, 1f, 11.5f)
                curveTo(1f, 13.985f, 3.015f, 16f, 5.5f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18.5f, 16f)
                curveTo(20.985f, 16f, 23f, 13.985f, 23f, 11.5f)
                curveTo(23f, 9.015f, 20.985f, 7f, 18.5f, 7f)
                curveTo(16.015f, 7f, 14f, 9.015f, 14f, 11.5f)
                curveTo(14f, 13.985f, 16.015f, 16f, 18.5f, 16f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 16f)
                horizontalLineTo(18.5f)
            }
        }.build()

        return _Voicemail!!
    }

@Suppress("ObjectPropertyName")
private var _Voicemail: ImageVector? = null
