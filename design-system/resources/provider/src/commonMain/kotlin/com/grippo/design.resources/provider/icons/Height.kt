package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Height: ImageVector
    get() {
        if (_Height != null) {
            return _Height!!
        }
        _Height = ImageVector.Builder(
            name = "Height",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).apply {
            group(
                translationX = 2.5f,
                translationY = 8.5f,
            ) {
                path(
                    stroke = SolidColor(Color(0xFF141B34)),
                    strokeLineWidth = 3f,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(7.5f, 11.458f)
                    curveTo(7.5f, 13.69f, 11.082f, 15.5f, 15.5f, 15.5f)
                    verticalLineTo(11.458f)
                    curveTo(15.5f, 9.469f, 15.5f, 8.475f, 14.705f, 7.868f)
                    curveTo(13.91f, 7.262f, 13.122f, 7.489f, 11.546f, 7.942f)
                    curveTo(9.129f, 8.638f, 7.5f, 9.952f, 7.5f, 11.458f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF141B34)),
                    strokeLineWidth = 3f
                ) {
                    moveTo(29.5f, 8.5f)
                    curveTo(29.5f, 12.366f, 23.232f, 15.5f, 15.5f, 15.5f)
                    curveTo(7.768f, 15.5f, 1.5f, 12.366f, 1.5f, 8.5f)
                    curveTo(1.5f, 4.634f, 7.768f, 1.5f, 15.5f, 1.5f)
                    curveTo(23.232f, 1.5f, 29.5f, 4.634f, 29.5f, 8.5f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF141B34)),
                    strokeLineWidth = 3f
                ) {
                    moveTo(1.5f, 9.5f)
                    verticalLineTo(22.833f)
                    curveTo(1.5f, 26.515f, 7.768f, 29.5f, 15.5f, 29.5f)
                    horizontalLineTo(37.5f)
                    curveTo(39.386f, 29.5f, 40.328f, 29.5f, 40.914f, 28.914f)
                    curveTo(41.5f, 28.328f, 41.5f, 27.386f, 41.5f, 25.5f)
                    verticalLineTo(19.5f)
                    curveTo(41.5f, 17.614f, 41.5f, 16.672f, 40.914f, 16.086f)
                    curveTo(40.328f, 15.5f, 39.386f, 15.5f, 37.5f, 15.5f)
                    horizontalLineTo(15.5f)
                }
                path(
                    stroke = SolidColor(Color(0xFF141B34)),
                    strokeLineWidth = 3f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(33.5f, 29.5f)
                    verticalLineTo(25.5f)
                    moveTo(25.5f, 29.5f)
                    verticalLineTo(25.5f)
                    moveTo(17.5f, 29.5f)
                    verticalLineTo(25.5f)
                    moveTo(9.5f, 28.5f)
                    verticalLineTo(24.5f)
                }
            }
        }.build()

        return _Height!!
    }

@Suppress("ObjectPropertyName")
private var _Height: ImageVector? = null
