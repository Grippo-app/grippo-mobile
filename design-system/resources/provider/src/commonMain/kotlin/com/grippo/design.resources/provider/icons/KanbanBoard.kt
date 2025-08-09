package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.KanbanBoard: ImageVector
    get() {
        if (_KanbanBoard != null) {
            return _KanbanBoard!!
        }
        _KanbanBoard = ImageVector.Builder(
            name = "KanbanBoard",
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
                moveTo(3f, 3.6f)
                verticalLineTo(20.4f)
                curveTo(3f, 20.731f, 3.269f, 21f, 3.6f, 21f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 21f, 21f, 20.731f, 21f, 20.4f)
                verticalLineTo(3.6f)
                curveTo(21f, 3.269f, 20.731f, 3f, 20.4f, 3f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 3f, 3f, 3.269f, 3f, 3.6f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 6f)
                verticalLineTo(16f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 6f)
                verticalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14f, 6f)
                verticalLineTo(13f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 6f)
                verticalLineTo(11f)
            }
        }.build()

        return _KanbanBoard!!
    }

@Suppress("ObjectPropertyName")
private var _KanbanBoard: ImageVector? = null
