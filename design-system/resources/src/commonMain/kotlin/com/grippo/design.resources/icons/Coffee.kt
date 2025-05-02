package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Coffee: ImageVector
    get() {
        if (_Coffee != null) {
            return _Coffee!!
        }
        _Coffee = ImageVector.Builder(
            name = "Coffee",
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
                moveTo(18f, 8f)
                horizontalLineTo(19f)
                curveTo(20.061f, 8f, 21.078f, 8.421f, 21.828f, 9.172f)
                curveTo(22.579f, 9.922f, 23f, 10.939f, 23f, 12f)
                curveTo(23f, 13.061f, 22.579f, 14.078f, 21.828f, 14.828f)
                curveTo(21.078f, 15.579f, 20.061f, 16f, 19f, 16f)
                horizontalLineTo(18f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 8f)
                horizontalLineTo(18f)
                verticalLineTo(17f)
                curveTo(18f, 18.061f, 17.579f, 19.078f, 16.828f, 19.828f)
                curveTo(16.078f, 20.579f, 15.061f, 21f, 14f, 21f)
                horizontalLineTo(6f)
                curveTo(4.939f, 21f, 3.922f, 20.579f, 3.172f, 19.828f)
                curveTo(2.421f, 19.078f, 2f, 18.061f, 2f, 17f)
                verticalLineTo(8f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 1f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 1f)
                verticalLineTo(4f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 1f)
                verticalLineTo(4f)
            }
        }.build()

        return _Coffee!!
    }

@Suppress("ObjectPropertyName")
private var _Coffee: ImageVector? = null
