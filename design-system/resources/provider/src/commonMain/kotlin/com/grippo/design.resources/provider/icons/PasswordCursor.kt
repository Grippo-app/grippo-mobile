package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PasswordCursor: ImageVector
    get() {
        if (_PasswordCursor != null) {
            return _PasswordCursor!!
        }
        _PasswordCursor = ImageVector.Builder(
            name = "PasswordCursor",
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
                moveTo(21f, 13f)
                verticalLineTo(8f)
                curveTo(21f, 6.895f, 20.105f, 6f, 19f, 6f)
                horizontalLineTo(5f)
                curveTo(3.895f, 6f, 3f, 6.895f, 3f, 8f)
                verticalLineTo(14f)
                curveTo(3f, 15.105f, 3.895f, 16f, 5f, 16f)
                horizontalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(20.879f, 16.917f)
                curveTo(21.373f, 17.221f, 21.342f, 17.96f, 20.834f, 18.018f)
                lineTo(18.267f, 18.309f)
                lineTo(17.116f, 20.621f)
                curveTo(16.888f, 21.08f, 16.183f, 20.855f, 16.066f, 20.287f)
                lineTo(14.811f, 14.171f)
                curveTo(14.712f, 13.691f, 15.144f, 13.389f, 15.561f, 13.646f)
                lineTo(20.879f, 16.917f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 11.01f)
                lineTo(12.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f, 11.01f)
                lineTo(16.01f, 10.999f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8f, 11.01f)
                lineTo(8.01f, 10.999f)
            }
        }.build()

        return _PasswordCursor!!
    }

@Suppress("ObjectPropertyName")
private var _PasswordCursor: ImageVector? = null
