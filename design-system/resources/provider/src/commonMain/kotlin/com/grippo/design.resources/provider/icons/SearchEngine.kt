package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.SearchEngine: ImageVector
    get() {
        if (_SearchEngine != null) {
            return _SearchEngine!!
        }
        _SearchEngine = ImageVector.Builder(
            name = "SearchEngine",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(3f, 19f)
                verticalLineTo(5f)
                curveTo(3f, 3.895f, 3.895f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.105f, 3f, 21f, 3.895f, 21f, 5f)
                verticalLineTo(19f)
                curveTo(21f, 20.105f, 20.105f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.895f, 21f, 3f, 20.105f, 3f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.856f, 13.85f)
                lineTo(16f, 16f)
                moveTo(13.856f, 13.85f)
                curveTo(14.475f, 13.229f, 14.857f, 12.374f, 14.857f, 11.429f)
                curveTo(14.857f, 9.535f, 13.322f, 8f, 11.429f, 8f)
                curveTo(9.535f, 8f, 8f, 9.535f, 8f, 11.429f)
                curveTo(8f, 13.322f, 9.535f, 14.857f, 11.429f, 14.857f)
                curveTo(12.377f, 14.857f, 13.236f, 14.472f, 13.856f, 13.85f)
                close()
            }
        }.build()

        return _SearchEngine!!
    }

@Suppress("ObjectPropertyName")
private var _SearchEngine: ImageVector? = null
