package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Tune: ImageVector
    get() {
        if (_Tune != null) {
            return _Tune!!
        }
        _Tune = ImageVector.Builder(
            name = "Tune",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 6f)
                horizontalLineTo(13.5f)
                moveTo(18.5f, 6f)
                horizontalLineTo(20.5f)
                moveTo(16f, 3.5f)
                curveTo(17.381f, 3.5f, 18.5f, 4.619f, 18.5f, 6f)
                curveTo(18.5f, 7.381f, 17.381f, 8.5f, 16f, 8.5f)
                curveTo(14.619f, 8.5f, 13.5f, 7.381f, 13.5f, 6f)
                curveTo(13.5f, 4.619f, 14.619f, 3.5f, 16f, 3.5f)
                close()
                moveTo(3.5f, 12f)
                horizontalLineTo(5.5f)
                moveTo(10.5f, 12f)
                horizontalLineTo(20.5f)
                moveTo(8f, 9.5f)
                curveTo(9.381f, 9.5f, 10.5f, 10.619f, 10.5f, 12f)
                curveTo(10.5f, 13.381f, 9.381f, 14.5f, 8f, 14.5f)
                curveTo(6.619f, 14.5f, 5.5f, 13.381f, 5.5f, 12f)
                curveTo(5.5f, 10.619f, 6.619f, 9.5f, 8f, 9.5f)
                close()
                moveTo(3.5f, 18f)
                horizontalLineTo(11.5f)
                moveTo(16.5f, 18f)
                horizontalLineTo(20.5f)
                moveTo(14f, 15.5f)
                curveTo(15.381f, 15.5f, 16.5f, 16.619f, 16.5f, 18f)
                curveTo(16.5f, 19.381f, 15.381f, 20.5f, 14f, 20.5f)
                curveTo(12.619f, 20.5f, 11.5f, 19.381f, 11.5f, 18f)
                curveTo(11.5f, 16.619f, 12.619f, 15.5f, 14f, 15.5f)
                close()
            }
        }.build()

        return _Tune!!
    }

@Suppress("ObjectPropertyName")
private var _Tune: ImageVector? = null
