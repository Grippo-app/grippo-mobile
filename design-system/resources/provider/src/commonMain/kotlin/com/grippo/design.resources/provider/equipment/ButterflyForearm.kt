package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _ButterflyForearm: ImageVector? = null

/** Pec deck with upright pads supporting bent elbows and the full forearms. */
public val ButterflyForearm: ImageVector
    get() {
        if (_ButterflyForearm != null) {
            return _ButterflyForearm!!
        }
        _ButterflyForearm = ImageVector.Builder(
            name = "ButterflyForearm",
            defaultWidth = 191.dp,
            defaultHeight = 321.dp,
            viewportWidth = 191f,
            viewportHeight = 321f
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 0f, y = 312.793f)
                verticalLineTo(298.068f)
                lineTo(x = 14.7257f, y = 306.192f)
                verticalLineTo(320.41f)
                lineTo(x = 0f, y = 312.793f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 14.7258f, y = 320.41f)
                verticalLineTo(306.193f)
                lineTo(x = 130.5f, y = 239.165f)
                verticalLineTo(252.875f)
                lineTo(x = 14.7258f, y = 320.41f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 14.7257f, y = 306.141f)
                lineTo(x = 0f, y = 298.016f)
                lineTo(x = 117.298f, y = 230.533f)
                lineTo(x = 130.5f, y = 239.165f)
                lineTo(x = 14.7257f, y = 306.141f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 130.501f, y = 239.165f)
                verticalLineTo(20.3113f)
                lineTo(x = 118.314f, y = 27.4202f)
                verticalLineTo(246.274f)
                lineTo(x = 130.501f, y = 239.165f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 106.635f, y = 236.627f)
                verticalLineTo(33.0061f)
                lineTo(x = 118.314f, y = 26.9127f)
                verticalLineTo(246.783f)
                lineTo(x = 106.635f, y = 236.627f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.3008f, y = 233.201f)
                lineTo(x = 118.316f, y = 249.617f)
                lineTo(x = 33.1366f, y = 298.956f)
                lineTo(x = 8.12158f, y = 282.54f)
                lineTo(x = 93.3008f, y = 233.201f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 240.689f)
                verticalLineTo(249.829f)
                lineTo(x = 93.4319f, y = 228.502f)
                verticalLineTo(219.362f)
                lineTo(x = 125.93f, y = 240.689f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.3008f, y = 222.029f)
                lineTo(x = 118.316f, y = 238.445f)
                lineTo(x = 33.1366f, y = 287.784f)
                lineTo(x = 8.12158f, y = 271.368f)
                lineTo(x = 93.3008f, y = 222.029f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 229.517f)
                verticalLineTo(238.657f)
                lineTo(x = 93.4319f, y = 217.331f)
                verticalLineTo(208.19f)
                lineTo(x = 125.93f, y = 229.517f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.3008f, y = 210.858f)
                lineTo(x = 118.316f, y = 227.274f)
                lineTo(x = 33.1366f, y = 276.613f)
                lineTo(x = 8.12158f, y = 260.197f)
                lineTo(x = 93.3008f, y = 210.858f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 218.347f)
                verticalLineTo(227.487f)
                lineTo(x = 93.4319f, y = 206.16f)
                verticalLineTo(197.02f)
                lineTo(x = 125.93f, y = 218.347f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.3008f, y = 199.687f)
                lineTo(x = 118.316f, y = 216.104f)
                lineTo(x = 33.1366f, y = 265.442f)
                lineTo(x = 8.12158f, y = 249.026f)
                lineTo(x = 93.3008f, y = 199.687f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 207.176f)
                verticalLineTo(216.316f)
                lineTo(x = 93.4319f, y = 194.989f)
                verticalLineTo(185.849f)
                lineTo(x = 125.93f, y = 207.176f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.3008f, y = 188.516f)
                lineTo(x = 118.316f, y = 204.932f)
                lineTo(x = 33.1366f, y = 254.271f)
                lineTo(x = 8.12158f, y = 237.855f)
                lineTo(x = 93.3008f, y = 188.516f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 196.005f)
                verticalLineTo(205.145f)
                lineTo(x = 93.4319f, y = 183.818f)
                verticalLineTo(174.678f)
                lineTo(x = 125.93f, y = 196.005f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 125.93f, y = 196.005f)
                lineTo(x = 33.0059f, y = 249.83f)
                lineTo(x = 0.507812f, y = 228.503f)
                lineTo(x = 93.432f, y = 174.678f)
                lineTo(x = 125.93f, y = 196.005f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 88.1449f, y = 184.844f)
                lineTo(x = 108.16f, y = 197.979f)
                lineTo(x = 37.2771f, y = 239.037f)
                lineTo(x = 17.2617f, y = 225.902f)
                lineTo(x = 88.1449f, y = 184.844f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 115.774f, y = 189.403f)
                verticalLineTo(198.191f)
                lineTo(x = 91.4009f, y = 181.786f)
                verticalLineTo(172.998f)
                lineTo(x = 115.774f, y = 189.403f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 115.775f, y = 189.403f)
                lineTo(x = 37.1466f, y = 234.361f)
                lineTo(x = 12.773f, y = 218.432f)
                lineTo(x = 91.4011f, y = 172.888f)
                lineTo(x = 115.775f, y = 189.403f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N050),
                strokeAlpha = 1.0f,
                strokeLineWidth = 7f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 84.6851f, y = 181.824f)
                lineTo(x = 98.4295f, y = 190.844f)
                lineTo(x = 40.5123f, y = 224.392f)
                lineTo(x = 26.7679f, y = 215.372f)
                lineTo(x = 84.6851f, y = 181.824f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.092f, y = 184.325f)
                verticalLineTo(191.029f)
                lineTo(x = 91.1647f, y = 181.889f)
                verticalLineTo(175.185f)
                lineTo(x = 105.092f, y = 184.325f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.111f, y = 184.325f)
                lineTo(x = 40.1147f, y = 220.885f)
                lineTo(x = 25.8967f, y = 211.745f)
                lineTo(x = 90.1608f, y = 174.45f)
                lineTo(x = 105.111f, y = 184.325f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 52.3018f, y = 202.604f)
                verticalLineTo(65.9717f)
                lineTo(x = 57.8874f, y = 62.8042f)
                verticalLineTo(202.604f)
                curveTo(x1 = 55.8562f, y1 = 205.651f, x2 = 52.8095f, y2 = 204.128f, x3 = 52.3018f, y3 = 202.604f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 54.8406f, y = 204.371f)
                verticalLineTo(64.488f)
                lineTo(x = 57.8873f, y = 62.4569f)
                verticalLineTo(202.097f)
                curveTo(x1 = 57.8873f, y1 = 203.722f, x2 = 55.8561f, y2 = 204.371f, x3 = 54.8406f, y3 = 204.371f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 76.6753f, y = 188.388f)
                verticalLineTo(52.0243f)
                lineTo(x = 82.2609f, y = 48.9979f)
                verticalLineTo(188.388f)
                curveTo(x1 = 80.2298f, y1 = 191.435f, x2 = 77.1831f, y2 = 189.912f, x3 = 76.6753f, y3 = 188.388f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 79.2141f, y = 190.152f)
                verticalLineTo(50.6054f)
                lineTo(x = 82.2608f, y = 48.7468f)
                verticalLineTo(187.878f)
                curveTo(x1 = 82.2608f, y1 = 189.503f, x2 = 80.2297f, y2 = 190.152f, x3 = 79.2141f, y3 = 190.152f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 26.4048f, y = 299.592f)
                verticalLineTo(81.0269f)
                lineTo(x = 14.7258f, y = 87.3389f)
                verticalLineTo(306.194f)
                lineTo(x = 26.4048f, y = 299.592f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 0f, y = 298.576f)
                verticalLineTo(80.2296f)
                lineTo(x = 14.7257f, y = 87.3386f)
                verticalLineTo(306.193f)
                lineTo(x = 0f, y = 298.576f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 0f, y = 80.2298f)
                verticalLineTo(65.5042f)
                lineTo(x = 14.7257f, y = 73.6287f)
                verticalLineTo(87.8466f)
                lineTo(x = 0f, y = 80.2298f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 14.7258f, y = 87.8466f)
                verticalLineTo(73.6287f)
                lineTo(x = 130.5f, y = 6.60144f)
                verticalLineTo(21.8349f)
                lineTo(x = 14.7258f, y = 87.8466f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 14.7257f, y = 73.5768f)
                lineTo(x = 0f, y = 65.4523f)
                lineTo(x = 116.282f, y = 0f)
                lineTo(x = 130.5f, y = 6.60117f)
                lineTo(x = 14.7257f, y = 73.5768f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 60.5f, y = 135f)
                lineTo(x = 69f, y = 109.5f)
                lineTo(x = 72f, y = 110f)
                lineTo(x = 64f, y = 135f)
                verticalLineTo(214f)
                lineTo(x = 72f, y = 218f)
                lineTo(x = 72f, y = 222f)
                lineTo(x = 60.5f, y = 216f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 58.5f, y = 136f)
                lineTo(x = 63.5f, y = 107f)
                horizontalLineTo(69f)
                lineTo(x = 62f, y = 136f)
                verticalLineTo(215f)
                lineTo(x = 70f, y = 219f)
                verticalLineTo(222f)
                lineTo(x = 58.5f, y = 217f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 58.5f, y = 141f)
                lineTo(x = 62f, y = 141f)
                verticalLineTo(156f)
                lineTo(x = 58.5f, y = 158f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 155f, y = 86f)
                lineTo(x = 143f, y = 57f)
                lineTo(x = 140f, y = 57.5f)
                lineTo(x = 152.5f, y = 86f)
                verticalLineTo(168f)
                lineTo(x = 143f, y = 173f)
                verticalLineTo(177f)
                lineTo(x = 155f, y = 170f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 150.5f, y = 86f)
                lineTo(x = 139f, y = 61f)
                lineTo(x = 142.5f, y = 60f)
                lineTo(x = 153.5f, y = 85.5f)
                verticalLineTo(169f)
                lineTo(x = 143f, y = 175f)
                verticalLineTo(172f)
                lineTo(x = 150.5f, y = 167.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 150.5f, y = 93f)
                lineTo(x = 155f, y = 93f)
                verticalLineTo(108f)
                lineTo(x = 150.5f, y = 110f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 49f, y = 158f)
                lineTo(x = 54f, y = 161f)
                verticalLineTo(215f)
                curveTo(x1 = 54f, y1 = 217f, x2 = 53f, y2 = 218f, x3 = 51.5f, y3 = 217f)
                lineTo(x = 47f, y = 214.5f)
                verticalLineTo(162f)
                curveTo(x1 = 47f, y1 = 160f, x2 = 47.5f, y2 = 159f, x3 = 49f, y3 = 158f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 54f, y = 161f)
                lineTo(x = 72f, y = 151f)
                verticalLineTo(206f)
                curveTo(x1 = 72f, y1 = 208f, x2 = 71f, y2 = 209.5f, x3 = 69.5f, y3 = 210.5f)
                lineTo(x = 56.5f, y = 218f)
                curveTo(x1 = 55f, y1 = 219f, x2 = 54f, y2 = 218f, x3 = 54f, y3 = 216f)
                verticalLineTo(161f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 49f, y = 158f)
                lineTo(x = 66f, y = 148.5f)
                curveTo(x1 = 67.5f, y1 = 147.5f, x2 = 69f, y2 = 148f, x3 = 70f, y3 = 149f)
                lineTo(x = 72f, y = 151f)
                lineTo(x = 54f, y = 161f)
                lineTo(x = 49f, y = 158f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 141f, y = 110f)
                lineTo(x = 146f, y = 113f)
                verticalLineTo(167f)
                curveTo(x1 = 146f, y1 = 169f, x2 = 145f, y2 = 170f, x3 = 143.5f, y3 = 169f)
                lineTo(x = 139f, y = 166.5f)
                verticalLineTo(114f)
                curveTo(x1 = 139f, y1 = 112f, x2 = 139.5f, y2 = 111f, x3 = 141f, y3 = 110f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 146f, y = 113f)
                lineTo(x = 164f, y = 103f)
                verticalLineTo(158f)
                curveTo(x1 = 164f, y1 = 160f, x2 = 163f, y2 = 161.5f, x3 = 161.5f, y3 = 162.5f)
                lineTo(x = 148.5f, y = 170f)
                curveTo(x1 = 147f, y1 = 171f, x2 = 146f, y2 = 170f, x3 = 146f, y3 = 168f)
                verticalLineTo(113f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 141f, y = 110f)
                lineTo(x = 158f, y = 100.5f)
                curveTo(x1 = 159.5f, y1 = 99.5f, x2 = 161f, y2 = 100f, x3 = 162f, y3 = 101f)
                lineTo(x = 164f, y = 103f)
                lineTo(x = 146f, y = 113f)
                lineTo(x = 141f, y = 110f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 74.1033f, y = 285f)
                verticalLineTo(45.5004f)
                lineTo(x = 86.1033f, y = 48.5004f)
                verticalLineTo(290f)
                lineTo(x = 74.1033f, y = 285f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 93.6033f, y = 285.5f)
                lineTo(x = 86.1033f, y = 290f)
                lineTo(x = 86f, y = 48.4995f)
                lineTo(x = 93.5f, y = 43.4995f)
                lineTo(x = 93.6033f, y = 285.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 39.758f, y = 100.099f)
                curveTo(x1 = 38.013f, y1 = 89.1229f, x2 = 49.6918f, y2 = 77.6428f, x3 = 67.305f, y3 = 73.0208f)
                curveTo(x1 = 86.5405f, y1 = 67.973f, x2 = 105.48f, y2 = 74.1897f, x3 = 107.386f, y3 = 86.1768f)
                curveTo(x1 = 109.131f, y1 = 97.1529f, x2 = 97.4524f, y2 = 108.633f, x3 = 79.8392f, y3 = 113.255f)
                curveTo(x1 = 60.6037f, y1 = 118.303f, x2 = 41.6637f, y2 = 112.086f, x3 = 39.758f, y3 = 100.099f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 39.758f, y = 97.0495f)
                curveTo(x1 = 38.013f, y1 = 86.0733f, x2 = 49.6918f, y2 = 74.5933f, x3 = 67.305f, y3 = 69.9712f)
                curveTo(x1 = 86.5405f, y1 = 64.9234f, x2 = 105.48f, y2 = 71.1402f, x3 = 107.386f, y3 = 83.1272f)
                curveTo(x1 = 109.131f, y1 = 94.1034f, x2 = 97.4524f, y2 = 105.583f, x3 = 79.8392f, y3 = 110.206f)
                curveTo(x1 = 60.6037f, y1 = 115.253f, x2 = 41.6637f, y2 = 109.037f, x3 = 39.758f, y3 = 97.0495f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N050),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 110.535f, y = 65.3166f)
                curveTo(x1 = 108.818f, y1 = 54.5187f, x2 = 120.307f, y2 = 43.2251f, x3 = 137.634f, y3 = 38.6781f)
                curveTo(x1 = 156.557f, y1 = 33.7122f, x2 = 175.19f, y2 = 39.8280f, x3 = 177.064f, y3 = 51.6204f)
                curveTo(x1 = 178.781f, y1 = 62.4183f, x2 = 167.292f, y2 = 73.7119f, x3 = 149.965f, y3 = 78.2589f)
                curveTo(x1 = 131.042f, y1 = 83.2248f, x2 = 112.409f, y2 = 77.1090f, x3 = 110.535f, y3 = 65.3166f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 110.535f, y = 62.2671f)
                curveTo(x1 = 108.818f, y1 = 51.4692f, x2 = 120.307f, y2 = 40.1756f, x3 = 137.634f, y3 = 35.6286f)
                curveTo(x1 = 156.557f, y1 = 30.6627f, x2 = 175.19f, y2 = 36.7785f, x3 = 177.064f, y3 = 48.5709f)
                curveTo(x1 = 178.781f, y1 = 59.3688f, x2 = 167.292f, y2 = 70.6624f, x3 = 149.965f, y3 = 75.2095f)
                curveTo(x1 = 131.042f, y1 = 80.1753f, x2 = 112.409f, y2 = 74.0595f, x3 = 110.535f, y3 = 62.2671f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 59.871f, y = 85.2556f)
                lineTo(x = 141.734f, y = 44.7546f)
                lineTo(x = 156.527f, y = 48.2773f)
                lineTo(x = 75.454f, y = 90.2974f)
                lineTo(x = 59.871f, y = 85.2556f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 59.4565f, y = 89.7179f)
                lineTo(x = 59.7651f, y = 85.2285f)
                lineTo(x = 75.4532f, y = 90.3164f)
                lineTo(x = 75.1103f, y = 95.3046f)
                lineTo(x = 59.4565f, y = 89.7179f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 75.4532f, y = 90.3163f)
                lineTo(x = 75.1446f, y = 94.8057f)
                lineTo(x = 155.685f, y = 53.2312f)
                lineTo(x = 156.527f, y = 48.2773f)
                lineTo(x = 75.4532f, y = 90.3163f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 129f, y = 52f)
                lineTo(x = 74.8251f, y = 38.772f)
                lineTo(x = 65.6033f, y = 43.4999f)
                lineTo(x = 121.5f, y = 57f)
                lineTo(x = 129f, y = 52f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 66.1033f, y = 57f)
                verticalLineTo(43.5f)
                lineTo(x = 121.5f, y = 57f)
                verticalLineTo(69.5f)
                lineTo(x = 66.1033f, y = 57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 121.5f, y = 57f)
                verticalLineTo(69.5f)
                lineTo(x = 129f, y = 63f)
                verticalLineTo(52f)
                lineTo(x = 121.5f, y = 57f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 111.5f, y = 284.5f)
                lineTo(x = 93.5f, y = 275.5f)
                lineTo(x = 86f, y = 280f)
                lineTo(x = 105.5f, y = 290f)
                lineTo(x = 111.5f, y = 284.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 75.5f, y = 285f)
                lineTo(x = 77f, y = 275.5f)
                lineTo(x = 105f, y = 289.568f)
                verticalLineTo(299.568f)
                lineTo(x = 75.5f, y = 285f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 111.5f, y = 284.5f)
                lineTo(x = 105f, y = 289.5f)
                verticalLineTo(299.5f)
                lineTo(x = 111.5f, y = 294.5f)
                verticalLineTo(284.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 78.5f, y = 229.5f)
                verticalLineTo(156.5f)
                lineTo(x = 99.5f, y = 151f)
                lineTo(x = 106f, y = 152.5f)
                lineTo(x = 99.5f, y = 218.5f)
                lineTo(x = 85f, y = 231.5f)
                lineTo(x = 78.5f, y = 229.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 85f, y = 231.5f)
                verticalLineTo(158.5f)
                lineTo(x = 106f, y = 153f)
                verticalLineTo(222.5f)
                lineTo(x = 85f, y = 231.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 79.4997f, y = 251.499f)
                lineTo(x = 106f, y = 263f)
                lineTo(x = 115.631f, y = 256.999f)
                lineTo(x = 115.631f, y = 251.771f)
                lineTo(x = 86.9284f, y = 241.388f)
                lineTo(x = 79.4997f, y = 244.999f)
                lineTo(x = 79.4997f, y = 251.499f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 79.6316f, y = 244.77f)
                lineTo(x = 106f, y = 257.499f)
                lineTo(x = 115.632f, y = 251.77f)
                lineTo(x = 99.9998f, y = 235.5f)
                lineTo(x = 79.6316f, y = 244.77f)
                close()
            }
        }.build()
        return _ButterflyForearm!!
    }

