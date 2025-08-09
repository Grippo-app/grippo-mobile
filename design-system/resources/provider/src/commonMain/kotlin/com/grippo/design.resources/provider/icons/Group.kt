package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Group: ImageVector
    get() {
        if (_Group != null) {
            return _Group!!
        }
        _Group = ImageVector.Builder(
            name = "Group",
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
                moveTo(1f, 20f)
                verticalLineTo(19f)
                curveTo(1f, 15.134f, 4.134f, 12f, 8f, 12f)
                curveTo(11.866f, 12f, 15f, 15.134f, 15f, 19f)
                verticalLineTo(20f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(13f, 14f)
                curveTo(13f, 11.239f, 15.239f, 9f, 18f, 9f)
                curveTo(20.761f, 9f, 23f, 11.239f, 23f, 14f)
                verticalLineTo(14.5f)
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
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 9f)
                curveTo(19.657f, 9f, 21f, 7.657f, 21f, 6f)
                curveTo(21f, 4.343f, 19.657f, 3f, 18f, 3f)
                curveTo(16.343f, 3f, 15f, 4.343f, 15f, 6f)
                curveTo(15f, 7.657f, 16.343f, 9f, 18f, 9f)
                close()
            }
        }.build()

        return _Group!!
    }

@Suppress("ObjectPropertyName")
private var _Group: ImageVector? = null
