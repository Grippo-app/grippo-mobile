package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Tag: ImageVector
    get() {
        if (_Tag != null) {
            return _Tag!!
        }
        _Tag = ImageVector.Builder(
            name = "Tag",
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
                moveTo(20.59f, 13.41f)
                lineTo(13.42f, 20.58f)
                curveTo(13.234f, 20.766f, 13.014f, 20.913f, 12.771f, 21.014f)
                curveTo(12.528f, 21.115f, 12.268f, 21.167f, 12.005f, 21.167f)
                curveTo(11.742f, 21.167f, 11.482f, 21.115f, 11.239f, 21.014f)
                curveTo(10.996f, 20.913f, 10.776f, 20.766f, 10.59f, 20.58f)
                lineTo(2f, 12f)
                verticalLineTo(2f)
                horizontalLineTo(12f)
                lineTo(20.59f, 10.59f)
                curveTo(20.962f, 10.965f, 21.172f, 11.472f, 21.172f, 12f)
                curveTo(21.172f, 12.528f, 20.962f, 13.035f, 20.59f, 13.41f)
                verticalLineTo(13.41f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 7f)
                horizontalLineTo(7.01f)
            }
        }.build()

        return _Tag!!
    }

@Suppress("ObjectPropertyName")
private var _Tag: ImageVector? = null
