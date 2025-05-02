package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Cast: ImageVector
    get() {
        if (_Cast != null) {
            return _Cast!!
        }
        _Cast = ImageVector.Builder(
            name = "Cast",
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
                moveTo(2f, 8f)
                verticalLineTo(6f)
                curveTo(2f, 5.47f, 2.211f, 4.961f, 2.586f, 4.586f)
                curveTo(2.961f, 4.211f, 3.47f, 4f, 4f, 4f)
                horizontalLineTo(20f)
                curveTo(20.53f, 4f, 21.039f, 4.211f, 21.414f, 4.586f)
                curveTo(21.789f, 4.961f, 22f, 5.47f, 22f, 6f)
                verticalLineTo(18f)
                curveTo(22f, 18.53f, 21.789f, 19.039f, 21.414f, 19.414f)
                curveTo(21.039f, 19.789f, 20.53f, 20f, 20f, 20f)
                horizontalLineTo(14f)
                moveTo(2f, 16.1f)
                curveTo(2.961f, 16.296f, 3.843f, 16.77f, 4.536f, 17.464f)
                curveTo(5.23f, 18.157f, 5.704f, 19.039f, 5.9f, 20f)
                lineTo(2f, 16.1f)
                close()
                moveTo(2f, 12.05f)
                curveTo(4.031f, 12.276f, 5.924f, 13.186f, 7.369f, 14.631f)
                curveTo(8.814f, 16.076f, 9.724f, 17.969f, 9.95f, 20f)
                lineTo(2f, 12.05f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2f, 20f)
                horizontalLineTo(2.01f)
            }
        }.build()

        return _Cast!!
    }

@Suppress("ObjectPropertyName")
private var _Cast: ImageVector? = null
