package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Leaderboard: ImageVector
    get() {
        if (_Leaderboard != null) {
            return _Leaderboard!!
        }
        _Leaderboard = ImageVector.Builder(
            name = "Leaderboard",
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
                moveTo(15f, 19f)
                horizontalLineTo(9f)
                verticalLineTo(12.5f)
                verticalLineTo(8.6f)
                curveTo(9f, 8.269f, 9.269f, 8f, 9.6f, 8f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 8f, 15f, 8.269f, 15f, 8.6f)
                verticalLineTo(14.5f)
                verticalLineTo(19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 5f)
                horizontalLineTo(9f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.4f, 19f)
                horizontalLineTo(15f)
                verticalLineTo(15.1f)
                curveTo(15f, 14.769f, 15.269f, 14.5f, 15.6f, 14.5f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 14.5f, 21f, 14.769f, 21f, 15.1f)
                verticalLineTo(18.4f)
                curveTo(21f, 18.731f, 20.731f, 19f, 20.4f, 19f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 19f)
                verticalLineTo(13.1f)
                curveTo(9f, 12.769f, 8.731f, 12.5f, 8.4f, 12.5f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 12.5f, 3f, 12.769f, 3f, 13.1f)
                verticalLineTo(18.4f)
                curveTo(3f, 18.731f, 3.269f, 19f, 3.6f, 19f)
                horizontalLineTo(9f)
                close()
            }
        }.build()

        return _Leaderboard!!
    }

@Suppress("ObjectPropertyName")
private var _Leaderboard: ImageVector? = null
