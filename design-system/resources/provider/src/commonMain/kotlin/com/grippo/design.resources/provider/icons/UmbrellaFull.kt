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

public val AppIcon.UmbrellaFull: ImageVector
    get() {
        if (_UmbrellaFull != null) {
            return _UmbrellaFull!!
        }
        _UmbrellaFull = ImageVector.Builder(
            name = "UmbrellaFull",
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
                    strokeLineWidth = 1.5f
                ) {
                    moveTo(19.778f, 4.043f)
                    curveTo(17.701f, 2.081f, 14.938f, 1f, 12f, 1f)
                    curveTo(9.062f, 1f, 6.299f, 2.081f, 4.222f, 4.043f)
                    curveTo(2.144f, 6.006f, 1f, 8.616f, 1f, 11.391f)
                    curveTo(1f, 11.727f, 1.289f, 12f, 1.644f, 12f)
                    curveTo(2f, 12f, 2.289f, 11.727f, 2.289f, 11.391f)
                    curveTo(2.289f, 10.378f, 3.161f, 9.554f, 4.233f, 9.554f)
                    curveTo(6.126f, 9.554f, 5.431f, 12f, 6.822f, 12f)
                    curveTo(8.213f, 12f, 7.519f, 9.554f, 9.411f, 9.554f)
                    curveTo(11.304f, 9.554f, 12f, 12f, 12f, 12f)
                    curveTo(12f, 12f, 12.696f, 9.554f, 14.589f, 9.554f)
                    curveTo(16.481f, 9.554f, 15.988f, 12f, 17.178f, 12f)
                    curveTo(18.368f, 12f, 17.874f, 9.554f, 19.767f, 9.554f)
                    curveTo(20.839f, 9.554f, 21.711f, 10.378f, 21.711f, 11.391f)
                    curveTo(21.711f, 11.727f, 22f, 12f, 22.356f, 12f)
                    curveTo(22.712f, 12f, 23f, 11.727f, 23f, 11.391f)
                    curveTo(23f, 8.616f, 21.856f, 6.006f, 19.778f, 4.043f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF0F172A)),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 12f)
                    curveTo(12f, 12f, 12f, 16.095f, 12f, 20f)
                    curveTo(12f, 24f, 6f, 24f, 6f, 20f)
                }
            }
        }.build()

        return _UmbrellaFull!!
    }

@Suppress("ObjectPropertyName")
private var _UmbrellaFull: ImageVector? = null
