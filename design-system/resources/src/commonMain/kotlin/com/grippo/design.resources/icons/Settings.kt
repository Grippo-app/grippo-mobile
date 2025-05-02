package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.AppIcon

public val AppIcon.Settings: ImageVector
    get() {
        if (_Settings != null) {
            return _Settings!!
        }
        _Settings = ImageVector.Builder(
            name = "Settings",
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
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(12f, 15f)
                    curveTo(13.657f, 15f, 15f, 13.657f, 15f, 12f)
                    curveTo(15f, 10.343f, 13.657f, 9f, 12f, 9f)
                    curveTo(10.343f, 9f, 9f, 10.343f, 9f, 12f)
                    curveTo(9f, 13.657f, 10.343f, 15f, 12f, 15f)
                    close()
                }
                path(
                    stroke = SolidColor(Color(0xFF000000)),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round
                ) {
                    moveTo(19.4f, 15f)
                    curveTo(19.267f, 15.302f, 19.227f, 15.636f, 19.286f, 15.961f)
                    curveTo(19.345f, 16.285f, 19.5f, 16.584f, 19.73f, 16.82f)
                    lineTo(19.79f, 16.88f)
                    curveTo(19.976f, 17.066f, 20.124f, 17.286f, 20.224f, 17.529f)
                    curveTo(20.325f, 17.772f, 20.377f, 18.032f, 20.377f, 18.295f)
                    curveTo(20.377f, 18.558f, 20.325f, 18.818f, 20.224f, 19.061f)
                    curveTo(20.124f, 19.304f, 19.976f, 19.524f, 19.79f, 19.71f)
                    curveTo(19.604f, 19.896f, 19.384f, 20.044f, 19.141f, 20.144f)
                    curveTo(18.898f, 20.245f, 18.638f, 20.297f, 18.375f, 20.297f)
                    curveTo(18.112f, 20.297f, 17.852f, 20.245f, 17.609f, 20.144f)
                    curveTo(17.366f, 20.044f, 17.146f, 19.896f, 16.96f, 19.71f)
                    lineTo(16.9f, 19.65f)
                    curveTo(16.664f, 19.42f, 16.365f, 19.265f, 16.041f, 19.206f)
                    curveTo(15.716f, 19.147f, 15.382f, 19.187f, 15.08f, 19.32f)
                    curveTo(14.784f, 19.447f, 14.532f, 19.657f, 14.354f, 19.926f)
                    curveTo(14.177f, 20.194f, 14.081f, 20.508f, 14.08f, 20.83f)
                    verticalLineTo(21f)
                    curveTo(14.08f, 21.53f, 13.869f, 22.039f, 13.494f, 22.414f)
                    curveTo(13.119f, 22.789f, 12.61f, 23f, 12.08f, 23f)
                    curveTo(11.55f, 23f, 11.041f, 22.789f, 10.666f, 22.414f)
                    curveTo(10.291f, 22.039f, 10.08f, 21.53f, 10.08f, 21f)
                    verticalLineTo(20.91f)
                    curveTo(10.072f, 20.579f, 9.965f, 20.258f, 9.773f, 19.989f)
                    curveTo(9.58f, 19.719f, 9.311f, 19.514f, 9f, 19.4f)
                    curveTo(8.698f, 19.267f, 8.364f, 19.227f, 8.039f, 19.286f)
                    curveTo(7.715f, 19.345f, 7.416f, 19.5f, 7.18f, 19.73f)
                    lineTo(7.12f, 19.79f)
                    curveTo(6.934f, 19.976f, 6.714f, 20.124f, 6.471f, 20.224f)
                    curveTo(6.228f, 20.325f, 5.968f, 20.377f, 5.705f, 20.377f)
                    curveTo(5.442f, 20.377f, 5.182f, 20.325f, 4.939f, 20.224f)
                    curveTo(4.696f, 20.124f, 4.476f, 19.976f, 4.29f, 19.79f)
                    curveTo(4.104f, 19.604f, 3.957f, 19.384f, 3.856f, 19.141f)
                    curveTo(3.755f, 18.898f, 3.703f, 18.638f, 3.703f, 18.375f)
                    curveTo(3.703f, 18.112f, 3.755f, 17.852f, 3.856f, 17.609f)
                    curveTo(3.957f, 17.366f, 4.104f, 17.146f, 4.29f, 16.96f)
                    lineTo(4.35f, 16.9f)
                    curveTo(4.581f, 16.664f, 4.735f, 16.365f, 4.794f, 16.041f)
                    curveTo(4.853f, 15.716f, 4.813f, 15.382f, 4.68f, 15.08f)
                    curveTo(4.553f, 14.784f, 4.343f, 14.532f, 4.074f, 14.354f)
                    curveTo(3.806f, 14.177f, 3.492f, 14.081f, 3.17f, 14.08f)
                    horizontalLineTo(3f)
                    curveTo(2.47f, 14.08f, 1.961f, 13.869f, 1.586f, 13.494f)
                    curveTo(1.211f, 13.119f, 1f, 12.61f, 1f, 12.08f)
                    curveTo(1f, 11.55f, 1.211f, 11.041f, 1.586f, 10.666f)
                    curveTo(1.961f, 10.291f, 2.47f, 10.08f, 3f, 10.08f)
                    horizontalLineTo(3.09f)
                    curveTo(3.421f, 10.072f, 3.742f, 9.965f, 4.011f, 9.773f)
                    curveTo(4.281f, 9.58f, 4.486f, 9.311f, 4.6f, 9f)
                    curveTo(4.733f, 8.698f, 4.773f, 8.364f, 4.714f, 8.039f)
                    curveTo(4.655f, 7.715f, 4.501f, 7.416f, 4.27f, 7.18f)
                    lineTo(4.21f, 7.12f)
                    curveTo(4.024f, 6.934f, 3.877f, 6.714f, 3.776f, 6.471f)
                    curveTo(3.675f, 6.228f, 3.623f, 5.968f, 3.623f, 5.705f)
                    curveTo(3.623f, 5.442f, 3.675f, 5.182f, 3.776f, 4.939f)
                    curveTo(3.877f, 4.696f, 4.024f, 4.476f, 4.21f, 4.29f)
                    curveTo(4.396f, 4.104f, 4.616f, 3.957f, 4.859f, 3.856f)
                    curveTo(5.102f, 3.755f, 5.362f, 3.703f, 5.625f, 3.703f)
                    curveTo(5.888f, 3.703f, 6.148f, 3.755f, 6.391f, 3.856f)
                    curveTo(6.634f, 3.957f, 6.854f, 4.104f, 7.04f, 4.29f)
                    lineTo(7.1f, 4.35f)
                    curveTo(7.336f, 4.581f, 7.635f, 4.735f, 7.959f, 4.794f)
                    curveTo(8.284f, 4.853f, 8.618f, 4.813f, 8.92f, 4.68f)
                    horizontalLineTo(9f)
                    curveTo(9.296f, 4.553f, 9.548f, 4.343f, 9.726f, 4.074f)
                    curveTo(9.903f, 3.806f, 9.999f, 3.492f, 10f, 3.17f)
                    verticalLineTo(3f)
                    curveTo(10f, 2.47f, 10.211f, 1.961f, 10.586f, 1.586f)
                    curveTo(10.961f, 1.211f, 11.47f, 1f, 12f, 1f)
                    curveTo(12.53f, 1f, 13.039f, 1.211f, 13.414f, 1.586f)
                    curveTo(13.789f, 1.961f, 14f, 2.47f, 14f, 3f)
                    verticalLineTo(3.09f)
                    curveTo(14.001f, 3.412f, 14.097f, 3.726f, 14.274f, 3.994f)
                    curveTo(14.452f, 4.263f, 14.704f, 4.473f, 15f, 4.6f)
                    curveTo(15.302f, 4.733f, 15.636f, 4.773f, 15.961f, 4.714f)
                    curveTo(16.285f, 4.655f, 16.584f, 4.501f, 16.82f, 4.27f)
                    lineTo(16.88f, 4.21f)
                    curveTo(17.066f, 4.024f, 17.286f, 3.877f, 17.529f, 3.776f)
                    curveTo(17.772f, 3.675f, 18.032f, 3.623f, 18.295f, 3.623f)
                    curveTo(18.558f, 3.623f, 18.818f, 3.675f, 19.061f, 3.776f)
                    curveTo(19.304f, 3.877f, 19.524f, 4.024f, 19.71f, 4.21f)
                    curveTo(19.896f, 4.396f, 20.044f, 4.616f, 20.144f, 4.859f)
                    curveTo(20.245f, 5.102f, 20.297f, 5.362f, 20.297f, 5.625f)
                    curveTo(20.297f, 5.888f, 20.245f, 6.148f, 20.144f, 6.391f)
                    curveTo(20.044f, 6.634f, 19.896f, 6.854f, 19.71f, 7.04f)
                    lineTo(19.65f, 7.1f)
                    curveTo(19.42f, 7.336f, 19.265f, 7.635f, 19.206f, 7.959f)
                    curveTo(19.147f, 8.284f, 19.187f, 8.618f, 19.32f, 8.92f)
                    verticalLineTo(9f)
                    curveTo(19.447f, 9.296f, 19.657f, 9.548f, 19.926f, 9.726f)
                    curveTo(20.194f, 9.903f, 20.508f, 9.999f, 20.83f, 10f)
                    horizontalLineTo(21f)
                    curveTo(21.53f, 10f, 22.039f, 10.211f, 22.414f, 10.586f)
                    curveTo(22.789f, 10.961f, 23f, 11.47f, 23f, 12f)
                    curveTo(23f, 12.53f, 22.789f, 13.039f, 22.414f, 13.414f)
                    curveTo(22.039f, 13.789f, 21.53f, 14f, 21f, 14f)
                    horizontalLineTo(20.91f)
                    curveTo(20.588f, 14.001f, 20.274f, 14.097f, 20.006f, 14.274f)
                    curveTo(19.737f, 14.452f, 19.527f, 14.704f, 19.4f, 15f)
                    verticalLineTo(15f)
                    close()
                }
            }
        }.build()

        return _Settings!!
    }

@Suppress("ObjectPropertyName")
private var _Settings: ImageVector? = null
