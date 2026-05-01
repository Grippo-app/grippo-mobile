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

public val AppIcon.EmptyExerciseExample: ImageVector
    get() {
        if (_EmptyExerciseExample != null) {
            return _EmptyExerciseExample!!
        }
        _EmptyExerciseExample = ImageVector.Builder(
            name = "EmptyExerciseExample",
            defaultWidth = 220.dp,
            defaultHeight = 236.dp,
            viewportWidth = 220f,
            viewportHeight = 236f
        ).apply {
            path(
                fill = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color(0x333366FF),
                        0.58f to Color(0x193366FF)
                    ),
                    center = Offset(110f, 104f),
                    radius = 108f
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
                moveTo(44f, 218f)
                arcToRelative(66f, 14f, 0f, isMoreThanHalf = true, isPositiveArc = false, 132f, 0f)
                arcToRelative(66f, 14f, 0f, isMoreThanHalf = true, isPositiveArc = false, -132f, 0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF3366FF)),
                fillAlpha = 0.7f
            ) {
                moveTo(20f, 88f)
                moveToRelative(-3f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
                arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
            }
            path(
                fill = SolidColor(Color(0xFF7090FF)),
                fillAlpha = 0.76f
            ) {
                moveTo(200f, 66f)
                moveToRelative(-3.2f, 0f)
                arcToRelative(3.2f, 3.2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6.4f, 0f)
                arcToRelative(
                    3.2f,
                    3.2f,
                    0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    -6.4f,
                    0f
                )
            }
            path(
                fill = SolidColor(Color(0xFF7090FF)),
                fillAlpha = 0.55f
            ) {
                moveTo(191f, 144f)
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
                moveTo(30f, 168f)
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
                moveTo(180f, 80f)
                curveTo(188f, 90f, 192f, 103f, 193f, 119f)
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
                moveTo(62f, 63f)
                moveToRelative(-20f, 0f)
                arcToRelative(20f, 20f, 0f, isMoreThanHalf = true, isPositiveArc = true, 40f, 0f)
                arcToRelative(20f, 20f, 0f, isMoreThanHalf = true, isPositiveArc = true, -40f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(42f, 44f),
                    end = Offset(166f, 186f)
                ),
                strokeLineWidth = 2f
            ) {
                moveTo(62f, 63f)
                moveToRelative(-19f, 0f)
                arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, 38f, 0f)
                arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, -38f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(42f, 44f),
                    end = Offset(166f, 186f)
                ),
                strokeLineWidth = 3f
            ) {
                moveTo(59f, 60f)
                moveToRelative(-6.5f, 0f)
                arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 13f, 0f)
                arcToRelative(6.5f, 6.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -13f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(42f, 44f),
                    end = Offset(166f, 186f)
                ),
                strokeLineWidth = 3.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(64f, 65f)
                lineTo(71f, 72f)
            }
            path(fill = SolidColor(Color(0xFF5B667A))) {
                moveTo(97f, 54f)
                lineTo(161f, 54f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 165f, 58f)
                lineTo(165f, 58f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 62f)
                lineTo(97f, 62f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 93f, 58f)
                lineTo(93f, 58f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 97f, 54f)
                close()
            }
            path(fill = SolidColor(Color(0xFF3F4857))) {
                moveTo(97f, 70f)
                lineTo(139f, 70f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 143f, 74f)
                lineTo(143f, 74f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 139f, 78f)
                lineTo(97f, 78f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 93f, 74f)
                lineTo(93f, 74f)
                arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 97f, 70f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 88f),
                    end = Offset(174f, 190f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 94f)
                lineTo(161f, 94f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 105f)
                lineTo(172f, 109f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 120f)
                lineTo(59f, 120f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 109f)
                lineTo(48f, 105f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 94f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(63f, 107f)
                moveToRelative(-7f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
            }
            path(fill = SolidColor(Color(0xFF333B47))) {
                moveTo(80.5f, 103.5f)
                lineTo(127.5f, 103.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 131f, 107f)
                lineTo(131f, 107f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 127.5f, 110.5f)
                lineTo(80.5f, 110.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 77f, 107f)
                lineTo(77f, 107f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 80.5f, 103.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF2A303B))) {
                moveTo(138.5f, 103.5f)
                lineTo(149.5f, 103.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 153f, 107f)
                lineTo(153f, 107f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 149.5f, 110.5f)
                lineTo(138.5f, 110.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 135f, 107f)
                lineTo(135f, 107f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 138.5f, 103.5f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 88f),
                    end = Offset(174f, 190f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 130f)
                lineTo(161f, 130f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 141f)
                lineTo(172f, 145f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 156f)
                lineTo(59f, 156f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 145f)
                lineTo(48f, 141f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 130f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(63f, 143f)
                moveToRelative(-7f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
            }
            path(fill = SolidColor(Color(0xFF333B47))) {
                moveTo(80.5f, 139.5f)
                lineTo(119.5f, 139.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 123f, 143f)
                lineTo(123f, 143f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 119.5f, 146.5f)
                lineTo(80.5f, 146.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 77f, 143f)
                lineTo(77f, 143f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 80.5f, 139.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF2A303B))) {
                moveTo(130.5f, 139.5f)
                lineTo(149.5f, 139.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 153f, 143f)
                lineTo(153f, 143f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 149.5f, 146.5f)
                lineTo(130.5f, 146.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 127f, 143f)
                lineTo(127f, 143f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 130.5f, 139.5f)
                close()
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF222833),
                        1f to Color(0xFF171B21)
                    ),
                    start = Offset(46f, 88f),
                    end = Offset(174f, 190f)
                ),
                stroke = SolidColor(Color(0xFF333B47)),
                strokeLineWidth = 1.5f
            ) {
                moveTo(59f, 166f)
                lineTo(161f, 166f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 172f, 177f)
                lineTo(172f, 181f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 161f, 192f)
                lineTo(59f, 192f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 48f, 181f)
                lineTo(48f, 177f)
                arcTo(11f, 11f, 0f, isMoreThanHalf = false, isPositiveArc = true, 59f, 166f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF4A5566)),
                strokeLineWidth = 3f
            ) {
                moveTo(63f, 179f)
                moveToRelative(-7f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 0f)
                arcToRelative(7f, 7f, 0f, isMoreThanHalf = true, isPositiveArc = true, -14f, 0f)
            }
            path(fill = SolidColor(Color(0xFF333B47))) {
                moveTo(80.5f, 175.5f)
                lineTo(112.5f, 175.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 116f, 179f)
                lineTo(116f, 179f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 112.5f, 182.5f)
                lineTo(80.5f, 182.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 77f, 179f)
                lineTo(77f, 179f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 80.5f, 175.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF2A303B))) {
                moveTo(123.5f, 175.5f)
                lineTo(149.5f, 175.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 153f, 179f)
                lineTo(153f, 179f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 149.5f, 182.5f)
                lineTo(123.5f, 182.5f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 120f, 179f)
                lineTo(120f, 179f)
                arcTo(3.5f, 3.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 123.5f, 175.5f)
                close()
            }
            path(fill = SolidColor(Color(0xFF1C2128))) {
                moveTo(153f, 158f)
                moveToRelative(-19f, 0f)
                arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, 38f, 0f)
                arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, -38f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(42f, 44f),
                    end = Offset(166f, 186f)
                ),
                strokeLineWidth = 2.5f
            ) {
                moveTo(153f, 158f)
                moveToRelative(-18f, 0f)
                arcToRelative(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, 36f, 0f)
                arcToRelative(18f, 18f, 0f, isMoreThanHalf = true, isPositiveArc = true, -36f, 0f)
            }
            path(
                fill = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF99B2FF),
                        0.48f to Color(0xFF7090FF),
                        1f to Color(0xFF3366FF)
                    ),
                    start = Offset(42f, 44f),
                    end = Offset(166f, 186f)
                ),
                strokeLineWidth = 4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(166f, 171f)
                lineTo(174f, 179f)
            }
            path(
                stroke = SolidColor(Color(0xFF7090FF)),
                strokeLineWidth = 3.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(146f, 158f)
                lineTo(160f, 158f)
            }
        }.build()

        return _EmptyExerciseExample!!
    }

@Suppress("ObjectPropertyName")
private var _EmptyExerciseExample: ImageVector? = null
