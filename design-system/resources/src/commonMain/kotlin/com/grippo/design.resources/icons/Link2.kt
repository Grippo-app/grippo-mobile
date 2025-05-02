package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Link2: ImageVector
    get() {
        if (_Link2 != null) {
            return _Link2!!
        }
        _Link2 = ImageVector.Builder(
            name = "Link2",
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
                moveTo(9f, 17f)
                horizontalLineTo(6f)
                curveTo(5.343f, 17f, 4.693f, 16.871f, 4.087f, 16.619f)
                curveTo(3.48f, 16.368f, 2.929f, 16f, 2.464f, 15.535f)
                curveTo(1.527f, 14.598f, 1f, 13.326f, 1f, 12f)
                curveTo(1f, 10.674f, 1.527f, 9.402f, 2.464f, 8.464f)
                curveTo(3.402f, 7.527f, 4.674f, 7f, 6f, 7f)
                horizontalLineTo(9f)
                moveTo(15f, 7f)
                horizontalLineTo(18f)
                curveTo(18.657f, 7f, 19.307f, 7.129f, 19.913f, 7.381f)
                curveTo(20.52f, 7.632f, 21.071f, 8f, 21.535f, 8.464f)
                curveTo(22f, 8.929f, 22.368f, 9.48f, 22.619f, 10.087f)
                curveTo(22.871f, 10.693f, 23f, 11.343f, 23f, 12f)
                curveTo(23f, 12.657f, 22.871f, 13.307f, 22.619f, 13.913f)
                curveTo(22.368f, 14.52f, 22f, 15.071f, 21.535f, 15.535f)
                curveTo(21.071f, 16f, 20.52f, 16.368f, 19.913f, 16.619f)
                curveTo(19.307f, 16.871f, 18.657f, 17f, 18f, 17f)
                horizontalLineTo(15f)
                verticalLineTo(7f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 12f)
                horizontalLineTo(16f)
            }
        }.build()

        return _Link2!!
    }

@Suppress("ObjectPropertyName")
private var _Link2: ImageVector? = null
