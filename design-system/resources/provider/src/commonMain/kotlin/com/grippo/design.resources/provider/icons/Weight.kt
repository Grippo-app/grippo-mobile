package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Weight: ImageVector
    get() {
        if (_Weight != null) {
            return _Weight!!
        }
        _Weight = ImageVector.Builder(
            name = "Weight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 3f)
                horizontalLineTo(17f)
                curveTo(19.209f, 3f, 21f, 4.791f, 21f, 7f)
                verticalLineTo(17f)
                curveTo(21f, 19.209f, 19.209f, 21f, 17f, 21f)
                horizontalLineTo(7f)
                curveTo(4.791f, 21f, 3f, 19.209f, 3f, 17f)
                verticalLineTo(7f)
                curveTo(3f, 4.791f, 4.791f, 3f, 7f, 3f)
                close()
                moveTo(7f, 9f)
                curveTo(8.2f, 7.7f, 10f, 7f, 12f, 7f)
                curveTo(14f, 7f, 15.8f, 7.7f, 17f, 9f)
                lineTo(15f, 12f)
                horizontalLineTo(9f)
                close()
                moveTo(12f, 8f)
                verticalLineTo(10f)
            }
        }.build()

        return _Weight!!
    }

@Suppress("ObjectPropertyName")
private var _Weight: ImageVector? = null
