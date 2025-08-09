package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.PeaceHand: ImageVector
    get() {
        if (_PeaceHand != null) {
            return _PeaceHand!!
        }
        _PeaceHand = ImageVector.Builder(
            name = "PeaceHand",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(14.149f, 9.472f)
                verticalLineTo(3.612f)
                curveTo(14.149f, 2.722f, 13.427f, 2f, 12.537f, 2f)
                curveTo(11.647f, 2f, 10.926f, 2.722f, 10.926f, 3.612f)
                verticalLineTo(8.446f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16.346f, 12.841f)
                lineTo(18.522f, 5.589f)
                curveTo(18.775f, 4.743f, 18.289f, 3.853f, 17.439f, 3.61f)
                curveTo(16.594f, 3.369f, 15.714f, 3.861f, 15.478f, 4.708f)
                lineTo(14.148f, 9.472f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.619f, 9.25f)
                lineTo(8.675f, 11.591f)
                curveTo(9.04f, 12.4f, 8.682f, 13.352f, 7.874f, 13.72f)
                curveTo(7.062f, 14.09f, 6.103f, 13.73f, 5.737f, 12.916f)
                lineTo(4.681f, 10.575f)
                curveTo(4.316f, 9.765f, 4.674f, 8.814f, 5.482f, 8.446f)
                curveTo(6.294f, 8.076f, 7.253f, 8.436f, 7.619f, 9.25f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(11.719f, 12.262f)
                curveTo(11.924f, 11.694f, 11.9f, 11.069f, 11.652f, 10.519f)
                lineTo(10.579f, 8.139f)
                curveTo(10.218f, 7.339f, 9.276f, 6.985f, 8.478f, 7.348f)
                curveTo(7.665f, 7.719f, 7.319f, 8.688f, 7.714f, 9.49f)
                lineTo(7.849f, 9.765f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(13.857f, 17.677f)
                lineTo(14.349f, 16.693f)
                curveTo(14.398f, 16.595f, 14.346f, 16.476f, 14.241f, 16.445f)
                lineTo(10.69f, 15.401f)
                curveTo(9.979f, 15.192f, 9.518f, 14.504f, 9.596f, 13.766f)
                curveTo(9.684f, 12.929f, 10.428f, 12.319f, 11.266f, 12.397f)
                lineTo(16.054f, 12.842f)
                curveTo(16.054f, 12.842f, 19.863f, 13.428f, 18.545f, 17.237f)
                curveTo(17.226f, 21.046f, 16.787f, 22.365f, 13.857f, 22.365f)
                curveTo(11.952f, 22.365f, 9.169f, 22.365f, 9.169f, 22.365f)
                horizontalLineTo(8.876f)
                curveTo(6.529f, 22.365f, 4.627f, 20.463f, 4.627f, 18.116f)
                lineTo(4.48f, 9.912f)
            }
        }.build()

        return _PeaceHand!!
    }

@Suppress("ObjectPropertyName")
private var _PeaceHand: ImageVector? = null
