package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _FlatBench: ImageVector? = null

public val FlatBench: ImageVector
    get() {
        if (_FlatBench != null) {
            return _FlatBench!!
        }
        _FlatBench = ImageVector.Builder(
            name = "FlatBench",
            defaultWidth = 233.dp,
            defaultHeight = 234.dp,
            viewportWidth = 233f,
            viewportHeight = 234f
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
                moveTo(46.0882f, 194.562f)
                lineTo(32.0385f, 185.376f)
                lineTo(204.868f, 115.112f)
                lineTo(218.075f, 121.054f)
                lineTo(46.0882f, 194.562f)
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
                moveTo(46.0882f, 212.395f)
                lineTo(46.0882f, 194.562f)
                lineTo(218.075f, 121.054f)
                lineTo(218.075f, 135.487f)
                lineTo(46.0882f, 212.395f)
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
                moveTo(32.0383f, 203.208f)
                lineTo(32.0383f, 185.376f)
                lineTo(46.0882f, 194.562f)
                lineTo(46.0882f, 212.395f)
                lineTo(32.0383f, 203.208f)
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
                moveTo(76.3496f, 186.077f)
                lineTo(76.3496f, 116.748f)
                lineTo(59.968f, 123.514f)
                lineTo(59.968f, 193.504f)
                lineTo(76.3496f, 186.077f)
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
                moveTo(46.1025f, 179.973f)
                lineTo(59.9683f, 188.5f)
                lineTo(59.9682f, 125.496f)
                lineTo(46.1025f, 129.457f)
                lineTo(46.1025f, 179.973f)
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
                moveTo(44.1423f, 121.059f)
                lineTo(28.3603f, 112.471f)
                lineTo(191.315f, 41.2584f)
                lineTo(208.169f, 45.5834f)
                lineTo(44.1423f, 121.059f)
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
                moveTo(44.4203f, 132.798f)
                lineTo(44.0257f, 119.864f)
                lineTo(212.131f, 44.1067f)
                lineTo(212.131f, 57.9728f)
                lineTo(44.4203f, 132.798f)
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
                moveTo(28.7973f, 125.393f)
                lineTo(27.2532f, 114.451f)
                lineTo(43.9503f, 121.714f)
                lineTo(44.4204f, 132.939f)
                lineTo(28.7973f, 125.393f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400)
            ) {
                moveTo(x = 13f, y = 98f)
                lineTo(x = 184f, y = 31.5f)
                lineTo(x = 219.4f, y = 44.9f)
                lineTo(x = 49f, y = 115f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300)
            ) {
                moveTo(x = 49f, y = 115f)
                lineTo(x = 219.4f, y = 44.9f)
                lineTo(x = 219.4f, y = 52.1f)
                lineTo(x = 49f, y = 124f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250)
            ) {
                moveTo(x = 13f, y = 98f)
                lineTo(x = 49f, y = 115f)
                lineTo(x = 49f, y = 124f)
                lineTo(x = 13f, y = 107f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450)
            ) {
                moveTo(x = 13f, y = 98f)
                lineTo(x = 184f, y = 31.5f)
                lineTo(x = 188.1f, y = 33.1f)
                lineTo(x = 17.1f, y = 100f)
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
                moveTo(182.678f, 138.564f)
                lineTo(182.678f, 69.2349f)
                lineTo(171.454f, 73.8568f)
                lineTo(171.454f, 143.846f)
                lineTo(182.678f, 138.564f)
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
                moveTo(159.969f, 134.92f)
                lineTo(171.454f, 140.8f)
                lineTo(171.454f, 75.8396f)
                lineTo(159.969f, 81.0683f)
                lineTo(159.969f, 134.92f)
                close()
            }
        }.build()
        return _FlatBench!!
    }

