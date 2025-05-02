package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.RotateCcw: ImageVector
    get() {
        if (_RotateCcw != null) {
            return _RotateCcw!!
        }
        _RotateCcw = ImageVector.Builder(
            name = "RotateCcw",
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
                moveTo(1f, 4f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.51f, 15f)
                curveTo(4.158f, 16.84f, 5.387f, 18.42f, 7.012f, 19.501f)
                curveTo(8.636f, 20.583f, 10.568f, 21.107f, 12.516f, 20.994f)
                curveTo(14.464f, 20.882f, 16.323f, 20.14f, 17.812f, 18.88f)
                curveTo(19.302f, 17.619f, 20.341f, 15.909f, 20.774f, 14.006f)
                curveTo(21.207f, 12.104f, 21.01f, 10.112f, 20.213f, 8.331f)
                curveTo(19.415f, 6.55f, 18.06f, 5.077f, 16.353f, 4.133f)
                curveTo(14.645f, 3.189f, 12.677f, 2.825f, 10.745f, 3.097f)
                curveTo(8.812f, 3.369f, 7.021f, 4.261f, 5.64f, 5.64f)
                lineTo(1f, 10f)
            }
        }.build()

        return _RotateCcw!!
    }

@Suppress("ObjectPropertyName")
private var _RotateCcw: ImageVector? = null
