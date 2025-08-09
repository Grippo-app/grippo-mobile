package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AppleHalf: ImageVector
    get() {
        if (_AppleHalf != null) {
            return _AppleHalf!!
        }
        _AppleHalf = ImageVector.Builder(
            name = "AppleHalf",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12.147f, 21.265f)
                lineTo(12f, 21.235f)
                lineTo(11.853f, 21.265f)
                curveTo(9.476f, 21.74f, 7.233f, 21.476f, 5.594f, 20.164f)
                curveTo(3.963f, 18.86f, 2.75f, 16.374f, 2.75f, 12f)
                curveTo(2.75f, 7.527f, 3.758f, 5.71f, 5.085f, 5.046f)
                curveTo(5.78f, 4.699f, 6.678f, 4.598f, 7.82f, 4.729f)
                curveTo(8.961f, 4.861f, 10.278f, 5.217f, 11.763f, 5.711f)
                lineTo(12.024f, 5.798f)
                lineTo(12.278f, 5.696f)
                curveTo(14.76f, 4.704f, 16.991f, 4.323f, 18.558f, 5.055f)
                curveTo(20.027f, 5.74f, 21.25f, 7.593f, 21.25f, 12f)
                curveTo(21.25f, 16.374f, 20.037f, 18.86f, 18.406f, 20.164f)
                curveTo(16.767f, 21.476f, 14.524f, 21.74f, 12.147f, 21.265f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 5.5f)
                curveTo(12f, 3f, 11f, 2f, 9f, 2f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 6f)
                verticalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 12f)
                verticalLineTo(14f)
            }
        }.build()

        return _AppleHalf!!
    }

@Suppress("ObjectPropertyName")
private var _AppleHalf: ImageVector? = null
