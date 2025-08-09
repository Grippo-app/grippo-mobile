package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.BookStack: ImageVector
    get() {
        if (_BookStack != null) {
            return _BookStack!!
        }
        _BookStack = ImageVector.Builder(
            name = "BookStack",
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
                moveTo(5f, 19.5f)
                verticalLineTo(5f)
                curveTo(5f, 3.895f, 5.895f, 3f, 7f, 3f)
                horizontalLineTo(18.4f)
                curveTo(18.731f, 3f, 19f, 3.269f, 19f, 3.6f)
                verticalLineTo(21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 7f)
                horizontalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 15f)
                horizontalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 18f)
                horizontalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.5f, 21f)
                horizontalLineTo(19f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 18f)
                curveTo(5.5f, 18f, 5f, 17.328f, 5f, 16.5f)
                curveTo(5f, 15.672f, 5.5f, 15f, 6.5f, 15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 21f)
                curveTo(5.5f, 21f, 5f, 20.328f, 5f, 19.5f)
                curveTo(5f, 18.672f, 5.5f, 18f, 6.5f, 18f)
            }
        }.build()

        return _BookStack!!
    }

@Suppress("ObjectPropertyName")
private var _BookStack: ImageVector? = null
