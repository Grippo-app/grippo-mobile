package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.RemoveUser: ImageVector
    get() {
        if (_RemoveUser != null) {
            return _RemoveUser!!
        }
        _RemoveUser = ImageVector.Builder(
            name = "RemoveUser",
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
                moveTo(20.743f, 10f)
                lineTo(22.864f, 12.121f)
                moveTo(18.621f, 12.121f)
                lineTo(20.743f, 10f)
                lineTo(18.621f, 12.121f)
                close()
                moveTo(22.864f, 7.879f)
                lineTo(20.743f, 10f)
                lineTo(22.864f, 7.879f)
                close()
                moveTo(20.743f, 10f)
                lineTo(18.621f, 7.879f)
                lineTo(20.743f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 20f)
                verticalLineTo(19f)
                curveTo(1f, 15.134f, 4.134f, 12f, 8f, 12f)
                curveTo(11.866f, 12f, 15f, 15.134f, 15f, 19f)
                verticalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 12f)
                curveTo(10.209f, 12f, 12f, 10.209f, 12f, 8f)
                curveTo(12f, 5.791f, 10.209f, 4f, 8f, 4f)
                curveTo(5.791f, 4f, 4f, 5.791f, 4f, 8f)
                curveTo(4f, 10.209f, 5.791f, 12f, 8f, 12f)
                close()
            }
        }.build()

        return _RemoveUser!!
    }

@Suppress("ObjectPropertyName")
private var _RemoveUser: ImageVector? = null
