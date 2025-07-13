package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.WifiIssue: ImageVector
    get() {
        if (_WifiIssue != null) {
            return _WifiIssue!!
        }
        _WifiIssue = ImageVector.Builder(
            name = "WifiIssue",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(2.126f, 8.324f)
                curveTo(1.926f, 8.062f, 1.971f, 7.719f, 2.212f, 7.535f)
                curveTo(5.29f, 5.178f, 8.553f, 4f, 11.999f, 4f)
                curveTo(15.446f, 4f, 18.708f, 5.178f, 21.787f, 7.535f)
                curveTo(22.039f, 7.747f, 22.068f, 8.093f, 21.872f, 8.324f)
                lineTo(12.417f, 19.497f)
                curveTo(12.398f, 19.52f, 12.376f, 19.541f, 12.353f, 19.561f)
                curveTo(12.122f, 19.756f, 11.776f, 19.728f, 11.581f, 19.497f)
                lineTo(2.126f, 8.324f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 8f)
                verticalLineTo(10f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 14.01f)
                lineTo(12.01f, 13.999f)
            }
        }.build()

        return _WifiIssue!!
    }

@Suppress("ObjectPropertyName")
private var _WifiIssue: ImageVector? = null
