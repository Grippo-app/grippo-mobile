package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _BeltSquat: ImageVector? = null

public val BeltSquat: ImageVector
    get() {
        if (_BeltSquat != null) {
            return _BeltSquat!!
        }
        _BeltSquat = ImageVector.Builder(
            name = "BeltSquat",
            defaultWidth = 185.dp,
            defaultHeight = 188.dp,
            viewportWidth = 185f,
            viewportHeight = 188f,
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(75.5f, 104.85f)
                lineTo(80.7f, 102.05f)
                lineTo(90.9f, 106.25f)
                lineTo(85.7f, 109.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(75.5f, 107.7f)
                lineTo(75.5f, 104.85f)
                lineTo(85.7f, 109.05f)
                lineTo(85.7f, 111.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(90.9f, 109.1f)
                lineTo(85.7f, 111.9f)
                lineTo(85.7f, 109.05f)
                lineTo(90.9f, 106.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(82.05f, 104.175f)
                lineTo(78.15f, 106.275f)
                lineTo(78.15f, 81.575f)
                lineTo(82.05f, 79.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(78.15f, 106.275f)
                lineTo(73.05f, 104.175f)
                lineTo(73.05f, 79.475f)
                lineTo(78.15f, 81.575f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(76.95f, 77.375f)
                lineTo(82.05f, 79.475f)
                lineTo(78.15f, 81.575f)
                lineTo(73.05f, 79.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(77.525f, 98.725f)
                lineTo(82.075f, 96.275f)
                lineTo(170.475f, 132.675f)
                lineTo(165.925f, 135.125f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(77.525f, 105.375f)
                lineTo(77.525f, 98.725f)
                lineTo(165.925f, 135.125f)
                lineTo(165.925f, 141.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(79.332f, 67.126f)
                lineTo(80.893f, 69.899f)
                lineTo(68.543f, 87.949f)
                lineTo(66.982f, 85.176f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(75.507f, 65.551f)
                lineTo(79.332f, 67.126f)
                lineTo(66.982f, 85.176f)
                lineTo(63.157f, 83.601f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(114.15f, 116.775f)
                lineTo(110.25f, 118.875f)
                lineTo(110.25f, 106.525f)
                lineTo(114.15f, 104.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(110.25f, 118.875f)
                lineTo(105.15f, 116.775f)
                lineTo(105.15f, 104.425f)
                lineTo(110.25f, 106.525f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(82.7f, 102.875f)
                lineTo(78.8f, 104.975f)
                lineTo(78.8f, 9.025f)
                lineTo(82.7f, 6.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(63.157f, 83.601f)
                lineTo(66.982f, 85.176f)
                lineTo(68.543f, 87.949f)
                lineTo(64.718f, 86.374f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(78.8f, 104.975f)
                lineTo(73.7f, 102.875f)
                lineTo(73.7f, 6.925f)
                lineTo(78.8f, 9.025f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(109.05f, 102.325f)
                lineTo(114.15f, 104.425f)
                lineTo(110.25f, 106.525f)
                lineTo(105.15f, 104.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(74.19f, 80.097f)
                lineTo(74.71f, 79.818f)
                lineTo(99.36f, 89.968f)
                lineTo(98.84f, 90.248f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(74.19f, 80.763f)
                lineTo(74.19f, 80.097f)
                lineTo(98.84f, 90.248f)
                lineTo(98.84f, 90.912f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(103.775f, 100.225f)
                lineTo(107.025f, 98.475f)
                lineTo(132.525f, 108.975f)
                lineTo(129.275f, 110.725f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(81.075f, 98.025f)
                lineTo(87.025f, 100.475f)
                lineTo(15.525f, 138.975f)
                lineTo(9.575f, 136.525f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(87.025f, 100.475f)
                lineTo(87.025f, 107.125f)
                lineTo(15.525f, 145.625f)
                lineTo(15.525f, 138.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(103.775f, 104.975f)
                lineTo(103.775f, 100.225f)
                lineTo(129.275f, 110.725f)
                lineTo(129.275f, 115.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(131.15f, 123.775f)
                lineTo(127.25f, 125.875f)
                lineTo(127.25f, 113.525f)
                lineTo(131.15f, 111.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(127.25f, 125.875f)
                lineTo(122.15f, 123.775f)
                lineTo(122.15f, 111.425f)
                lineTo(127.25f, 113.525f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(126.05f, 109.325f)
                lineTo(131.15f, 111.425f)
                lineTo(127.25f, 113.525f)
                lineTo(122.15f, 111.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(76.9f, 78.875f)
                lineTo(80.8f, 76.775f)
                lineTo(160.7f, 109.675f)
                lineTo(156.8f, 111.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(76.9f, 84.575f)
                lineTo(76.9f, 78.875f)
                lineTo(156.8f, 111.775f)
                lineTo(156.8f, 117.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(99.36f, 90.633f)
                lineTo(98.84f, 90.912f)
                lineTo(98.84f, 90.248f)
                lineTo(99.36f, 89.968f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(132.525f, 113.725f)
                lineTo(129.275f, 115.475f)
                lineTo(129.275f, 110.725f)
                lineTo(132.525f, 108.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(127.775f, 114.15f)
                curveTo(127.775f, 111.422f, 129.288f, 108.395f, 131.155f, 107.39f)
                curveTo(133.022f, 106.385f, 134.535f, 107.782f, 134.535f, 110.51f)
                curveTo(134.535f, 113.238f, 133.022f, 116.265f, 131.155f, 117.27f)
                curveTo(129.288f, 118.275f, 127.775f, 116.878f, 127.775f, 114.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(129.895f, 113.17f)
                curveTo(129.895f, 112.016f, 130.535f, 110.735f, 131.325f, 110.31f)
                curveTo(132.115f, 109.885f, 132.755f, 110.476f, 132.755f, 111.63f)
                curveTo(132.755f, 112.784f, 132.115f, 114.065f, 131.325f, 114.49f)
                curveTo(130.535f, 114.915f, 129.895f, 114.324f, 129.895f, 113.17f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(65.74f, 84.648f)
                lineTo(66.26f, 84.368f)
                lineTo(90.91f, 94.518f)
                lineTo(90.39f, 94.797f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(65.74f, 85.312f)
                lineTo(65.74f, 84.648f)
                lineTo(90.39f, 94.797f)
                lineTo(90.39f, 95.463f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(157.1f, 138.45f)
                lineTo(162.3f, 135.65f)
                lineTo(172.5f, 139.85f)
                lineTo(167.3f, 142.65f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(157.1f, 141.3f)
                lineTo(157.1f, 138.45f)
                lineTo(167.3f, 142.65f)
                lineTo(167.3f, 145.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(172.5f, 142.7f)
                lineTo(167.3f, 145.5f)
                lineTo(167.3f, 142.65f)
                lineTo(172.5f, 139.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(75f, 77.475f)
                lineTo(80.1f, 79.575f)
                lineTo(20.95f, 111.425f)
                lineTo(15.85f, 109.325f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(80.1f, 79.575f)
                lineTo(80.1f, 85.275f)
                lineTo(20.95f, 117.125f)
                lineTo(20.95f, 111.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(170.475f, 139.325f)
                lineTo(165.925f, 141.775f)
                lineTo(165.925f, 135.125f)
                lineTo(170.475f, 132.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(77.6f, 4.825f)
                lineTo(82.7f, 6.925f)
                lineTo(78.8f, 9.025f)
                lineTo(73.7f, 6.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(90.91f, 95.183f)
                lineTo(90.39f, 95.463f)
                lineTo(90.39f, 94.797f)
                lineTo(90.91f, 94.518f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(57.29f, 89.198f)
                lineTo(57.81f, 88.918f)
                lineTo(82.46f, 99.067f)
                lineTo(81.94f, 99.347f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(57.29f, 89.863f)
                lineTo(57.29f, 89.198f)
                lineTo(81.94f, 99.347f)
                lineTo(81.94f, 100.013f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(115.497f, 102.285f)
                lineTo(121.447f, 104.735f)
                lineTo(85.047f, 116.735f)
                lineTo(79.097f, 114.285f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(121.447f, 104.735f)
                lineTo(120.803f, 111.665f)
                lineTo(84.403f, 123.665f)
                lineTo(85.047f, 116.735f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(77.35f, 75.45f)
                lineTo(110.5f, 89.1f)
                lineTo(51.35f, 120.95f)
                lineTo(18.2f, 107.3f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(77.75f, 76.375f)
                lineTo(107.5f, 88.625f)
                lineTo(50.95f, 119.075f)
                lineTo(21.2f, 106.825f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(161.95f, 137.075f)
                lineTo(158.05f, 139.175f)
                lineTo(158.05f, 114.475f)
                lineTo(161.95f, 112.375f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(48.84f, 93.748f)
                lineTo(49.36f, 93.468f)
                lineTo(74.01f, 103.618f)
                lineTo(73.49f, 103.898f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(48.84f, 94.412f)
                lineTo(48.84f, 93.748f)
                lineTo(73.49f, 103.898f)
                lineTo(73.49f, 104.562f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(158.05f, 139.175f)
                lineTo(152.95f, 137.075f)
                lineTo(152.95f, 112.375f)
                lineTo(158.05f, 114.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(160.7f, 115.375f)
                lineTo(156.8f, 117.475f)
                lineTo(156.8f, 111.775f)
                lineTo(160.7f, 109.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(122.64f, 100.047f)
                lineTo(123.16f, 99.768f)
                lineTo(147.81f, 109.918f)
                lineTo(147.29f, 110.198f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(122.64f, 100.713f)
                lineTo(122.64f, 100.047f)
                lineTo(147.29f, 110.198f)
                lineTo(147.29f, 110.862f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(156.85f, 110.275f)
                lineTo(161.95f, 112.375f)
                lineTo(158.05f, 114.475f)
                lineTo(152.95f, 112.375f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(110.5f, 89.1f)
                lineTo(110.5f, 93.85f)
                lineTo(51.35f, 125.7f)
                lineTo(51.35f, 120.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(76.288f, 4f)
                lineTo(80.112f, 5.575f)
                lineTo(47.613f, 23.075f)
                lineTo(43.788f, 21.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(80.112f, 5.575f)
                lineTo(80.112f, 9.85f)
                lineTo(47.613f, 27.35f)
                lineTo(47.613f, 23.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(108.562f, 89.3f)
                lineTo(109.838f, 89.825f)
                lineTo(53.288f, 120.275f)
                lineTo(52.013f, 119.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(109.838f, 89.825f)
                lineTo(109.838f, 91.25f)
                lineTo(53.288f, 121.7f)
                lineTo(53.288f, 120.275f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(74.01f, 104.282f)
                lineTo(73.49f, 104.562f)
                lineTo(73.49f, 103.898f)
                lineTo(74.01f, 103.618f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(4f, 143.35f)
                lineTo(9.2f, 140.55f)
                lineTo(19.4f, 144.75f)
                lineTo(14.2f, 147.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(40.39f, 98.297f)
                lineTo(40.91f, 98.018f)
                lineTo(65.56f, 108.168f)
                lineTo(65.04f, 108.448f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(40.39f, 98.963f)
                lineTo(40.39f, 98.297f)
                lineTo(65.04f, 108.448f)
                lineTo(65.04f, 109.113f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(147.81f, 110.583f)
                lineTo(147.29f, 110.862f)
                lineTo(147.29f, 110.198f)
                lineTo(147.81f, 109.918f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(4f, 146.2f)
                lineTo(4f, 143.35f)
                lineTo(14.2f, 147.55f)
                lineTo(14.2f, 150.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(9.575f, 136.525f)
                lineTo(15.525f, 138.975f)
                lineTo(15.525f, 145.625f)
                lineTo(9.575f, 143.175f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(19.4f, 147.6f)
                lineTo(14.2f, 150.4f)
                lineTo(14.2f, 147.55f)
                lineTo(19.4f, 144.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(98f, 111.175f)
                lineTo(101.9f, 109.075f)
                lineTo(154.6f, 130.775f)
                lineTo(150.7f, 132.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(114.19f, 104.597f)
                lineTo(114.71f, 104.317f)
                lineTo(139.36f, 114.468f)
                lineTo(138.84f, 114.748f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(23.55f, 135.675f)
                lineTo(19.65f, 137.775f)
                lineTo(19.65f, 113.075f)
                lineTo(23.55f, 110.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(114.19f, 105.263f)
                lineTo(114.19f, 104.597f)
                lineTo(138.84f, 114.748f)
                lineTo(138.84f, 115.412f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(98f, 116.875f)
                lineTo(98f, 111.175f)
                lineTo(150.7f, 132.875f)
                lineTo(150.7f, 138.575f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(19.65f, 137.775f)
                lineTo(14.55f, 135.675f)
                lineTo(14.55f, 110.975f)
                lineTo(19.65f, 113.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(87.25f, 117.225f)
                lineTo(85.3f, 118.275f)
                lineTo(85.3f, 114.475f)
                lineTo(87.25f, 113.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(84.7f, 112.375f)
                lineTo(87.25f, 113.425f)
                lineTo(85.3f, 114.475f)
                lineTo(82.75f, 113.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(159.232f, 100.026f)
                lineTo(160.793f, 102.799f)
                lineTo(148.443f, 120.849f)
                lineTo(146.882f, 118.076f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(85.3f, 118.275f)
                lineTo(82.75f, 117.225f)
                lineTo(82.75f, 113.425f)
                lineTo(85.3f, 114.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(155.407f, 98.451f)
                lineTo(159.232f, 100.026f)
                lineTo(146.882f, 118.076f)
                lineTo(143.057f, 116.501f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(87.12f, 115.02f)
                curveTo(87.12f, 113.551f, 86.054f, 111.921f, 84.74f, 111.38f)
                curveTo(83.426f, 110.839f, 82.36f, 111.591f, 82.36f, 113.06f)
                curveTo(82.36f, 114.529f, 83.426f, 116.159f, 84.74f, 116.7f)
                curveTo(86.054f, 117.241f, 87.12f, 116.489f, 87.12f, 115.02f)
                close()
                moveTo(85.845f, 114.495f)
                curveTo(85.845f, 113.813f, 85.35f, 113.056f, 84.74f, 112.805f)
                curveTo(84.13f, 112.554f, 83.635f, 112.903f, 83.635f, 113.585f)
                curveTo(83.635f, 114.267f, 84.13f, 115.024f, 84.74f, 115.275f)
                curveTo(85.35f, 115.526f, 85.845f, 115.177f, 85.845f, 114.495f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(87.55f, 112.575f)
                curveTo(87.55f, 110.45f, 86.408f, 108.257f, 85f, 107.678f)
                curveTo(83.592f, 107.098f, 82.45f, 108.35f, 82.45f, 110.475f)
                curveTo(82.45f, 112.6f, 83.592f, 114.793f, 85f, 115.373f)
                curveTo(86.408f, 115.952f, 87.55f, 114.7f, 87.55f, 112.575f)
                close()
                moveTo(86.487f, 112.138f)
                curveTo(86.487f, 110.898f, 85.822f, 109.619f, 85f, 109.281f)
                curveTo(84.178f, 108.942f, 83.513f, 109.673f, 83.513f, 110.912f)
                curveTo(83.513f, 112.152f, 84.178f, 113.431f, 85f, 113.769f)
                curveTo(85.822f, 114.108f, 86.487f, 113.377f, 86.487f, 112.138f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(79.097f, 114.285f)
                lineTo(85.047f, 116.735f)
                lineTo(84.403f, 123.665f)
                lineTo(78.453f, 121.215f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(15.85f, 109.325f)
                lineTo(20.95f, 111.425f)
                lineTo(20.95f, 117.125f)
                lineTo(15.85f, 115.025f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(123.863f, 95.6f)
                lineTo(125.138f, 96.125f)
                lineTo(68.588f, 126.575f)
                lineTo(67.312f, 126.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(162.6f, 135.775f)
                lineTo(158.7f, 137.875f)
                lineTo(158.7f, 41.925f)
                lineTo(162.6f, 39.825f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(125.138f, 96.125f)
                lineTo(125.138f, 97.55f)
                lineTo(68.588f, 128f)
                lineTo(68.588f, 126.575f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(143.057f, 116.501f)
                lineTo(146.882f, 118.076f)
                lineTo(148.443f, 120.849f)
                lineTo(144.618f, 119.274f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(18.45f, 108.875f)
                lineTo(23.55f, 110.975f)
                lineTo(19.65f, 113.075f)
                lineTo(14.55f, 110.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(65.56f, 108.833f)
                lineTo(65.04f, 109.113f)
                lineTo(65.04f, 108.448f)
                lineTo(65.56f, 108.168f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(158.7f, 137.875f)
                lineTo(153.6f, 135.775f)
                lineTo(153.6f, 39.825f)
                lineTo(158.7f, 41.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(56.235f, 13.655f)
                lineTo(61.165f, 15.685f)
                lineTo(46.865f, 23.385f)
                lineTo(41.935f, 21.355f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(61.165f, 15.685f)
                lineTo(61.165f, 21.195f)
                lineTo(46.865f, 28.895f)
                lineTo(46.865f, 23.385f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(83.05f, 106.875f)
                curveTo(83.05f, 104.75f, 83.923f, 102.557f, 85f, 101.977f)
                curveTo(86.077f, 101.398f, 86.95f, 102.65f, 86.95f, 104.775f)
                curveTo(86.95f, 106.9f, 86.077f, 109.093f, 85f, 109.672f)
                curveTo(83.923f, 110.252f, 83.05f, 109f, 83.05f, 106.875f)
                close()
                moveTo(83.862f, 106.438f)
                curveTo(83.862f, 105.198f, 84.372f, 103.919f, 85f, 103.581f)
                curveTo(85.628f, 103.242f, 86.138f, 103.973f, 86.138f, 105.213f)
                curveTo(86.138f, 106.452f, 85.628f, 107.731f, 85f, 108.069f)
                curveTo(84.372f, 108.408f, 83.862f, 107.677f, 83.862f, 106.438f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(31.94f, 102.847f)
                lineTo(32.46f, 102.567f)
                lineTo(57.11f, 112.718f)
                lineTo(56.59f, 112.998f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(31.94f, 103.513f)
                lineTo(31.94f, 102.847f)
                lineTo(56.59f, 112.998f)
                lineTo(56.59f, 113.662f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(139.36f, 115.132f)
                lineTo(138.84f, 115.412f)
                lineTo(138.84f, 114.748f)
                lineTo(139.36f, 114.468f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(87.55f, 101.175f)
                curveTo(87.55f, 99.05f, 86.408f, 96.857f, 85f, 96.278f)
                curveTo(83.592f, 95.698f, 82.45f, 96.95f, 82.45f, 99.075f)
                curveTo(82.45f, 101.2f, 83.592f, 103.393f, 85f, 103.972f)
                curveTo(86.408f, 104.552f, 87.55f, 103.3f, 87.55f, 101.175f)
                close()
                moveTo(86.487f, 100.738f)
                curveTo(86.487f, 99.498f, 85.822f, 98.219f, 85f, 97.881f)
                curveTo(84.178f, 97.542f, 83.513f, 98.273f, 83.513f, 99.513f)
                curveTo(83.513f, 100.752f, 84.178f, 102.031f, 85f, 102.369f)
                curveTo(85.822f, 102.708f, 86.487f, 101.977f, 86.487f, 100.738f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(105.74f, 109.148f)
                lineTo(106.26f, 108.868f)
                lineTo(130.91f, 119.017f)
                lineTo(130.39f, 119.297f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(105.74f, 109.812f)
                lineTo(105.74f, 109.148f)
                lineTo(130.39f, 119.297f)
                lineTo(130.39f, 119.963f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(66.155f, 61.758f)
                lineTo(68.535f, 62.738f)
                lineTo(85.735f, 99.888f)
                lineTo(83.355f, 98.908f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(68.535f, 62.738f)
                lineTo(69.445f, 62.292f)
                lineTo(86.645f, 99.442f)
                lineTo(85.735f, 99.888f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(43.788f, 21.5f)
                lineTo(47.613f, 23.075f)
                lineTo(47.613f, 27.35f)
                lineTo(43.788f, 25.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(66.155f, 61.758f)
                lineTo(67.065f, 61.312f)
                lineTo(69.445f, 62.292f)
                lineTo(68.535f, 62.738f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(69.625f, 62.9f)
                curveTo(69.625f, 61.693f, 68.75f, 60.355f, 67.67f, 59.91f)
                curveTo(66.59f, 59.465f, 65.715f, 60.083f, 65.715f, 61.29f)
                curveTo(65.715f, 62.497f, 66.59f, 63.835f, 67.67f, 64.28f)
                curveTo(68.75f, 64.725f, 69.625f, 64.107f, 69.625f, 62.9f)
                close()
                moveTo(68.605f, 62.48f)
                curveTo(68.605f, 61.903f, 68.186f, 61.263f, 67.67f, 61.05f)
                curveTo(67.154f, 60.837f, 66.735f, 61.133f, 66.735f, 61.71f)
                curveTo(66.735f, 62.287f, 67.154f, 62.927f, 67.67f, 63.14f)
                curveTo(68.186f, 63.353f, 68.605f, 63.057f, 68.605f, 62.48f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(41.935f, 21.355f)
                lineTo(46.865f, 23.385f)
                lineTo(46.865f, 28.895f)
                lineTo(41.935f, 26.865f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(160.975f, 130.925f)
                lineTo(166.925f, 133.375f)
                lineTo(95.425f, 171.875f)
                lineTo(89.475f, 169.425f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(166.925f, 133.375f)
                lineTo(166.925f, 140.025f)
                lineTo(95.425f, 178.525f)
                lineTo(95.425f, 171.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(41.115f, 21.445f)
                lineTo(46.385f, 23.615f)
                lineTo(46.385f, 29.505f)
                lineTo(41.115f, 27.335f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(42.415f, 20.745f)
                lineTo(47.685f, 22.915f)
                lineTo(46.385f, 23.615f)
                lineTo(41.115f, 21.445f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(47.685f, 22.915f)
                lineTo(47.685f, 28.805f)
                lineTo(46.385f, 29.505f)
                lineTo(46.385f, 23.615f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(57.11f, 113.383f)
                lineTo(56.59f, 113.662f)
                lineTo(56.59f, 112.998f)
                lineTo(57.11f, 112.718f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(125.8f, 95.4f)
                lineTo(158.95f, 109.05f)
                lineTo(99.8f, 140.9f)
                lineTo(66.65f, 127.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(18.2f, 107.3f)
                lineTo(51.35f, 120.95f)
                lineTo(51.35f, 125.7f)
                lineTo(18.2f, 112.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(126.2f, 96.325f)
                lineTo(155.95f, 108.575f)
                lineTo(99.4f, 139.025f)
                lineTo(69.65f, 126.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(154.6f, 136.475f)
                lineTo(150.7f, 138.575f)
                lineTo(150.7f, 132.875f)
                lineTo(154.6f, 130.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(70.377f, 64.581f)
                lineTo(70.46f, 64.036f)
                lineTo(70.707f, 63.542f)
                lineTo(71.118f, 63.103f)
                lineTo(71.687f, 62.723f)
                lineTo(72.41f, 62.407f)
                lineTo(73.278f, 62.157f)
                lineTo(74.285f, 61.975f)
                lineTo(75.42f, 61.864f)
                lineTo(76.672f, 61.824f)
                lineTo(78.029f, 61.855f)
                lineTo(79.479f, 61.959f)
                lineTo(81.007f, 62.132f)
                lineTo(82.598f, 62.375f)
                lineTo(84.238f, 62.684f)
                lineTo(85.91f, 63.056f)
                lineTo(87.598f, 63.488f)
                lineTo(89.286f, 63.976f)
                lineTo(90.958f, 64.515f)
                lineTo(92.597f, 65.1f)
                lineTo(94.189f, 65.725f)
                lineTo(95.717f, 66.384f)
                lineTo(97.167f, 67.071f)
                lineTo(98.524f, 67.78f)
                lineTo(99.777f, 68.502f)
                lineTo(100.912f, 69.233f)
                lineTo(101.919f, 69.963f)
                lineTo(102.788f, 70.687f)
                lineTo(103.511f, 71.397f)
                lineTo(104.081f, 72.087f)
                lineTo(104.492f, 72.75f)
                lineTo(104.74f, 73.38f)
                lineTo(104.823f, 73.969f)
                lineTo(104.823f, 64.469f)
                lineTo(104.74f, 63.88f)
                lineTo(104.492f, 63.25f)
                lineTo(104.081f, 62.587f)
                lineTo(103.511f, 61.897f)
                lineTo(102.788f, 61.187f)
                lineTo(101.919f, 60.463f)
                lineTo(100.912f, 59.733f)
                lineTo(99.777f, 59.002f)
                lineTo(98.524f, 58.28f)
                lineTo(97.167f, 57.571f)
                lineTo(95.717f, 56.884f)
                lineTo(94.189f, 56.225f)
                lineTo(92.597f, 55.6f)
                lineTo(90.958f, 55.015f)
                lineTo(89.286f, 54.476f)
                lineTo(87.598f, 53.988f)
                lineTo(85.91f, 53.556f)
                lineTo(84.238f, 53.184f)
                lineTo(82.598f, 52.875f)
                lineTo(81.007f, 52.632f)
                lineTo(79.479f, 52.459f)
                lineTo(78.029f, 52.355f)
                lineTo(76.672f, 52.324f)
                lineTo(75.42f, 52.364f)
                lineTo(74.285f, 52.475f)
                lineTo(73.278f, 52.657f)
                lineTo(72.41f, 52.907f)
                lineTo(71.687f, 53.223f)
                lineTo(71.118f, 53.603f)
                lineTo(70.707f, 54.042f)
                lineTo(70.46f, 54.536f)
                lineTo(70.377f, 55.081f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(143.135f, 143.11f)
                curveTo(143.135f, 132.617f, 148.955f, 120.976f, 156.135f, 117.11f)
                curveTo(163.315f, 113.244f, 169.135f, 118.617f, 169.135f, 129.11f)
                curveTo(169.135f, 139.603f, 163.315f, 151.244f, 156.135f, 155.11f)
                curveTo(148.955f, 158.976f, 143.135f, 153.603f, 143.135f, 143.11f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(97.29f, 113.698f)
                lineTo(97.81f, 113.418f)
                lineTo(122.46f, 123.567f)
                lineTo(121.94f, 123.847f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(97.29f, 114.362f)
                lineTo(97.29f, 113.698f)
                lineTo(121.94f, 123.847f)
                lineTo(121.94f, 124.513f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(163.221f, 148.224f)
                lineTo(161.738f, 150.238f)
                lineTo(160.159f, 152.01f)
                lineTo(158.511f, 153.511f)
                lineTo(156.822f, 154.713f)
                lineTo(155.122f, 155.598f)
                lineTo(153.439f, 156.149f)
                lineTo(151.802f, 156.357f)
                lineTo(150.239f, 156.218f)
                lineTo(148.777f, 155.736f)
                lineTo(147.441f, 154.918f)
                lineTo(146.254f, 153.778f)
                lineTo(145.236f, 152.335f)
                lineTo(144.404f, 150.615f)
                lineTo(143.773f, 148.647f)
                lineTo(143.354f, 146.464f)
                lineTo(143.153f, 144.105f)
                lineTo(143.175f, 141.608f)
                lineTo(143.418f, 139.017f)
                lineTo(143.878f, 136.377f)
                lineTo(144.549f, 133.732f)
                lineTo(145.417f, 131.127f)
                lineTo(146.47f, 128.608f)
                lineTo(147.687f, 126.218f)
                lineTo(149.049f, 123.996f)
                lineTo(153.979f, 126.026f)
                lineTo(152.617f, 128.248f)
                lineTo(151.4f, 130.638f)
                lineTo(150.347f, 133.157f)
                lineTo(149.479f, 135.762f)
                lineTo(148.808f, 138.407f)
                lineTo(148.348f, 141.047f)
                lineTo(148.105f, 143.638f)
                lineTo(148.083f, 146.135f)
                lineTo(148.284f, 148.494f)
                lineTo(148.703f, 150.677f)
                lineTo(149.334f, 152.645f)
                lineTo(150.166f, 154.365f)
                lineTo(151.184f, 155.808f)
                lineTo(152.371f, 156.948f)
                lineTo(153.707f, 157.766f)
                lineTo(155.169f, 158.248f)
                lineTo(156.732f, 158.387f)
                lineTo(158.369f, 158.179f)
                lineTo(160.052f, 157.628f)
                lineTo(161.752f, 156.743f)
                lineTo(163.441f, 155.541f)
                lineTo(165.089f, 154.04f)
                lineTo(166.668f, 152.268f)
                lineTo(168.151f, 150.254f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(148.065f, 145.14f)
                curveTo(148.065f, 134.647f, 153.885f, 123.006f, 161.065f, 119.14f)
                curveTo(168.245f, 115.274f, 174.065f, 120.647f, 174.065f, 131.14f)
                curveTo(174.065f, 141.633f, 168.245f, 153.274f, 161.065f, 157.14f)
                curveTo(153.885f, 161.006f, 148.065f, 155.633f, 148.065f, 145.14f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(149.142f, 144.657f)
                curveTo(149.142f, 134.951f, 154.526f, 124.183f, 161.167f, 120.607f)
                curveTo(167.808f, 117.031f, 173.192f, 122.001f, 173.192f, 131.707f)
                curveTo(173.192f, 141.413f, 167.808f, 152.181f, 161.167f, 155.757f)
                curveTo(154.526f, 159.333f, 149.142f, 154.363f, 149.142f, 144.657f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(150.869f, 143.824f)
                curveTo(150.869f, 135.429f, 155.525f, 126.117f, 161.269f, 123.024f)
                curveTo(167.013f, 119.931f, 171.669f, 124.229f, 171.669f, 132.624f)
                curveTo(171.669f, 141.019f, 167.013f, 150.331f, 161.269f, 153.424f)
                curveTo(155.525f, 156.517f, 150.869f, 152.219f, 150.869f, 143.824f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(159.421f, 139.316f)
                curveTo(159.421f, 137.742f, 160.294f, 135.996f, 161.371f, 135.416f)
                curveTo(162.448f, 134.836f, 163.321f, 135.642f, 163.321f, 137.216f)
                curveTo(163.321f, 138.79f, 162.448f, 140.536f, 161.371f, 141.116f)
                curveTo(160.294f, 141.696f, 159.421f, 140.89f, 159.421f, 139.316f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(149.085f, 145.56f)
                curveTo(149.085f, 135.067f, 154.905f, 123.426f, 162.085f, 119.56f)
                curveTo(169.265f, 115.694f, 175.085f, 121.067f, 175.085f, 131.56f)
                curveTo(175.085f, 142.053f, 169.265f, 153.694f, 162.085f, 157.56f)
                curveTo(154.905f, 161.426f, 149.085f, 156.053f, 149.085f, 145.56f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(101.855f, 76.458f)
                lineTo(104.235f, 77.438f)
                lineTo(85.735f, 99.888f)
                lineTo(83.355f, 98.908f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(104.235f, 77.438f)
                lineTo(105.145f, 76.992f)
                lineTo(86.645f, 99.442f)
                lineTo(85.735f, 99.888f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(151.188f, 133.325f)
                lineTo(154.113f, 131.75f)
                lineTo(175.363f, 140.5f)
                lineTo(172.438f, 142.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(151.188f, 137.6f)
                lineTo(151.188f, 133.325f)
                lineTo(172.438f, 142.075f)
                lineTo(172.438f, 146.35f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(108f, 68.175f)
                curveTo(103.333f, 70.688f, 90.417f, 68.964f, 79.15f, 64.325f)
                curveTo(67.883f, 59.686f, 62.533f, 53.888f, 67.2f, 51.375f)
                curveTo(71.867f, 48.862f, 84.783f, 50.586f, 96.05f, 55.225f)
                curveTo(107.317f, 59.864f, 112.667f, 65.662f, 108f, 68.175f)
                close()
                moveTo(103.512f, 66.327f)
                curveTo(99.872f, 68.287f, 89.797f, 66.943f, 81.009f, 63.324f)
                curveTo(72.221f, 59.705f, 68.048f, 55.183f, 71.688f, 53.223f)
                curveTo(75.328f, 51.263f, 85.403f, 52.607f, 94.191f, 56.226f)
                curveTo(102.979f, 59.845f, 107.152f, 64.367f, 103.512f, 66.327f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(158.95f, 109.05f)
                lineTo(158.95f, 113.8f)
                lineTo(99.8f, 145.65f)
                lineTo(99.8f, 140.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(154.9f, 110.375f)
                lineTo(160f, 112.475f)
                lineTo(100.85f, 144.325f)
                lineTo(95.75f, 142.225f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(169.171f, 150.674f)
                lineTo(167.688f, 152.688f)
                lineTo(166.109f, 154.46f)
                lineTo(164.461f, 155.961f)
                lineTo(162.772f, 157.163f)
                lineTo(161.072f, 158.048f)
                lineTo(159.389f, 158.599f)
                lineTo(157.752f, 158.807f)
                lineTo(156.189f, 158.668f)
                lineTo(154.727f, 158.186f)
                lineTo(153.391f, 157.368f)
                lineTo(152.204f, 156.228f)
                lineTo(151.186f, 154.785f)
                lineTo(150.354f, 153.065f)
                lineTo(149.723f, 151.097f)
                lineTo(149.304f, 148.914f)
                lineTo(149.103f, 146.555f)
                lineTo(149.125f, 144.058f)
                lineTo(149.368f, 141.467f)
                lineTo(149.828f, 138.827f)
                lineTo(150.499f, 136.182f)
                lineTo(151.367f, 133.577f)
                lineTo(152.42f, 131.058f)
                lineTo(153.637f, 128.668f)
                lineTo(154.999f, 126.446f)
                lineTo(159.929f, 128.476f)
                lineTo(158.567f, 130.698f)
                lineTo(157.35f, 133.088f)
                lineTo(156.297f, 135.607f)
                lineTo(155.429f, 138.212f)
                lineTo(154.758f, 140.857f)
                lineTo(154.298f, 143.497f)
                lineTo(154.055f, 146.088f)
                lineTo(154.033f, 148.585f)
                lineTo(154.234f, 150.944f)
                lineTo(154.653f, 153.127f)
                lineTo(155.284f, 155.095f)
                lineTo(156.116f, 156.815f)
                lineTo(157.134f, 158.258f)
                lineTo(158.321f, 159.398f)
                lineTo(159.657f, 160.216f)
                lineTo(161.119f, 160.698f)
                lineTo(162.682f, 160.837f)
                lineTo(164.319f, 160.629f)
                lineTo(166.002f, 160.078f)
                lineTo(167.702f, 159.193f)
                lineTo(169.391f, 157.991f)
                lineTo(171.039f, 156.49f)
                lineTo(172.618f, 154.718f)
                lineTo(174.101f, 152.704f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(154.015f, 147.59f)
                curveTo(154.015f, 137.097f, 159.835f, 125.456f, 167.015f, 121.59f)
                curveTo(174.195f, 117.724f, 180.015f, 123.097f, 180.015f, 133.59f)
                curveTo(180.015f, 144.083f, 174.195f, 155.724f, 167.015f, 159.59f)
                curveTo(159.835f, 163.456f, 154.015f, 158.083f, 154.015f, 147.59f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(160f, 112.475f)
                lineTo(160f, 118.175f)
                lineTo(100.85f, 150.025f)
                lineTo(100.85f, 144.325f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(155.092f, 147.107f)
                curveTo(155.092f, 137.401f, 160.476f, 126.633f, 167.117f, 123.057f)
                curveTo(173.758f, 119.481f, 179.142f, 124.451f, 179.142f, 134.157f)
                curveTo(179.142f, 143.863f, 173.758f, 154.631f, 167.117f, 158.207f)
                curveTo(160.476f, 161.783f, 155.092f, 156.813f, 155.092f, 147.107f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(156.819f, 146.274f)
                curveTo(156.819f, 137.879f, 161.475f, 128.567f, 167.219f, 125.474f)
                curveTo(172.963f, 122.381f, 177.619f, 126.679f, 177.619f, 135.074f)
                curveTo(177.619f, 143.469f, 172.963f, 152.781f, 167.219f, 155.874f)
                curveTo(161.475f, 158.967f, 156.819f, 154.669f, 156.819f, 146.274f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(165.371f, 141.766f)
                curveTo(165.371f, 140.192f, 166.244f, 138.446f, 167.321f, 137.866f)
                curveTo(168.398f, 137.286f, 169.271f, 138.092f, 169.271f, 139.666f)
                curveTo(169.271f, 141.24f, 168.398f, 142.986f, 167.321f, 143.566f)
                curveTo(166.244f, 144.146f, 165.371f, 143.34f, 165.371f, 141.766f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(168.7835f, 137.791f)
                lineTo(175.363f, 140.5f)
                lineTo(172.438f, 142.075f)
                lineTo(165.8585f, 139.366f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(165.8585f, 139.366f)
                lineTo(172.438f, 142.075f)
                lineTo(172.438f, 146.35f)
                lineTo(165.8585f, 143.641f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(52.013f, 119.75f)
                lineTo(53.288f, 120.275f)
                lineTo(53.288f, 121.7f)
                lineTo(52.013f, 121.175f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(6.025f, 137.225f)
                lineTo(10.575f, 134.775f)
                lineTo(98.975f, 171.175f)
                lineTo(94.425f, 173.625f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(157.5f, 37.725f)
                lineTo(162.6f, 39.825f)
                lineTo(158.7f, 41.925f)
                lineTo(153.6f, 39.825f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(122.46f, 124.233f)
                lineTo(121.94f, 124.513f)
                lineTo(121.94f, 123.847f)
                lineTo(122.46f, 123.567f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(6.025f, 143.875f)
                lineTo(6.025f, 137.225f)
                lineTo(94.425f, 173.625f)
                lineTo(94.425f, 180.275f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(88.84f, 118.248f)
                lineTo(89.36f, 117.968f)
                lineTo(114.01f, 128.118f)
                lineTo(113.49f, 128.398f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(88.84f, 118.912f)
                lineTo(88.84f, 118.248f)
                lineTo(113.49f, 128.398f)
                lineTo(113.49f, 129.062f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(175.363f, 144.775f)
                lineTo(172.438f, 146.35f)
                lineTo(172.438f, 142.075f)
                lineTo(175.363f, 140.5f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(168.025f, 140.175f)
                lineTo(171.275f, 138.425f)
                lineTo(177.225f, 140.875f)
                lineTo(173.975f, 142.625f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(168.025f, 144.925f)
                lineTo(168.025f, 140.175f)
                lineTo(173.975f, 142.625f)
                lineTo(173.975f, 147.375f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(177.225f, 145.625f)
                lineTo(173.975f, 147.375f)
                lineTo(173.975f, 142.625f)
                lineTo(177.225f, 140.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(109.681f, 75.293f)
                lineTo(109.575f, 75.992f)
                lineTo(109.257f, 76.625f)
                lineTo(108.731f, 77.188f)
                lineTo(108.001f, 77.674f)
                lineTo(107.075f, 78.08f)
                lineTo(105.961f, 78.401f)
                lineTo(104.671f, 78.634f)
                lineTo(103.216f, 78.777f)
                lineTo(101.61f, 78.828f)
                lineTo(99.87f, 78.787f)
                lineTo(98.011f, 78.655f)
                lineTo(96.053f, 78.432f)
                lineTo(94.013f, 78.122f)
                lineTo(91.911f, 77.726f)
                lineTo(89.767f, 77.248f)
                lineTo(87.603f, 76.694f)
                lineTo(85.439f, 76.068f)
                lineTo(83.295f, 75.377f)
                lineTo(81.193f, 74.628f)
                lineTo(79.153f, 73.826f)
                lineTo(77.194f, 72.981f)
                lineTo(75.335f, 72.1f)
                lineTo(73.594f, 71.192f)
                lineTo(71.989f, 70.265f)
                lineTo(70.533f, 69.329f)
                lineTo(69.242f, 68.393f)
                lineTo(68.128f, 67.465f)
                lineTo(67.201f, 66.554f)
                lineTo(66.471f, 65.669f)
                lineTo(65.944f, 64.82f)
                lineTo(65.626f, 64.013f)
                lineTo(65.519f, 63.257f)
                lineTo(65.519f, 53.757f)
                lineTo(65.626f, 54.513f)
                lineTo(65.944f, 55.32f)
                lineTo(66.471f, 56.169f)
                lineTo(67.201f, 57.054f)
                lineTo(68.128f, 57.965f)
                lineTo(69.242f, 58.893f)
                lineTo(70.533f, 59.829f)
                lineTo(71.989f, 60.765f)
                lineTo(73.594f, 61.692f)
                lineTo(75.335f, 62.6f)
                lineTo(77.194f, 63.481f)
                lineTo(79.153f, 64.326f)
                lineTo(81.193f, 65.128f)
                lineTo(83.295f, 65.877f)
                lineTo(85.439f, 66.568f)
                lineTo(87.603f, 67.194f)
                lineTo(89.767f, 67.748f)
                lineTo(91.911f, 68.226f)
                lineTo(94.013f, 68.622f)
                lineTo(96.053f, 68.932f)
                lineTo(98.011f, 69.155f)
                lineTo(99.87f, 69.287f)
                lineTo(101.61f, 69.328f)
                lineTo(103.216f, 69.277f)
                lineTo(104.671f, 69.134f)
                lineTo(105.961f, 68.901f)
                lineTo(107.075f, 68.58f)
                lineTo(108.001f, 68.174f)
                lineTo(108.731f, 67.688f)
                lineTo(109.257f, 67.125f)
                lineTo(109.575f, 66.492f)
                lineTo(109.681f, 65.793f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(16.45f, 111.425f)
                lineTo(20.35f, 109.325f)
                lineTo(100.25f, 142.225f)
                lineTo(96.35f, 144.325f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(16.45f, 117.125f)
                lineTo(16.45f, 111.425f)
                lineTo(96.35f, 144.325f)
                lineTo(96.35f, 150.025f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(101.855f, 76.458f)
                lineTo(102.765f, 76.012f)
                lineTo(105.145f, 76.992f)
                lineTo(104.235f, 77.438f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N600),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(105.325f, 77.6f)
                curveTo(105.325f, 76.393f, 104.45f, 75.055f, 103.37f, 74.61f)
                curveTo(102.29f, 74.165f, 101.415f, 74.783f, 101.415f, 75.99f)
                curveTo(101.415f, 77.197f, 102.29f, 78.535f, 103.37f, 78.98f)
                curveTo(104.45f, 79.425f, 105.325f, 78.807f, 105.325f, 77.6f)
                close()
                moveTo(104.305f, 77.18f)
                curveTo(104.305f, 76.603f, 103.886f, 75.963f, 103.37f, 75.75f)
                curveTo(102.854f, 75.537f, 102.435f, 75.833f, 102.435f, 76.41f)
                curveTo(102.435f, 76.987f, 102.854f, 77.627f, 103.37f, 77.84f)
                curveTo(103.886f, 78.053f, 104.305f, 77.757f, 104.305f, 77.18f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(67.312f, 126.05f)
                lineTo(68.588f, 126.575f)
                lineTo(68.588f, 128f)
                lineTo(67.312f, 127.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(114.01f, 128.782f)
                lineTo(113.49f, 129.062f)
                lineTo(113.49f, 128.398f)
                lineTo(114.01f, 128.118f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(80.39f, 122.797f)
                lineTo(80.91f, 122.517f)
                lineTo(105.56f, 132.668f)
                lineTo(105.04f, 132.947f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(80.39f, 123.463f)
                lineTo(80.39f, 122.797f)
                lineTo(105.04f, 132.947f)
                lineTo(105.04f, 133.613f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(105.56f, 133.333f)
                lineTo(105.04f, 133.613f)
                lineTo(105.04f, 132.947f)
                lineTo(105.56f, 132.668f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(156.188f, 36.9f)
                lineTo(160.012f, 38.475f)
                lineTo(127.513f, 55.975f)
                lineTo(123.688f, 54.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(160.012f, 38.475f)
                lineTo(160.012f, 42.75f)
                lineTo(127.513f, 60.25f)
                lineTo(127.513f, 55.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(66.65f, 127.25f)
                lineTo(99.8f, 140.9f)
                lineTo(99.8f, 145.65f)
                lineTo(66.65f, 132f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(85.6f, 176.95f)
                lineTo(90.8f, 174.15f)
                lineTo(101f, 178.35f)
                lineTo(95.8f, 181.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(89.475f, 169.425f)
                lineTo(95.425f, 171.875f)
                lineTo(95.425f, 178.525f)
                lineTo(89.475f, 176.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(85.6f, 179.8f)
                lineTo(85.6f, 176.95f)
                lineTo(95.8f, 181.15f)
                lineTo(95.8f, 184f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(103.45f, 168.575f)
                lineTo(99.55f, 170.675f)
                lineTo(99.55f, 145.975f)
                lineTo(103.45f, 143.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(101f, 181.2f)
                lineTo(95.8f, 184f)
                lineTo(95.8f, 181.15f)
                lineTo(101f, 178.35f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(99.55f, 170.675f)
                lineTo(94.45f, 168.575f)
                lineTo(94.45f, 143.875f)
                lineTo(99.55f, 145.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(98.975f, 177.825f)
                lineTo(94.425f, 180.275f)
                lineTo(94.425f, 173.625f)
                lineTo(98.975f, 171.175f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(95.75f, 142.225f)
                lineTo(100.85f, 144.325f)
                lineTo(100.85f, 150.025f)
                lineTo(95.75f, 147.925f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(100.25f, 147.925f)
                lineTo(96.35f, 150.025f)
                lineTo(96.35f, 144.325f)
                lineTo(100.25f, 142.225f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(98.35f, 141.775f)
                lineTo(103.45f, 143.875f)
                lineTo(99.55f, 145.975f)
                lineTo(94.45f, 143.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(136.135f, 46.555f)
                lineTo(141.065f, 48.585f)
                lineTo(126.765f, 56.285f)
                lineTo(121.835f, 54.255f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(141.065f, 48.585f)
                lineTo(141.065f, 54.095f)
                lineTo(126.765f, 61.795f)
                lineTo(126.765f, 56.285f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(123.688f, 54.4f)
                lineTo(127.513f, 55.975f)
                lineTo(127.513f, 60.25f)
                lineTo(123.688f, 58.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(121.835f, 54.255f)
                lineTo(126.765f, 56.285f)
                lineTo(126.765f, 61.795f)
                lineTo(121.835f, 59.765f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(121.015f, 54.345f)
                lineTo(126.285f, 56.515f)
                lineTo(126.285f, 62.405f)
                lineTo(121.015f, 60.235f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(122.315f, 53.645f)
                lineTo(127.585f, 55.815f)
                lineTo(126.285f, 56.515f)
                lineTo(121.015f, 54.345f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(127.585f, 55.815f)
                lineTo(127.585f, 61.705f)
                lineTo(126.285f, 62.405f)
                lineTo(126.285f, 56.515f)
                close()
            }
        }.build()
        return _BeltSquat!!
    }
