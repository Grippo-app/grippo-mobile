package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.OneFingerSelectHandGesture: ImageVector
    get() {
        if (_OneFingerSelectHandGesture != null) {
            return _OneFingerSelectHandGesture!!
        }
        _OneFingerSelectHandGesture = ImageVector.Builder(
            name = "OneFingerSelectHandGesture",
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
                moveTo(7.5f, 12f)
                lineTo(5.496f, 14.672f)
                curveTo(4.918f, 15.442f, 4.971f, 16.514f, 5.622f, 17.224f)
                lineTo(9.406f, 21.352f)
                curveTo(9.784f, 21.765f, 10.318f, 22f, 10.879f, 22f)
                curveTo(11.965f, 22f, 13.741f, 22f, 15.5f, 22f)
                curveTo(17.9f, 22f, 19.5f, 20f, 19.5f, 18f)
                curveTo(19.5f, 18f, 19.5f, 11.143f, 19.5f, 9.429f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.5f, 10f)
                curveTo(16.5f, 10f, 16.5f, 9.875f, 16.5f, 9.428f)
                curveTo(16.5f, 7.143f, 19.5f, 7.143f, 19.5f, 9.428f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.5f, 10f)
                curveTo(13.5f, 10f, 13.5f, 9.178f, 13.5f, 8.286f)
                curveTo(13.5f, 6f, 16.5f, 6f, 16.5f, 8.286f)
                curveTo(16.5f, 8.509f, 16.5f, 9.205f, 16.5f, 9.428f)
                curveTo(16.5f, 9.875f, 16.5f, 10f, 16.5f, 10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 10f)
                curveTo(10.5f, 10f, 10.5f, 8.616f, 10.5f, 7.5f)
                curveTo(10.5f, 5.214f, 13.5f, 5.214f, 13.5f, 7.5f)
                curveTo(13.5f, 7.5f, 13.5f, 8.063f, 13.5f, 8.286f)
                curveTo(13.5f, 9.179f, 13.5f, 10f, 13.5f, 10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.5f, 10f)
                curveTo(10.5f, 10f, 10.5f, 8.616f, 10.5f, 7.5f)
                curveTo(10.5f, 6.342f, 10.5f, 4.69f, 10.5f, 3.499f)
                curveTo(10.5f, 2.671f, 9.828f, 2f, 9f, 2f)
                curveTo(8.172f, 2f, 7.5f, 2.672f, 7.5f, 3.5f)
                verticalLineTo(12f)
                verticalLineTo(15f)
            }
        }.build()

        return _OneFingerSelectHandGesture!!
    }

@Suppress("ObjectPropertyName")
private var _OneFingerSelectHandGesture: ImageVector? = null
