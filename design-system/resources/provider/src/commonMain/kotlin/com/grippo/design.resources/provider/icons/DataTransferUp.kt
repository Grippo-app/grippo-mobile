package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.DataTransferUp: ImageVector
    get() {
        if (_DataTransferUp != null) {
            return _DataTransferUp!!
        }
        _DataTransferUp = ImageVector.Builder(
            name = "DataTransferUp",
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
                moveTo(7f, 4f)
                verticalLineTo(5f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 9f)
                verticalLineTo(10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 4f)
                lineTo(14f, 7f)
                moveTo(17f, 20f)
                verticalLineTo(4f)
                verticalLineTo(20f)
                close()
                moveTo(17f, 4f)
                lineTo(20f, 7f)
                lineTo(17f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 20f)
                lineTo(4f, 17f)
                moveTo(7f, 14f)
                verticalLineTo(20f)
                verticalLineTo(14f)
                close()
                moveTo(7f, 20f)
                lineTo(10f, 17f)
                lineTo(7f, 20f)
                close()
            }
        }.build()

        return _DataTransferUp!!
    }

@Suppress("ObjectPropertyName")
private var _DataTransferUp: ImageVector? = null
