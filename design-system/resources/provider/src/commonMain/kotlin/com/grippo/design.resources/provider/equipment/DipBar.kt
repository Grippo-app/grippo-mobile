package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _DipBar: ImageVector? = null

public val DipBar: ImageVector
    get() {
        if (_DipBar != null) {
            return _DipBar!!
        }
        _DipBar = ImageVector.Builder(
            name = "DipBar",
            defaultWidth = 296.dp,
            defaultHeight = 283.dp,
            viewportWidth = 296f,
            viewportHeight = 283f
        ).apply {
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
                moveTo(200.2459f, 166.925f)
                lineTo(116.0083f, 190.697f)
                curveTo(123.8636f, 188.63f, 122.3821f, 199.827f, 120.6595f, 205.684f)
                lineTo(201.7963f, 181.912f)
                curveTo(206.3441f, 176.124f, 202.6577f, 169.509f, 200.2459f, 166.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(116.00847f, 190.698f)
                curveTo(114.4581f, 195.349f, 113.9413f, 203.617f, 120.1429f, 205.684f)
                curveTo(122.21f, 200.517f, 124.2772f, 190.285f, 116.0085f, 190.698f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(126.3441f, 188.114f)
                lineTo(116.0083f, 190.698f)
                curveTo(123.0367f, 189.044f, 122.2098f, 202.584f, 120.6595f, 205.685f)
                lineTo(127.8944f, 203.617f)
                curveTo(134.096f, 196.589f, 128.7558f, 189.836f, 126.3441f, 188.114f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(200.2459f, 167.442f)
                lineTo(190.9436f, 170.026f)
                curveTo(197.972f, 168.372f, 196.1116f, 181.912f, 195.0779f, 183.462f)
                lineTo(201.7963f, 181.395f)
                curveTo(206.4474f, 174.16f, 202.6576f, 169.164f, 200.2459f, 167.442f)
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
                moveTo(290.2459f, 203.925f)
                lineTo(206.0083f, 227.697f)
                curveTo(213.8636f, 225.63f, 212.3821f, 236.827f, 210.6595f, 242.684f)
                lineTo(291.7963f, 218.912f)
                curveTo(296.3441f, 213.124f, 292.6577f, 206.509f, 290.2459f, 203.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(206.00847f, 227.698f)
                curveTo(204.4581f, 232.349f, 203.9413f, 240.617f, 210.1429f, 242.684f)
                curveTo(212.21f, 237.517f, 214.2772f, 227.285f, 206.0085f, 227.698f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(216.3441f, 225.114f)
                lineTo(206.0083f, 227.698f)
                curveTo(213.0367f, 226.044f, 212.2098f, 239.584f, 210.6595f, 242.685f)
                lineTo(217.8944f, 240.617f)
                curveTo(224.096f, 233.589f, 218.7558f, 226.836f, 216.3441f, 225.114f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(290.2459f, 204.442f)
                lineTo(280.9436f, 207.026f)
                curveTo(287.972f, 205.372f, 286.1116f, 218.912f, 285.0779f, 220.462f)
                lineTo(291.7963f, 218.395f)
                curveTo(296.4474f, 211.16f, 292.6576f, 206.164f, 290.2459f, 204.442f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 13f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(161.5f, 180f)
                lineTo(161.5f, 24f)
                curveTo(161.5f, 14.666667f, 166.166667f, 11.918519f, 175.5f, 15.755556f)
                lineTo(237.5f, 41.244444f)
                curveTo(246.833333f, 45.081481f, 251.5f, 51.666667f, 251.5f, 61f)
                lineTo(251.5f, 217f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeAlpha = 1.0f,
                strokeLineWidth = 11.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(161.5f, 180f)
                lineTo(161.5f, 24f)
                curveTo(161.5f, 14.666667f, 166.166667f, 11.918519f, 175.5f, 15.755556f)
                lineTo(237.5f, 41.244444f)
                curveTo(246.833333f, 45.081481f, 251.5f, 51.666667f, 251.5f, 61f)
                lineTo(251.5f, 217f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralLight.N200),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(160.25f, 179.4f)
                lineTo(160.25f, 23.4f)
                curveTo(160.25f, 14.066667f, 164.916667f, 11.318519f, 174.25f, 15.155556f)
                lineTo(236.25f, 40.644444f)
                curveTo(245.583333f, 44.481481f, 250.25f, 51.066667f, 250.25f, 60.4f)
                lineTo(250.25f, 216.4f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(192.971514f, 15.910432f)
                lineTo(224.971514f, 29.065988f)
                curveTo(224.711174f, 33.644197f, 223.063498f, 37.652057f, 220.028486f, 41.089568f)
                lineTo(188.028486f, 27.934012f)
                curveTo(188.288826f, 23.355803f, 189.936502f, 19.347943f, 192.971514f, 15.910432f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(194.073675f, 17.17445f)
                lineTo(223.299003f, 29.189306f)
                curveTo(222.766334f, 33.114954f, 221.308775f, 36.660369f, 218.926325f, 39.82555f)
                lineTo(189.700997f, 27.810694f)
                curveTo(190.233666f, 23.885046f, 191.691225f, 20.339631f, 194.073675f, 17.17445f)
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
                moveTo(86.2459f, 202.925f)
                lineTo(2.0083f, 226.697f)
                curveTo(9.8636f, 224.63f, 8.3821f, 235.827f, 6.6595f, 241.684f)
                lineTo(87.7963f, 217.912f)
                curveTo(92.3441f, 212.124f, 88.6577f, 205.509f, 86.2459f, 202.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(2.00847f, 226.698f)
                curveTo(0.4581f, 231.349f, -0.0587f, 239.617f, 6.1429f, 241.684f)
                curveTo(8.21f, 236.517f, 10.2772f, 226.285f, 2.0085f, 226.698f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12.3441f, 224.114f)
                lineTo(2.0083f, 226.698f)
                curveTo(9.0367f, 225.044f, 8.2098f, 238.584f, 6.6595f, 241.685f)
                lineTo(13.8944f, 239.617f)
                curveTo(20.096f, 232.589f, 14.7558f, 225.836f, 12.3441f, 224.114f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(86.2459f, 203.442f)
                lineTo(76.9436f, 206.026f)
                curveTo(83.972f, 204.372f, 82.1116f, 217.912f, 81.0779f, 219.462f)
                lineTo(87.7963f, 217.395f)
                curveTo(92.4474f, 210.16f, 88.6576f, 205.164f, 86.2459f, 203.442f)
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
                moveTo(176.2459f, 239.925f)
                lineTo(92.0083f, 263.697f)
                curveTo(99.8636f, 261.63f, 98.3821f, 272.827f, 96.6595f, 278.684f)
                lineTo(177.7963f, 254.912f)
                curveTo(182.3441f, 249.124f, 178.6577f, 242.509f, 176.2459f, 239.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(92.00847f, 263.698f)
                curveTo(90.4581f, 268.349f, 89.9413f, 276.617f, 96.1429f, 278.684f)
                curveTo(98.21f, 273.517f, 100.2772f, 263.285f, 92.0085f, 263.698f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(102.3441f, 261.114f)
                lineTo(92.0083f, 263.698f)
                curveTo(99.0367f, 262.044f, 98.2098f, 275.584f, 96.6595f, 278.685f)
                lineTo(103.8944f, 276.617f)
                curveTo(110.096f, 269.589f, 104.7558f, 262.836f, 102.3441f, 261.114f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(176.2459f, 240.442f)
                lineTo(166.9436f, 243.026f)
                curveTo(173.972f, 241.372f, 172.1116f, 254.912f, 171.0779f, 256.462f)
                lineTo(177.7963f, 254.395f)
                curveTo(182.4474f, 247.16f, 178.6576f, 242.164f, 176.2459f, 240.442f)
                close()
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N600),
                strokeAlpha = 1.0f,
                strokeLineWidth = 13f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(47.5f, 216f)
                lineTo(47.5f, 60f)
                curveTo(47.5f, 50.666667f, 52.166667f, 47.918519f, 61.5f, 51.755556f)
                lineTo(123.5f, 77.244444f)
                curveTo(132.833333f, 81.081481f, 137.5f, 87.666667f, 137.5f, 97f)
                lineTo(137.5f, 253f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralDark.N700),
                strokeAlpha = 1.0f,
                strokeLineWidth = 11.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(47.5f, 216f)
                lineTo(47.5f, 60f)
                curveTo(47.5f, 50.666667f, 52.166667f, 47.918519f, 61.5f, 51.755556f)
                lineTo(123.5f, 77.244444f)
                curveTo(132.833333f, 81.081481f, 137.5f, 87.666667f, 137.5f, 97f)
                lineTo(137.5f, 253f)
            }
            path(
                fill = null,
                fillAlpha = 1.0f,
                stroke = SolidColor(AppPalette.NeutralLight.N200),
                strokeAlpha = 1.0f,
                strokeLineWidth = 8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(46.25f, 215.4f)
                lineTo(46.25f, 59.4f)
                curveTo(46.25f, 50.066667f, 50.916667f, 47.318519f, 60.25f, 51.155556f)
                lineTo(122.25f, 76.644444f)
                curveTo(131.583333f, 80.481481f, 136.25f, 87.066667f, 136.25f, 96.4f)
                lineTo(136.25f, 252.4f)
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(78.971514f, 51.910432f)
                lineTo(110.971514f, 65.065988f)
                curveTo(110.711174f, 69.644197f, 109.063498f, 73.652057f, 106.028486f, 77.089568f)
                lineTo(74.028486f, 63.934012f)
                curveTo(74.288826f, 59.355803f, 75.936502f, 55.347943f, 78.971514f, 51.910432f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(80.073675f, 53.17445f)
                lineTo(109.299003f, 65.189306f)
                curveTo(108.766334f, 69.114954f, 107.308775f, 72.660369f, 104.926325f, 75.82555f)
                lineTo(75.700997f, 63.810694f)
                curveTo(76.233666f, 59.885046f, 77.691225f, 56.339631f, 80.073675f, 53.17445f)
                close()
            }
        }.build()
        return _DipBar!!
    }

