package com.grippo.design.resources.provider.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.EmptyExercise: ImageVector
    get() {
        if (_EmptyExercise != null) {
            return _EmptyExercise!!
        }
        _EmptyExercise = ImageVector.Builder(
            name = "EmptyExercise",
            defaultWidth = 220.dp,
            defaultHeight = 236.dp,
            viewportWidth = 220f,
            viewportHeight = 236f
        ).apply {
            path(
                fill = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0x383366FF),
                        0.58f to Color(0x193366FF)
                    ),
                    center = Offset(110f, 105f),
                    radius = 105f
                )
            ) {
                moveTo(28f, 106f)
                arcToRelative(82f, 78f, 0f, isMoreThanHalf = true, isPositiveArc = false, 164f, 0f)
                arcToRelative(82f, 78f, 0f, isMoreThanHalf = true, isPositiveArc = false, -164f, 0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF0D0F12)),
                fillAlpha = 0.48f
            ) {
                moveTo(46f, 218f)
                arcToRelative(66f, 14f, 0f, isMoreThanHalf = true, isPositiveArc = false, 132f, 0f)
                arcToRelative(66f, 14f, 0f, isMoreThanHalf = true, isPositiveArc = false, -132f, 0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF3366FF)),
                fillAlpha = 0.72f
            ) {
                moveTo(19f, 87f)
                moveToRelative(-3f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF7090FF)),
                fillAlpha = 0.72f
            ) {
                moveTo(198f, 67f)
                moveToRelative(-3f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF7090FF)),
                fillAlpha = 0.55f
            ) {
                moveTo(191f, 146f)
                moveToRelative(-2.4f, 0f)
                arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 4.8f, 0f)
                arcToRelative(
                    2.4f,
                    2.4f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -4.8f,
                    0f
                )
            }
            path(
                fillAlpha = 0.75f,
                stroke = SolidColor(Color(0xFF3366FF)),
                strokeAlpha = 0.75f,
                strokeLineWidth = 2f
            ) {
                moveTo(28f, 167f)
                moveToRelative(-5f, 0f)
                arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 0f)
                arcToRelative(5f, 5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -10f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF3366FF)),
                strokeAlpha = 0.32f,
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(182f, 81f)
                curveTo(189f, 91f, 193f, 105f, 194f, 119f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        0.52f to Color(0xFF171B21),
                        1f to Color(0xFF12151A)
                    ),
                    start = Offset(32f, 20f),
                    end = Offset(182f, 212f)
                )
            ) {
                moveTo(60f, 26f)
                lineTo(160f, 26f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = false, isPositiveArc = true, 188f, 54f)
                lineTo(188f, 176f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = false, isPositiveArc = true, 160f, 204f)
                lineTo(60f, 204f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = false, isPositiveArc = true, 32f, 176f)
                lineTo(32f, 54f)
                arcTo(28f, 28f, 0f, isMoreThanHalf = false, isPositiveArc = true, 60f, 26f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF4A5566),
                        0.5f to Color(0xFF333B47),
                        1f to Color(0xFF2A303B)
                    ),
                    start = Offset(32f, 20f),
                    end = Offset(182f, 212f)
                ),
                strokeLineWidth = 2f
            ) {
                moveTo(60f, 27f)
                lineTo(160f, 27f)
                arcTo(27f, 27f, 0f, isMoreThanHalf = false, isPositiveArc = true, 187f, 54f)
                lineTo(187f, 176f)
                arcTo(27f, 27f, 0f, isMoreThanHalf = false, isPositiveArc = true, 160f, 203f)
                lineTo(60f, 203f)
                arcTo(27f, 27f, 0f, isMoreThanHalf = false, isPositiveArc = true, 33f, 176f)
                lineTo(33f, 54f)
                arcTo(27f, 27f, 0f, isMoreThanHalf = false, isPositiveArc = true, 60f, 27f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF1C2128)),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(92f, 10f)
                lineTo(128f, 10f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, 142f, 24f)
                lineTo(142f, 24f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, 128f, 38f)
                lineTo(92f, 38f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, 78f, 24f)
                lineTo(78f, 24f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, 92f, 10f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF7D8898),
                        1f to Color(0xFF4A5566)
                    ),
                    start = Offset(93f, 54f),
                    end = Offset(159f, 54f)
                )
            ) {
                moveTo(99f, 20f)
                lineTo(121f, 20f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 125f, 24f)
                lineTo(125f, 24f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 121f, 28f)
                lineTo(99f, 28f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 95f, 24f)
                lineTo(95f, 24f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 99f, 20f)
                close()
            }
            path(fill = SolidColor(Color(0xFF171B21))) {
                moveTo(65f, 63f)
                moveToRelative(-22f, 0f)
                arcToRelative(22f, 22f, 0f, isMoreThanHalf = true, isPositiveArc = true, 44f, 0f)
                arcToRelative(22f, 22f, 0f, isMoreThanHalf = true, isPositiveArc = true, -44f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 2f
            ) {
                moveTo(65f, 63f)
                moveToRelative(-21f, 0f)
                arcToRelative(21f, 21f, 0f, isMoreThanHalf = true, isPositiveArc = true, 42f, 0f)
                arcToRelative(21f, 21f, 0f, isMoreThanHalf = true, isPositiveArc = true, -42f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                )
            ) {
                moveTo(58f, 59f)
                lineTo(72f, 59f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 76f, 63f)
                lineTo(76f, 63f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 72f, 67f)
                lineTo(58f, 67f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 54f, 63f)
                lineTo(54f, 63f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 58f, 59f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                )
            ) {
                moveTo(59.5f, 53f)
                lineTo(59.5f, 53f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 62f, 55.5f)
                lineTo(62f, 70.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59.5f, 73f)
                lineTo(59.5f, 73f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 57f, 70.5f)
                lineTo(57f, 55.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59.5f, 53f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                )
            ) {
                moveTo(71.5f, 53f)
                lineTo(71.5f, 53f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 74f, 55.5f)
                lineTo(74f, 70.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 71.5f, 73f)
                lineTo(71.5f, 73f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 69f, 70.5f)
                lineTo(69f, 55.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 71.5f, 53f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                )
            ) {
                moveTo(51f, 57f)
                lineTo(51f, 57f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 53f, 59f)
                lineTo(53f, 67f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 51f, 69f)
                lineTo(51f, 69f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 49f, 67f)
                lineTo(49f, 59f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 51f, 57f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                )
            ) {
                moveTo(79f, 57f)
                lineTo(79f, 57f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 81f, 59f)
                lineTo(81f, 67f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 79f, 69f)
                lineTo(79f, 69f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 77f, 67f)
                lineTo(77f, 59f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 79f, 57f)
                close()
            }
            path(fill = SolidColor(Color(0xFF5B667A))) {
                moveTo(99f, 52f)
                lineTo(163f, 52f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 167f, 56f)
                lineTo(167f, 56f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 163f, 60f)
                lineTo(99f, 60f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 95f, 56f)
                lineTo(95f, 56f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 99f, 52f)
                close()
            }
            path(fill = SolidColor(Color(0xFF3F4857))) {
                moveTo(99f, 68f)
                lineTo(143f, 68f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 147f, 72f)
                lineTo(147f, 72f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 143f, 76f)
                lineTo(99f, 76f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 95f, 72f)
                lineTo(95f, 72f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 99f, 68f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 84f),
                    end = Offset(176f, 191f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 92f)
                lineTo(161f, 92f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 103f)
                lineTo(172f, 109f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 120f)
                lineTo(59f, 120f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 109f)
                lineTo(48f, 103f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 92f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(65f, 106f)
                moveToRelative(-8f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, -16f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(152f, 100f)
                lineTo(152f, 112f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(146f, 106f)
                lineTo(158f, 106f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 84f),
                    end = Offset(176f, 191f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 128f)
                lineTo(161f, 128f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 139f)
                lineTo(172f, 145f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 156f)
                lineTo(59f, 156f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 145f)
                lineTo(48f, 139f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 128f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(65f, 142f)
                moveToRelative(-8f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, -16f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(152f, 136f)
                lineTo(152f, 148f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(146f, 142f)
                lineTo(158f, 142f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 84f),
                    end = Offset(176f, 191f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 164f)
                lineTo(161f, 164f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 175f)
                lineTo(172f, 181f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 192f)
                lineTo(59f, 192f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 181f)
                lineTo(48f, 175f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 164f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(65f, 178f)
                moveToRelative(-8f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 0f)
                arcToRelative(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, -16f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(152f, 172f)
                lineTo(152f, 184f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(53f, 47f),
                    end = Offset(164f, 179f)
                ),
                strokeLineWidth = 3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(146f, 178f)
                lineTo(158f, 178f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF7D8898),
                        0.5f to Color(0xFFF4F7FA),
                        1f to Color(0xFF5B667A)
                    ),
                    start = Offset(108f, 200f),
                    end = Offset(176f, 200f)
                )
            ) {
                moveTo(119.5f, 198f)
                lineTo(162.5f, 198f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 166f, 201.5f)
                lineTo(166f, 201.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 162.5f, 205f)
                lineTo(119.5f, 205f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 116f, 201.5f)
                lineTo(116f, 201.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 119.5f, 198f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF4A5566),
                        1f to Color(0xFF222833)
                    ),
                    start = Offset(104f, 186f),
                    end = Offset(113f, 217f)
                )
            ) {
                moveTo(108.5f, 186f)
                lineTo(108.5f, 186f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 113f, 190.5f)
                lineTo(113f, 212.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 108.5f, 217f)
                lineTo(108.5f, 217f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 104f, 212.5f)
                lineTo(104f, 190.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 108.5f, 186f)
                close()
            }
            path(fill = SolidColor(Color(0xFF333B47))) {
                moveTo(98f, 181f)
                lineTo(98f, 181f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 102f, 185f)
                lineTo(102f, 218f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 98f, 222f)
                lineTo(98f, 222f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 94f, 218f)
                lineTo(94f, 185f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 98f, 181f)
                close()
            }
            path(fill = SolidColor(Color(0xFF4A5566))) {
                moveTo(90f, 184f)
                lineTo(90f, 184f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 93f, 187f)
                lineTo(93f, 216f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 90f, 219f)
                lineTo(90f, 219f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 87f, 216f)
                lineTo(87f, 187f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 90f, 184f)
                close()
            }
            path(fill = SolidColor(Color(0xFF7090FF))) {
                moveTo(90f, 201f)
                moveToRelative(-4f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF4A5566),
                        1f to Color(0xFF222833)
                    ),
                    start = Offset(169f, 182f),
                    end = Offset(181f, 221f)
                )
            ) {
                moveTo(175f, 182f)
                lineTo(175f, 182f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 181f, 188f)
                lineTo(181f, 215f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 175f, 221f)
                lineTo(175f, 221f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 169f, 215f)
                lineTo(169f, 188f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, 175f, 182f)
                close()
            }
            path(fill = SolidColor(Color(0xFF333B47))) {
                moveTo(186f, 177f)
                lineTo(186f, 177f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 190f, 181f)
                lineTo(190f, 222f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 186f, 226f)
                lineTo(186f, 226f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 182f, 222f)
                lineTo(182f, 181f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 186f, 177f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 5f
            ) {
                moveTo(186f, 201f)
                moveToRelative(-13f, 0f)
                arcToRelative(13f, 13f, 0f, isMoreThanHalf = true, isPositiveArc = true, 26f, 0f)
                arcToRelative(13f, 13f, 0f, isMoreThanHalf = true, isPositiveArc = true, -26f, 0f)
            }
            path(fill = SolidColor(Color(0xFF12151A))) {
                moveTo(186f, 201f)
                moveToRelative(-6f, 0f)
                arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, 12f, 0f)
                arcToRelative(6f, 6f, 0f, isMoreThanHalf = true, isPositiveArc = true, -12f, 0f)
            }
            path(
                stroke = SolidColor(Color(0xFF3366FF)),
                strokeLineWidth = 2f
            ) {
                moveTo(186f, 201f)
                moveToRelative(-4f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
                arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
            }
        }.build()

        return _EmptyExercise!!
    }

@Suppress("ObjectPropertyName")
private var _EmptyExercise: ImageVector? = null
