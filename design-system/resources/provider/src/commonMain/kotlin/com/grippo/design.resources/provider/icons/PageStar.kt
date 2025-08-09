package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PageStar: ImageVector
    get() {
        if (_PageStar != null) {
            return _PageStar!!
        }
        _PageStar = ImageVector.Builder(
            name = "PageStar",
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
                moveTo(20f, 12f)
                verticalLineTo(5.749f)
                curveTo(20f, 5.589f, 19.937f, 5.437f, 19.824f, 5.324f)
                lineTo(16.676f, 2.176f)
                curveTo(16.563f, 2.063f, 16.411f, 2f, 16.251f, 2f)
                horizontalLineTo(4.6f)
                curveTo(4.269f, 2f, 4f, 2.269f, 4f, 2.6f)
                verticalLineTo(21.4f)
                curveTo(4f, 21.731f, 4.269f, 22f, 4.6f, 22f)
                horizontalLineTo(11f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 14f)
                horizontalLineTo(11f)
                moveTo(8f, 10f)
                horizontalLineTo(16f)
                horizontalLineTo(8f)
                close()
                moveTo(8f, 6f)
                horizontalLineTo(12f)
                horizontalLineTo(8f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 5.4f)
                verticalLineTo(2.354f)
                curveTo(16f, 2.158f, 16.158f, 2f, 16.354f, 2f)
                curveTo(16.447f, 2f, 16.537f, 2.037f, 16.604f, 2.104f)
                lineTo(19.896f, 5.396f)
                curveTo(19.963f, 5.463f, 20f, 5.553f, 20f, 5.646f)
                curveTo(20f, 5.842f, 19.842f, 6f, 19.646f, 6f)
                horizontalLineTo(16.6f)
                curveTo(16.269f, 6f, 16f, 5.731f, 16f, 5.4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.306f, 17.113f)
                lineTo(17.215f, 15.186f)
                curveTo(17.331f, 14.938f, 17.669f, 14.938f, 17.785f, 15.186f)
                lineTo(18.694f, 17.113f)
                lineTo(20.728f, 17.424f)
                curveTo(20.988f, 17.464f, 21.092f, 17.8f, 20.903f, 17.992f)
                lineTo(19.433f, 19.492f)
                lineTo(19.78f, 21.61f)
                curveTo(19.824f, 21.882f, 19.552f, 22.09f, 19.318f, 21.961f)
                lineTo(17.5f, 20.96f)
                lineTo(15.682f, 21.961f)
                curveTo(15.448f, 22.09f, 15.176f, 21.882f, 15.22f, 21.61f)
                lineTo(15.567f, 19.492f)
                lineTo(14.097f, 17.992f)
                curveTo(13.908f, 17.8f, 14.012f, 17.464f, 14.273f, 17.424f)
                lineTo(16.306f, 17.113f)
                close()
            }
        }.build()

        return _PageStar!!
    }

@Suppress("ObjectPropertyName")
private var _PageStar: ImageVector? = null
