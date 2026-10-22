package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _Rope: ImageVector? = null

public val Rope: ImageVector
    get() {
        if (_Rope != null) {
            return _Rope!!
        }
        _Rope = ImageVector.Builder(
            name = "Rope",
            defaultWidth = 126.dp,
            defaultHeight = 237.dp,
            viewportWidth = 126f,
            viewportHeight = 237f
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N550),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2.97534f, 201.955f)
                curveTo(8.6068f, 206.123f, 24.331f, 213.452f, 42.1762f, 209.415f)
                curveTo(38.3975f, 206.221f, 25.2672f, 200.258f, 2.9753f, 201.955f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(1.00068f, 220.093f)
                lineTo(2.75594f, 201.955f)
                curveTo(17.032f, 211.96f, 34.9358f, 211.097f, 42.1031f, 209.415f)
                curveTo(41.9812f, 210.926f, 41.518f, 215.573f, 40.6404f, 222.067f)
                curveTo(39.7627f, 228.562f, 35.8865f, 232.331f, 34.0581f, 233.403f)
                curveTo(31.8153f, 234.842f, 24.6821f, 237.133f, 14.092f, 234.793f)
                curveTo(3.5019f, 232.453f, 0.9519f, 224.017f, 1.0007f, 220.093f)
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
                moveTo(21.9907f, 74.9176f)
                curveTo(25.0331f, 47.126f, 41.2985f, 32.8157f, 49.0509f, 29.1345f)
                curveTo(53.2928f, 33.3033f, 59.2315f, 42.7671f, 49.0509f, 47.2722f)
                curveTo(38.8704f, 51.7774f, 35.4964f, 90.7393f, 35.082f, 109.657f)
                verticalLineTo(150.028f)
                curveTo(35.082f, 153.363f, 32.8879f, 189.838f, 31.7909f, 207.659f)
                curveTo(19.4455f, 211.93f, 15.3841f, 205.88f, 14.8965f, 202.32f)
                curveTo(19.0652f, 185.718f, 18.1876f, 109.657f, 21.9907f, 74.9176f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 18.2486f, y = 193.5492f)
                curveTo(x1 = 22.846f, y1 = 192.4698f, x2 = 29.8641f, y2 = 190.4945f, x3 = 32.1805f, y3 = 188.6637f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 18.8335f, y = 183.1635f)
                curveTo(x1 = 23.431f, y1 = 182.0847f, x2 = 30.4491f, y2 = 180.1094f, x3 = 32.7655f, y3 = 178.278f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 19.4187f, y = 171.9005f)
                curveTo(x1 = 24.0162f, y1 = 170.8217f, x2 = 31.0343f, y2 = 168.8464f, x3 = 33.3507f, y3 = 167.015f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 20.0037f, y = 162.3935f)
                curveTo(x1 = 24.6011f, y1 = 161.3146f, x2 = 31.6192f, y2 = 159.3387f, x3 = 33.9356f, y3 = 157.5078f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 20.4426f, y = 152.0082f)
                curveTo(x1 = 25.0401f, y1 = 150.9288f, x2 = 32.0582f, y2 = 148.9535f, x3 = 34.3746f, y3 = 147.1227f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 20.4426f, y = 140.7452f)
                curveTo(x1 = 25.0401f, y1 = 139.6658f, x2 = 32.0582f, y2 = 137.6905f, x3 = 34.3746f, y3 = 135.8597f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 20.2962f, y = 130.6522f)
                curveTo(x1 = 24.8936f, y1 = 129.5728f, x2 = 31.9117f, y2 = 127.5975f, x3 = 34.2281f, y3 = 125.7667f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 20.9481f, y = 120.3529f)
                curveTo(x1 = 25.3168f, y1 = 119.3142f, x2 = 31.9855f, y2 = 117.4119f, x3 = 34.1866f, y3 = 115.6487f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 21.2553f, y = 108.9618f)
                curveTo(x1 = 25.6656f, y1 = 107.9765f, x2 = 32.3978f, y2 = 106.1718f, x3 = 34.6199f, y3 = 104.4985f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 22.0894f, y = 97.2513f)
                curveTo(x1 = 26.3748f, y1 = 96.2388f, x2 = 32.9165f, y2 = 94.3852f, x3 = 35.0758f, y3 = 92.6672f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 22.7035f, y = 87.1496f)
                curveTo(x1 = 27.0722f, y1 = 86.1106f, x2 = 33.7409f, y2 = 84.2082f, x3 = 35.942f, y3 = 82.4449f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 23.8141f, y = 76.042f)
                curveTo(x1 = 28.4324f, y1 = 75.0296f, x2 = 35.4823f, y2 = 73.176f, x3 = 37.8092f, y3 = 71.458f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 25.3635f, y = 66.3345f)
                curveTo(x1 = 30.2314f, y1 = 65.1623f, x2 = 37.6622f, y2 = 63.016f, x3 = 40.115f, y3 = 61.0267f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 28.6537f, y = 56.57f)
                curveTo(x1 = 33.7297f, y1 = 55.5042f, x2 = 41.4781f, y2 = 53.5531f, x3 = 44.0356f, y3 = 51.7447f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 33.7228f, y = 46.0688f)
                curveTo(x1 = 40.3382f, y1 = 44.6568f, x2 = 50.4366f, y2 = 42.0716f, x3 = 53.7697f, y3 = 39.6754f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N550),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(123.356f, 201.955f)
                curveTo(117.725f, 206.123f, 102.001f, 213.452f, 84.1554f, 209.415f)
                curveTo(87.934f, 206.221f, 101.064f, 200.258f, 123.356f, 201.955f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N450),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(125.331f, 220.093f)
                lineTo(123.576f, 201.955f)
                curveTo(109.3f, 211.96f, 91.396f, 211.097f, 84.2287f, 209.415f)
                curveTo(84.3506f, 210.926f, 84.8138f, 215.573f, 85.6914f, 222.067f)
                curveTo(86.569f, 228.562f, 90.4452f, 232.331f, 92.2736f, 233.403f)
                curveTo(94.5165f, 234.842f, 101.65f, 237.133f, 112.24f, 234.793f)
                curveTo(122.83f, 232.453f, 125.38f, 224.017f, 125.331f, 220.093f)
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
                moveTo(104.341f, 74.9176f)
                curveTo(101.299f, 47.126f, 85.0332f, 32.8157f, 77.2808f, 29.1345f)
                curveTo(73.039f, 33.3033f, 67.1003f, 42.7671f, 77.2808f, 47.2722f)
                curveTo(87.4614f, 51.7774f, 90.8354f, 90.7393f, 91.2498f, 109.657f)
                verticalLineTo(150.028f)
                curveTo(91.2498f, 153.363f, 93.4439f, 189.838f, 94.5409f, 207.659f)
                curveTo(106.886f, 211.93f, 110.948f, 205.88f, 111.435f, 202.32f)
                curveTo(107.267f, 185.718f, 108.144f, 109.657f, 104.341f, 74.9176f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 108.0834f, y = 193.5492f)
                curveTo(x1 = 103.4861f, y1 = 192.4698f, x2 = 96.4678f, y2 = 190.4945f, x3 = 94.1513f, y3 = 188.6637f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 107.4984f, y = 183.1635f)
                curveTo(x1 = 102.9011f, y1 = 182.0847f, x2 = 95.8828f, y2 = 180.1094f, x3 = 93.5663f, y3 = 178.278f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 106.9131f, y = 171.9005f)
                curveTo(x1 = 102.3153f, y1 = 170.8217f, x2 = 95.2974f, y2 = 168.8464f, x3 = 92.9811f, y3 = 167.015f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 106.3281f, y = 162.3935f)
                curveTo(x1 = 101.7303f, y1 = 161.3146f, x2 = 94.7125f, y2 = 159.3387f, x3 = 92.3962f, y3 = 157.5078f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.8891f, y = 152.0082f)
                curveTo(x1 = 101.2913f, y1 = 150.9288f, x2 = 94.2735f, y2 = 148.9535f, x3 = 91.9572f, y3 = 147.1227f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.8891f, y = 140.7452f)
                curveTo(x1 = 101.2913f, y1 = 139.6658f, x2 = 94.2735f, y2 = 137.6905f, x3 = 91.9572f, y3 = 135.8597f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 106.0354f, y = 130.6522f)
                curveTo(x1 = 101.4381f, y1 = 129.5728f, x2 = 94.4199f, y2 = 127.5975f, x3 = 92.1034f, y3 = 125.7667f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.3838f, y = 120.3529f)
                curveTo(x1 = 101.015f, y1 = 119.3142f, x2 = 94.3463f, y2 = 117.4119f, x3 = 92.1451f, y3 = 115.6487f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 105.0763f, y = 108.9618f)
                curveTo(x1 = 100.6659f, y1 = 107.9765f, x2 = 93.9339f, y2 = 106.1718f, x3 = 91.7118f, y3 = 104.4985f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 104.2427f, y = 97.2513f)
                curveTo(x1 = 99.9572f, y1 = 96.2388f, x2 = 93.4153f, y2 = 94.3852f, x3 = 91.256f, y3 = 92.6672f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 103.6287f, y = 87.1496f)
                curveTo(x1 = 99.2599f, y1 = 86.1106f, x2 = 92.591f, y2 = 84.2082f, x3 = 90.3898f, y3 = 82.4449f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 102.518f, y = 76.042f)
                curveTo(x1 = 97.8994f, y1 = 75.0296f, x2 = 90.8496f, y2 = 73.176f, x3 = 88.5226f, y3 = 71.458f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 100.9684f, y = 66.3345f)
                curveTo(x1 = 96.1004f, y1 = 65.1623f, x2 = 88.6695f, y2 = 63.016f, x3 = 86.2168f, y3 = 61.0267f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 97.6781f, y = 56.57f)
                curveTo(x1 = 92.6021f, y1 = 55.5042f, x2 = 84.8537f, y2 = 53.5531f, x3 = 82.2962f, y3 = 51.7447f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(x = 92.609f, y = 46.0688f)
                curveTo(x1 = 85.9936f, y1 = 44.6568f, x2 = 75.8952f, y2 = 42.0716f, x3 = 72.5621f, y3 = 39.6754f)
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
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(75.2336f, 46.3947f)
                curveTo(67.2179f, 51.3094f, 55.3162f, 48.7351f, 50.3674f, 46.8335f)
                curveTo(60.3139f, 37.8378f, 48.9047f, 31.3287f, 48.6121f, 29.1346f)
                curveTo(48.3781f, 27.3794f, 54.1704f, 24.9903f, 57.0959f, 24.0151f)
                verticalLineTo(6.31623f)
                curveTo(57.1203f, 4.049f, 57.9004f, -0.383f, 60.8258f, 0.0265f)
                curveTo(63.7512f, 0.4361f, 65.1164f, 3.2201f, 65.4334f, 4.561f)
                lineTo(67.1886f, 24.7465f)
                curveTo(65.1408f, 24.9659f, 62.347f, 25.2731f, 67.5543f, 24.7465f)
                curveTo(72.7616f, 24.2199f, 76.1112f, 27.4525f, 77.1351f, 29.1346f)
                curveTo(69.3534f, 36.4482f, 72.6251f, 43.6887f, 75.2336f, 46.3947f)
                close()

                moveTo(60.8259f, 3.4639f)
                curveTo(61.8986f, 6.1699f, 63.4003f, 12.5913f, 60.8259f, 16.6284f)
                curveTo(57.608f, 21.6747f, 58.6319f, 0.6116f, 60.8259f, 3.4639f)
                close()
            }

        }.build()
        return _Rope!!
    }

