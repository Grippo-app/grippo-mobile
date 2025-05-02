package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Slack: ImageVector
    get() {
        if (_Slack != null) {
            return _Slack!!
        }
        _Slack = ImageVector.Builder(
            name = "Slack",
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
                moveTo(14.5f, 10f)
                curveTo(13.67f, 10f, 13f, 9.33f, 13f, 8.5f)
                verticalLineTo(3.5f)
                curveTo(13f, 2.67f, 13.67f, 2f, 14.5f, 2f)
                curveTo(15.33f, 2f, 16f, 2.67f, 16f, 3.5f)
                verticalLineTo(8.5f)
                curveTo(16f, 9.33f, 15.33f, 10f, 14.5f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.5f, 10f)
                horizontalLineTo(19f)
                verticalLineTo(8.5f)
                curveTo(19f, 7.67f, 19.67f, 7f, 20.5f, 7f)
                curveTo(21.33f, 7f, 22f, 7.67f, 22f, 8.5f)
                curveTo(22f, 9.33f, 21.33f, 10f, 20.5f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.5f, 14f)
                curveTo(10.33f, 14f, 11f, 14.67f, 11f, 15.5f)
                verticalLineTo(20.5f)
                curveTo(11f, 21.33f, 10.33f, 22f, 9.5f, 22f)
                curveTo(8.67f, 22f, 8f, 21.33f, 8f, 20.5f)
                verticalLineTo(15.5f)
                curveTo(8f, 14.67f, 8.67f, 14f, 9.5f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 14f)
                horizontalLineTo(5f)
                verticalLineTo(15.5f)
                curveTo(5f, 16.33f, 4.33f, 17f, 3.5f, 17f)
                curveTo(2.67f, 17f, 2f, 16.33f, 2f, 15.5f)
                curveTo(2f, 14.67f, 2.67f, 14f, 3.5f, 14f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 14.5f)
                curveTo(14f, 13.67f, 14.67f, 13f, 15.5f, 13f)
                horizontalLineTo(20.5f)
                curveTo(21.33f, 13f, 22f, 13.67f, 22f, 14.5f)
                curveTo(22f, 15.33f, 21.33f, 16f, 20.5f, 16f)
                horizontalLineTo(15.5f)
                curveTo(14.67f, 16f, 14f, 15.33f, 14f, 14.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 19f)
                horizontalLineTo(14f)
                verticalLineTo(20.5f)
                curveTo(14f, 21.33f, 14.67f, 22f, 15.5f, 22f)
                curveTo(16.33f, 22f, 17f, 21.33f, 17f, 20.5f)
                curveTo(17f, 19.67f, 16.33f, 19f, 15.5f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 9.5f)
                curveTo(10f, 8.67f, 9.33f, 8f, 8.5f, 8f)
                horizontalLineTo(3.5f)
                curveTo(2.67f, 8f, 2f, 8.67f, 2f, 9.5f)
                curveTo(2f, 10.33f, 2.67f, 11f, 3.5f, 11f)
                horizontalLineTo(8.5f)
                curveTo(9.33f, 11f, 10f, 10.33f, 10f, 9.5f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 5f)
                horizontalLineTo(10f)
                verticalLineTo(3.5f)
                curveTo(10f, 2.67f, 9.33f, 2f, 8.5f, 2f)
                curveTo(7.67f, 2f, 7f, 2.67f, 7f, 3.5f)
                curveTo(7f, 4.33f, 7.67f, 5f, 8.5f, 5f)
                close()
            }
        }.build()

        return _Slack!!
    }

@Suppress("ObjectPropertyName")
private var _Slack: ImageVector? = null
