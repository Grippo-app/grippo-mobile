package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.AddMediaImage: ImageVector
    get() {
        if (_AddMediaImage != null) {
            return _AddMediaImage!!
        }
        _AddMediaImage = ImageVector.Builder(
            name = "AddMediaImage",
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
                moveTo(13f, 21f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 21f, 3f, 20.731f, 3f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(3f, 3.269f, 3.269f, 3f, 3.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 16f)
                lineTo(10f, 13f)
                lineTo(15.5f, 15.5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 10f)
                curveTo(14.895f, 10f, 14f, 9.105f, 14f, 8f)
                curveTo(14f, 6.895f, 14.895f, 6f, 16f, 6f)
                curveTo(17.105f, 6f, 18f, 6.895f, 18f, 8f)
                curveTo(18f, 9.105f, 17.105f, 10f, 16f, 10f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 19f)
                verticalLineTo(22f)
                moveTo(16f, 19f)
                horizontalLineTo(19f)
                horizontalLineTo(16f)
                close()
                moveTo(22f, 19f)
                horizontalLineTo(19f)
                horizontalLineTo(22f)
                close()
                moveTo(19f, 19f)
                verticalLineTo(16f)
                verticalLineTo(19f)
                close()
            }
        }.build()

        return _AddMediaImage!!
    }

@Suppress("ObjectPropertyName")
private var _AddMediaImage: ImageVector? = null
