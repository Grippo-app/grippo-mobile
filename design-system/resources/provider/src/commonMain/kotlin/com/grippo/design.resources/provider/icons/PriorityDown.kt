package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.PriorityDown: ImageVector
    get() {
        if (_PriorityDown != null) {
            return _PriorityDown!!
        }
        _PriorityDown = ImageVector.Builder(
            name = "PriorityDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(11.576f, 1.424f)
                    curveTo(11.81f, 1.19f, 12.19f, 1.19f, 12.424f, 1.424f)
                    lineTo(22.576f, 11.576f)
                    curveTo(22.81f, 11.81f, 22.81f, 12.19f, 22.576f, 12.424f)
                    lineTo(12.424f, 22.576f)
                    curveTo(12.19f, 22.81f, 11.81f, 22.81f, 11.576f, 22.576f)
                    lineTo(1.424f, 12.424f)
                    curveTo(1.19f, 12.19f, 1.19f, 11.81f, 1.424f, 11.576f)
                    lineTo(11.576f, 1.424f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 16f)
                    verticalLineTo(7f)
                    moveTo(12f, 16f)
                    lineTo(16f, 12f)
                    lineTo(12f, 16f)
                    close()
                    moveTo(12f, 16f)
                    lineTo(8f, 11.833f)
                    lineTo(12f, 16f)
                    close()
                }
            }
        }.build()

        return _PriorityDown!!
    }

@Suppress("ObjectPropertyName")
private var _PriorityDown: ImageVector? = null
