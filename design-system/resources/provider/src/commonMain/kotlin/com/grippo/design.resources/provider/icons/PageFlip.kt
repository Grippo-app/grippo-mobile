package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PageFlip: ImageVector
    get() {
        if (_PageFlip != null) {
            return _PageFlip!!
        }
        _PageFlip = ImageVector.Builder(
            name = "PageFlip",
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
                moveTo(12f, 11f)
                horizontalLineTo(14.5f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 7f)
                horizontalLineTo(14.5f)
                horizontalLineTo(17f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 15f)
                verticalLineTo(3.6f)
                curveTo(8f, 3.269f, 8.269f, 3f, 8.6f, 3f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 3f, 21f, 3.269f, 21f, 3.6f)
                verticalLineTo(17f)
                curveTo(21f, 19.209f, 19.209f, 21f, 17f, 21f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 15f)
                horizontalLineTo(8f)
                horizontalLineTo(12.4f)
                curveTo(12.731f, 15f, 13.003f, 15.267f, 13.03f, 15.597f)
                curveTo(13.153f, 17.115f, 13.781f, 21f, 17f, 21f)
                horizontalLineTo(8f)
                horizontalLineTo(6f)
                curveTo(4.343f, 21f, 3f, 19.657f, 3f, 18f)
                verticalLineTo(17f)
                curveTo(3f, 15.895f, 3.895f, 15f, 5f, 15f)
                close()
            }
        }.build()

        return _PageFlip!!
    }

@Suppress("ObjectPropertyName")
private var _PageFlip: ImageVector? = null
