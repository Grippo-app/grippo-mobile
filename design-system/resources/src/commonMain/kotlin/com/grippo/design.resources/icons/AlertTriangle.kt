package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.AlertTriangle: ImageVector
    get() {
        if (_AlertTriangle != null) {
            return _AlertTriangle!!
        }
        _AlertTriangle = ImageVector.Builder(
            name = "AlertTriangle",
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
                moveTo(10.29f, 3.86f)
                lineTo(1.82f, 18f)
                curveTo(1.645f, 18.302f, 1.553f, 18.645f, 1.552f, 18.994f)
                curveTo(1.551f, 19.344f, 1.642f, 19.687f, 1.814f, 19.99f)
                curveTo(1.987f, 20.294f, 2.237f, 20.547f, 2.538f, 20.724f)
                curveTo(2.839f, 20.901f, 3.181f, 20.996f, 3.53f, 21f)
                horizontalLineTo(20.47f)
                curveTo(20.819f, 20.996f, 21.161f, 20.901f, 21.462f, 20.724f)
                curveTo(21.763f, 20.547f, 22.013f, 20.294f, 22.186f, 19.99f)
                curveTo(22.358f, 19.687f, 22.449f, 19.344f, 22.448f, 18.994f)
                curveTo(22.447f, 18.645f, 22.355f, 18.302f, 22.18f, 18f)
                lineTo(13.71f, 3.86f)
                curveTo(13.532f, 3.566f, 13.281f, 3.323f, 12.981f, 3.154f)
                curveTo(12.682f, 2.986f, 12.344f, 2.897f, 12f, 2.897f)
                curveTo(11.656f, 2.897f, 11.318f, 2.986f, 11.019f, 3.154f)
                curveTo(10.719f, 3.323f, 10.468f, 3.566f, 10.29f, 3.86f)
                verticalLineTo(3.86f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 9f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 17f)
                horizontalLineTo(12.01f)
            }
        }.build()

        return _AlertTriangle!!
    }

@Suppress("ObjectPropertyName")
private var _AlertTriangle: ImageVector? = null
