package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.JournalPage: ImageVector
    get() {
        if (_JournalPage != null) {
            return _JournalPage!!
        }
        _JournalPage = ImageVector.Builder(
            name = "JournalPage",
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
                moveTo(6f, 6f)
                horizontalLineTo(14f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 10f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 18f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 21.4f)
                verticalLineTo(2.6f)
                curveTo(2f, 2.269f, 2.269f, 2f, 2.6f, 2f)
                horizontalLineTo(18.251f)
                curveTo(18.411f, 2f, 18.563f, 2.063f, 18.676f, 2.176f)
                lineTo(21.824f, 5.324f)
                curveTo(21.937f, 5.437f, 22f, 5.589f, 22f, 5.749f)
                verticalLineTo(21.4f)
                curveTo(22f, 21.731f, 21.731f, 22f, 21.4f, 22f)
                horizontalLineTo(2.6f)
                curveTo(2.269f, 22f, 2f, 21.731f, 2f, 21.4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 5.4f)
                verticalLineTo(2.354f)
                curveTo(18f, 2.158f, 18.158f, 2f, 18.354f, 2f)
                curveTo(18.447f, 2f, 18.537f, 2.037f, 18.604f, 2.104f)
                lineTo(21.896f, 5.396f)
                curveTo(21.963f, 5.463f, 22f, 5.553f, 22f, 5.646f)
                curveTo(22f, 5.842f, 21.842f, 6f, 21.646f, 6f)
                horizontalLineTo(18.6f)
                curveTo(18.269f, 6f, 18f, 5.731f, 18f, 5.4f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0F172A)),
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 18f)
                verticalLineTo(14f)
                horizontalLineTo(8f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
            }
        }.build()

        return _JournalPage!!
    }

@Suppress("ObjectPropertyName")
private var _JournalPage: ImageVector? = null
