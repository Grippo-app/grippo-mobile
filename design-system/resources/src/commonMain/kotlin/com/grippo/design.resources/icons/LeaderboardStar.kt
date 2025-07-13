package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.LeaderboardStar: ImageVector
    get() {
        if (_LeaderboardStar != null) {
            return _LeaderboardStar!!
        }
        _LeaderboardStar = ImageVector.Builder(
            name = "LeaderboardStar",
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
                moveTo(15f, 21f)
                horizontalLineTo(9f)
                verticalLineTo(12.6f)
                curveTo(9f, 12.269f, 9.269f, 12f, 9.6f, 12f)
                horizontalLineTo(14.4f)
                curveTo(14.731f, 12f, 15f, 12.269f, 15f, 12.6f)
                verticalLineTo(21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(20.4f, 21f)
                horizontalLineTo(15f)
                verticalLineTo(18.1f)
                curveTo(15f, 17.769f, 15.269f, 17.5f, 15.6f, 17.5f)
                horizontalLineTo(20.4f)
                curveTo(20.731f, 17.5f, 21f, 17.769f, 21f, 18.1f)
                verticalLineTo(20.4f)
                curveTo(21f, 20.731f, 20.731f, 21f, 20.4f, 21f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9f, 21f)
                verticalLineTo(16.1f)
                curveTo(9f, 15.769f, 8.731f, 15.5f, 8.4f, 15.5f)
                horizontalLineTo(3.6f)
                curveTo(3.269f, 15.5f, 3f, 15.769f, 3f, 16.1f)
                verticalLineTo(20.4f)
                curveTo(3f, 20.731f, 3.269f, 21f, 3.6f, 21f)
                horizontalLineTo(9f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10.806f, 5.113f)
                lineTo(11.715f, 3.186f)
                curveTo(11.831f, 2.938f, 12.169f, 2.938f, 12.285f, 3.186f)
                lineTo(13.194f, 5.113f)
                lineTo(15.227f, 5.424f)
                curveTo(15.488f, 5.464f, 15.592f, 5.8f, 15.403f, 5.992f)
                lineTo(13.933f, 7.492f)
                lineTo(14.28f, 9.61f)
                curveTo(14.324f, 9.882f, 14.052f, 10.09f, 13.818f, 9.961f)
                lineTo(12f, 8.96f)
                lineTo(10.182f, 9.961f)
                curveTo(9.949f, 10.09f, 9.676f, 9.882f, 9.72f, 9.61f)
                lineTo(10.067f, 7.492f)
                lineTo(8.597f, 5.992f)
                curveTo(8.408f, 5.8f, 8.512f, 5.464f, 8.772f, 5.424f)
                lineTo(10.806f, 5.113f)
                close()
            }
        }.build()

        return _LeaderboardStar!!
    }

@Suppress("ObjectPropertyName")
private var _LeaderboardStar: ImageVector? = null
