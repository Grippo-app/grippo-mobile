package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

public val CordHandles: ImageVector
    get() {
        if (_Cordhandles != null) {
            return _Cordhandles!!
        }
        _Cordhandles = ImageVector.Builder(
            name = "Cordhandles",
            defaultWidth = 182.dp,
            defaultHeight = 168.dp,
            viewportWidth = 182f,
            viewportHeight = 168f
        ).apply {
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(3.27f, 74.92f)
                curveTo(6.31f, 47.13f, 22.58f, 32.82f, 30.33f, 29.13f)
                curveTo(34.57f, 33.3f, 40.51f, 42.77f, 30.33f, 47.27f)
                curveTo(20.15f, 51.78f, 16.77f, 90.74f, 16.36f, 109.66f)
                lineTo(11.78f, 125.8f)
                curveTo(1.04f, 121.84f, -0.54f, 109.66f, 3.27f, 74.92f)
                close()
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 2.5307f, y = 108.96f)
                curveTo(x1 = 6.9391f, y1 = 107.9732f, x2 = 13.6731f, y2 = 106.1705f, x3 = 15.8927f, y3 = 104.4989f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 3.3676f, y = 97.2526f)
                curveTo(x1 = 7.6512f, y1 = 96.2423f, x2 = 14.19f, y2 = 94.3878f, x3 = 16.3488f, y3 = 92.6671f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 3.9774f, y = 87.1525f)
                curveTo(x1 = 8.3467f, y1 = 86.1122f, x2 = 15.0182f, y2 = 84.2061f, x3 = 17.2179f, y3 = 82.4426f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 5.093f, y = 76.0426f)
                curveTo(x1 = 9.7092f, y1 = 75.0323f, x2 = 16.7586f, y2 = 73.1778f, x3 = 19.0836f, y3 = 71.4571f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 6.6414f, y = 66.338f)
                curveTo(x1 = 11.5108f, y1 = 65.1652f, x2 = 18.939f, y2 = 63.0196f, x3 = 21.3892f, y3 = 61.0276f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 9.9312f, y = 56.5726f)
                curveTo(x1 = 15.0035f, y1 = 55.5039f, x2 = 22.7528f, y2 = 53.5526f, x3 = 25.3099f, y3 = 51.7479f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 14.9975f, y = 46.0687f)
                curveTo(x1 = 21.6132f, y1 = 44.6579f, x2 = 31.7151f, y2 = 42.0735f, x3 = 35.0476f, y3 = 39.6771f)
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(84.97f, 74.78f)
                curveTo(81.92f, 46.99f, 65.66f, 32.68f, 57.91f, 29f)
                curveTo(53.66f, 33.17f, 47.73f, 42.63f, 57.91f, 47.14f)
                curveTo(68.09f, 51.64f, 71.46f, 90.6f, 71.88f, 109.52f)
                lineTo(76.46f, 125.8f)
                curveTo(87.2f, 121.7f, 88.77f, 109.52f, 84.97f, 74.78f)
                close()
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 85.6993f, y = 108.83f)
                curveTo(x1 = 81.2909f, y1 = 107.8432f, x2 = 74.5569f, y2 = 106.0405f, x3 = 72.3373f, y3 = 104.3689f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 84.8697f, y = 97.1199f)
                curveTo(x1 = 80.5797f, y1 = 96.1032f, x2 = 74.04f, y2 = 94.2487f, x3 = 71.8812f, y3 = 92.5344f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 84.2529f, y = 87.0127f)
                curveTo(x1 = 79.8849f, y1 = 85.974f, x2 = 73.2193f, y2 = 84.0743f, x3 = 71.0145f, y3 = 82.3123f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 83.1443f, y = 75.9099f)
                curveTo(x1 = 78.5216f, y1 = 74.8932f, x2 = 71.4715f, y2 = 73.0387f, x3 = 69.1464f, y3 = 71.3244f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 81.5983f, y = 66.198f)
                curveTo(x1 = 76.7275f, y1 = 65.0253f, x2 = 69.2927f, y2 = 62.8804f, x3 = 66.8411f, y3 = 60.8949f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 78.3061f, y = 56.4353f)
                curveTo(x1 = 73.2273f, y1 = 55.373f, x2 = 65.4773f, y2 = 53.4217f, x3 = 62.9201f, y3 = 51.6106f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 73.2328f, y = 45.9359f)
                curveTo(x1 = 66.6185f, y1 = 44.5187f, x2 = 56.5232f, y2 = 41.9336f, x3 = 53.1921f, y3 = 39.5372f)
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200), pathFillType = PathFillType.EvenOdd) {
                moveTo(56.51f, 46.39f)
                curveTo(48.49f, 51.31f, 36.59f, 48.74f, 31.64f, 46.83f)
                curveTo(41.59f, 37.84f, 30.18f, 31.33f, 29.89f, 29.13f)
                curveTo(29.65f, 27.38f, 35.45f, 24.99f, 38.37f, 24.02f)
                verticalLineTo(6.32f)
                curveTo(38.4f, 4.05f, 39.18f, -0.38f, 42.1f, 0.03f)
                curveTo(45.03f, 0.44f, 46.39f, 3.22f, 46.71f, 4.56f)
                lineTo(48.47f, 24.75f)
                curveTo(46.42f, 24.97f, 43.62f, 25.27f, 48.83f, 24.75f)
                curveTo(54.04f, 24.22f, 57.39f, 27.45f, 58.41f, 29.13f)
                curveTo(50.63f, 36.45f, 53.9f, 43.69f, 56.51f, 46.39f)
                close()

                moveTo(42.1f, 3.46f)
                curveTo(43.17f, 6.17f, 44.68f, 12.59f, 42.1f, 16.63f)
                curveTo(38.88f, 21.67f, 39.91f, 0.61f, 42.1f, 3.46f)
                close()
            }

            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(77.5f, 126f)
                horizontalLineTo(11.5f)
                curveTo(11.5f, 126f, 12.4f, 121.56f, 13.78f, 117.5f)
                curveTo(15.13f, 113.51f, 16.28f, 107.5f, 16.28f, 107.5f)
                horizontalLineTo(72f)
                curveTo(72f, 107.5f, 73.45f, 113.69f, 74.5f, 117.5f)
                curveTo(75.69f, 121.8f, 77.5f, 126f, 77.5f, 126f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500)
            ) {
                moveTo(x = 16.28f, y = 107.5f)
                lineTo(x = 72f, y = 107.5f)
                lineTo(x = 73f, y = 111.5f)
                lineTo(x = 15.3f, y = 111.5f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(97.27f, 115.92f)
                curveTo(100.31f, 88.13f, 116.57f, 73.82f, 124.33f, 70.13f)
                curveTo(128.57f, 74.3f, 134.51f, 83.77f, 124.33f, 88.27f)
                curveTo(114.15f, 92.78f, 110.77f, 131.74f, 110.36f, 150.66f)
                lineTo(105.78f, 166.8f)
                curveTo(95.04f, 162.84f, 93.46f, 150.66f, 97.27f, 115.92f)
                close()
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 96.5307f, y = 149.96f)
                curveTo(x1 = 100.9391f, y1 = 148.9733f, x2 = 107.6731f, y2 = 147.1704f, x3 = 109.8927f, y3 = 145.4988f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 97.3676f, y = 138.2526f)
                curveTo(x1 = 101.6512f, y1 = 137.2423f, x2 = 108.19f, y2 = 135.3878f, x3 = 110.3488f, y3 = 133.6671f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 97.9774f, y = 128.1525f)
                curveTo(x1 = 102.3467f, y1 = 127.1122f, x2 = 109.0182f, y2 = 125.2061f, x3 = 111.2179f, y3 = 123.4426f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 99.093f, y = 117.0426f)
                curveTo(x1 = 103.7092f, y1 = 116.0323f, x2 = 110.7586f, y2 = 114.1778f, x3 = 113.0836f, y3 = 112.4571f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 100.6414f, y = 107.338f)
                curveTo(x1 = 105.5108f, y1 = 106.1652f, x2 = 112.939f, y2 = 104.0196f, x3 = 115.3892f, y3 = 102.0276f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 103.9312f, y = 97.5726f)
                curveTo(x1 = 109.0035f, y1 = 96.5039f, x2 = 116.7528f, y2 = 94.5526f, x3 = 119.3099f, y3 = 92.7479f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 108.9975f, y = 87.0687f)
                curveTo(x1 = 115.6132f, y1 = 85.6579f, x2 = 125.7151f, y2 = 83.0735f, x3 = 129.0476f, y3 = 80.6771f)
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(178.97f, 115.78f)
                curveTo(175.93f, 87.99f, 159.66f, 73.68f, 151.91f, 70f)
                curveTo(147.66f, 74.17f, 141.73f, 83.63f, 151.91f, 88.14f)
                curveTo(162.09f, 92.64f, 165.46f, 131.6f, 165.88f, 150.52f)
                lineTo(170.46f, 166.8f)
                curveTo(181.2f, 162.7f, 182.77f, 150.52f, 178.97f, 115.78f)
                close()
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 179.6993f, y = 149.83f)
                curveTo(x1 = 175.2909f, y1 = 148.8432f, x2 = 168.5569f, y2 = 147.0396f, x3 = 166.3373f, y3 = 145.3616f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 178.8721f, y = 138.1199f)
                curveTo(x1 = 174.5871f, y1 = 137.1032f, x2 = 168.0417f, y2 = 135.2487f, x3 = 165.8815f, y3 = 133.5344f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 178.2529f, y = 128.0128f)
                curveTo(x1 = 173.8849f, y1 = 126.974f, x2 = 167.2193f, y2 = 125.0743f, x3 = 165.0145f, y3 = 123.3123f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 177.1443f, y = 116.9099f)
                curveTo(x1 = 172.5216f, y1 = 115.8932f, x2 = 165.4715f, y2 = 114.0387f, x3 = 163.1464f, y3 = 112.3244f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 175.5983f, y = 107.198f)
                curveTo(x1 = 170.7275f, y1 = 106.0253f, x2 = 163.2927f, y2 = 103.8804f, x3 = 160.8411f, y3 = 101.8949f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 172.3061f, y = 97.4353f)
                curveTo(x1 = 167.2273f, y1 = 96.373f, x2 = 159.4773f, y2 = 94.4217f, x3 = 156.9201f, y3 = 92.6106f)
            }
            path(
                stroke = SolidColor(AppPalette.NeutralDark.N300),
                strokeLineWidth = 1f
            ) {
                moveTo(x = 167.2328f, y = 86.9359f)
                curveTo(x1 = 160.6184f, y1 = 85.5187f, x2 = 150.5224f, y2 = 82.9335f, x3 = 147.1848f, y3 = 80.5371f)
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200), pathFillType = PathFillType.EvenOdd) {
                moveTo(150.51f, 87.39f)
                curveTo(142.49f, 92.31f, 130.59f, 89.74f, 125.64f, 87.83f)
                curveTo(135.59f, 78.84f, 124.18f, 72.33f, 123.89f, 70.13f)
                curveTo(123.65f, 68.38f, 129.45f, 65.99f, 132.37f, 65.02f)
                verticalLineTo(47.32f)
                curveTo(132.4f, 45.05f, 133.18f, 40.62f, 136.1f, 41.03f)
                curveTo(139.03f, 41.44f, 140.39f, 44.22f, 140.71f, 45.56f)
                lineTo(142.46f, 65.75f)
                curveTo(140.42f, 65.97f, 137.62f, 66.27f, 142.83f, 65.75f)
                curveTo(148.04f, 65.22f, 151.39f, 68.45f, 152.41f, 70.13f)
                curveTo(144.63f, 77.45f, 147.9f, 84.69f, 150.51f, 87.39f)
                close()

                moveTo(136.1f, 44.46f)
                curveTo(137.18f, 47.17f, 138.68f, 53.59f, 136.1f, 57.63f)
                curveTo(132.88f, 62.67f, 133.91f, 41.61f, 136.1f, 44.46f)
                close()
            }

            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(171f, 167f)
                horizontalLineTo(105.5f)
                curveTo(105.5f, 167f, 106.4f, 162.55f, 107.78f, 158.5f)
                curveTo(109.13f, 154.51f, 110.5f, 149.5f, 110.5f, 149.5f)
                horizontalLineTo(166f)
                curveTo(166f, 149.5f, 167.45f, 154.69f, 168.5f, 158.5f)
                curveTo(169.69f, 162.8f, 171f, 167f, 171f, 167f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500)
            ) {
                moveTo(x = 110.5f, y = 149.5f)
                lineTo(x = 165.8f, y = 149.5f)
                lineTo(x = 167.1f, y = 153.5f)
                lineTo(x = 109.4f, y = 153.5f)
                close()
            }
        }.build()

        return _Cordhandles!!
    }

@Suppress("ObjectPropertyName")
private var _Cordhandles: ImageVector? = null
