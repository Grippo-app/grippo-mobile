package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _PlateLoadedRow: ImageVector? = null

public val PlateLoadedRow: ImageVector
    get() {
        if (_PlateLoadedRow != null) {
            return _PlateLoadedRow!!
        }
        _PlateLoadedRow = ImageVector.Builder(
            name = "PlateLoadedRow",
            defaultWidth = 179.dp,
            defaultHeight = 175.dp,
            viewportWidth = 179f,
            viewportHeight = 175f,
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(79.4f, 92.25f)
                lineTo(83.95f, 89.8f)
                lineTo(94.15f, 94f)
                lineTo(89.6f, 96.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(79.4f, 95.1f)
                lineTo(79.4f, 92.25f)
                lineTo(89.6f, 96.45f)
                lineTo(89.6f, 99.3f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(94.15f, 96.85f)
                lineTo(89.6f, 99.3f)
                lineTo(89.6f, 96.45f)
                lineTo(94.15f, 94f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(81.95f, 86.65f)
                lineTo(86.5f, 84.2f)
                lineTo(166.4f, 117.1f)
                lineTo(161.85f, 119.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(81.95f, 93.3f)
                lineTo(81.95f, 86.65f)
                lineTo(161.85f, 119.55f)
                lineTo(161.85f, 126.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(82.559f, 93.388f)
                lineTo(78.309f, 91.638f)
                lineTo(61.409f, 64.638f)
                lineTo(65.659f, 66.388f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(85.241f, 89.262f)
                lineTo(82.559f, 93.388f)
                lineTo(65.659f, 66.388f)
                lineTo(68.341f, 62.262f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(87.025f, 91.4f)
                lineTo(82.475f, 93.85f)
                lineTo(82.475f, 10.25f)
                lineTo(87.025f, 7.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(82.475f, 93.85f)
                lineTo(76.525f, 91.4f)
                lineTo(76.525f, 7.8f)
                lineTo(82.475f, 10.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(36.95f, 24.9f)
                lineTo(33.7f, 26.65f)
                lineTo(28.6f, 24.55f)
                lineTo(31.85f, 22.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(36.95f, 24.9f)
                lineTo(36.95f, 29.65f)
                lineTo(33.7f, 31.4f)
                lineTo(33.7f, 26.65f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(33.7f, 26.65f)
                lineTo(33.7f, 31.4f)
                lineTo(28.6f, 29.3f)
                lineTo(28.6f, 24.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(27.438f, 36.638f)
                curveTo(27.438f, 26.669f, 32.967f, 15.61f, 39.788f, 11.938f)
                curveTo(46.608f, 8.265f, 52.138f, 13.369f, 52.138f, 23.338f)
                curveTo(52.138f, 33.306f, 46.608f, 44.365f, 39.788f, 48.038f)
                curveTo(32.967f, 51.71f, 27.438f, 46.606f, 27.438f, 36.638f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(85.5f, 85.95f)
                lineTo(91.45f, 88.4f)
                lineTo(16.05f, 129f)
                lineTo(10.1f, 126.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(46.519f, 41.496f)
                lineTo(45.11f, 43.409f)
                lineTo(43.61f, 45.093f)
                lineTo(42.044f, 46.518f)
                lineTo(40.44f, 47.661f)
                lineTo(38.825f, 48.501f)
                lineTo(37.226f, 49.024f)
                lineTo(35.671f, 49.222f)
                lineTo(34.186f, 49.09f)
                lineTo(32.798f, 48.632f)
                lineTo(31.529f, 47.855f)
                lineTo(30.401f, 46.772f)
                lineTo(29.433f, 45.401f)
                lineTo(28.643f, 43.767f)
                lineTo(28.044f, 41.898f)
                lineTo(27.645f, 39.824f)
                lineTo(27.455f, 37.582f)
                lineTo(27.475f, 35.211f)
                lineTo(27.706f, 32.749f)
                lineTo(28.144f, 30.241f)
                lineTo(28.781f, 27.728f)
                lineTo(29.606f, 25.254f)
                lineTo(30.605f, 22.861f)
                lineTo(31.762f, 20.59f)
                lineTo(33.056f, 18.479f)
                lineTo(37.731f, 20.404f)
                lineTo(36.437f, 22.515f)
                lineTo(35.28f, 24.786f)
                lineTo(34.281f, 27.179f)
                lineTo(33.456f, 29.653f)
                lineTo(32.819f, 32.166f)
                lineTo(32.381f, 34.674f)
                lineTo(32.15f, 37.136f)
                lineTo(32.13f, 39.507f)
                lineTo(32.32f, 41.749f)
                lineTo(32.719f, 43.823f)
                lineTo(33.318f, 45.692f)
                lineTo(34.108f, 47.326f)
                lineTo(35.076f, 48.697f)
                lineTo(36.204f, 49.78f)
                lineTo(37.473f, 50.557f)
                lineTo(38.861f, 51.015f)
                lineTo(40.346f, 51.147f)
                lineTo(41.901f, 50.949f)
                lineTo(43.5f, 50.426f)
                lineTo(45.115f, 49.586f)
                lineTo(46.719f, 48.443f)
                lineTo(48.285f, 47.018f)
                lineTo(49.785f, 45.334f)
                lineTo(51.194f, 43.421f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(32.112f, 38.563f)
                curveTo(32.112f, 28.594f, 37.642f, 17.535f, 44.462f, 13.863f)
                curveTo(51.283f, 10.19f, 56.812f, 15.294f, 56.812f, 25.263f)
                curveTo(56.812f, 35.231f, 51.283f, 46.29f, 44.463f, 49.963f)
                curveTo(37.642f, 53.635f, 32.113f, 48.531f, 32.112f, 38.563f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(79.825f, 65.85f)
                lineTo(83.725f, 63.75f)
                lineTo(156.825f, 93.85f)
                lineTo(152.925f, 95.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(33.255f, 38.045f)
                curveTo(33.255f, 28.915f, 38.318f, 18.788f, 44.565f, 15.425f)
                curveTo(50.811f, 12.061f, 55.875f, 16.735f, 55.875f, 25.865f)
                curveTo(55.875f, 34.994f, 50.811f, 45.121f, 44.565f, 48.485f)
                curveTo(38.318f, 51.848f, 33.255f, 47.174f, 33.255f, 38.045f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(91.45f, 88.4f)
                lineTo(91.45f, 95.05f)
                lineTo(16.05f, 135.65f)
                lineTo(16.05f, 129f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(34.916f, 37.247f)
                curveTo(34.916f, 29.376f, 39.282f, 20.646f, 44.666f, 17.747f)
                curveTo(50.051f, 14.847f, 54.416f, 18.876f, 54.416f, 26.747f)
                curveTo(54.416f, 34.617f, 50.051f, 43.347f, 44.666f, 46.247f)
                curveTo(39.282f, 49.146f, 34.916f, 45.117f, 34.916f, 37.247f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(64.091f, 60.512f)
                lineTo(68.341f, 62.262f)
                lineTo(65.659f, 66.388f)
                lineTo(61.409f, 64.638f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(42.819f, 33.089f)
                curveTo(42.819f, 31.514f, 43.692f, 29.768f, 44.769f, 29.189f)
                curveTo(45.845f, 28.609f, 46.719f, 29.414f, 46.719f, 30.989f)
                curveTo(46.719f, 32.563f, 45.845f, 34.309f, 44.769f, 34.889f)
                curveTo(43.692f, 35.468f, 42.819f, 34.663f, 42.819f, 33.089f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(33.387f, 39.088f)
                curveTo(33.387f, 29.119f, 38.917f, 18.06f, 45.737f, 14.388f)
                curveTo(52.558f, 10.715f, 58.087f, 15.819f, 58.087f, 25.788f)
                curveTo(58.087f, 35.756f, 52.558f, 46.815f, 45.737f, 50.488f)
                curveTo(38.917f, 54.16f, 33.388f, 49.056f, 33.387f, 39.088f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(79.825f, 71.55f)
                lineTo(79.825f, 65.85f)
                lineTo(152.925f, 95.95f)
                lineTo(152.925f, 101.65f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(154.2f, 123.05f)
                lineTo(158.75f, 120.6f)
                lineTo(168.95f, 124.8f)
                lineTo(164.4f, 127.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(64.838f, 36.775f)
                lineTo(61.913f, 38.35f)
                lineTo(29.612f, 25.05f)
                lineTo(32.537f, 23.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(61.913f, 38.35f)
                lineTo(61.913f, 42.625f)
                lineTo(29.612f, 29.325f)
                lineTo(29.612f, 25.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(52.469f, 43.946f)
                lineTo(51.06f, 45.859f)
                lineTo(49.56f, 47.543f)
                lineTo(47.994f, 48.968f)
                lineTo(46.39f, 50.111f)
                lineTo(44.775f, 50.951f)
                lineTo(43.176f, 51.474f)
                lineTo(41.621f, 51.672f)
                lineTo(40.136f, 51.54f)
                lineTo(38.748f, 51.082f)
                lineTo(37.479f, 50.305f)
                lineTo(36.351f, 49.222f)
                lineTo(35.383f, 47.851f)
                lineTo(34.593f, 46.217f)
                lineTo(33.994f, 44.348f)
                lineTo(33.595f, 42.274f)
                lineTo(33.405f, 40.032f)
                lineTo(33.425f, 37.661f)
                lineTo(33.656f, 35.199f)
                lineTo(34.094f, 32.691f)
                lineTo(34.731f, 30.178f)
                lineTo(35.556f, 27.704f)
                lineTo(36.555f, 25.311f)
                lineTo(37.712f, 23.04f)
                lineTo(39.006f, 20.929f)
                lineTo(43.681f, 22.854f)
                lineTo(42.387f, 24.965f)
                lineTo(41.23f, 27.236f)
                lineTo(40.231f, 29.629f)
                lineTo(39.406f, 32.103f)
                lineTo(38.769f, 34.616f)
                lineTo(38.331f, 37.124f)
                lineTo(38.1f, 39.586f)
                lineTo(38.08f, 41.957f)
                lineTo(38.27f, 44.199f)
                lineTo(38.669f, 46.273f)
                lineTo(39.268f, 48.142f)
                lineTo(40.058f, 49.776f)
                lineTo(41.026f, 51.147f)
                lineTo(42.154f, 52.23f)
                lineTo(43.423f, 53.007f)
                lineTo(44.811f, 53.465f)
                lineTo(46.296f, 53.597f)
                lineTo(47.851f, 53.399f)
                lineTo(49.45f, 52.876f)
                lineTo(51.065f, 52.036f)
                lineTo(52.669f, 50.893f)
                lineTo(54.235f, 49.468f)
                lineTo(55.735f, 47.784f)
                lineTo(57.144f, 45.871f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(38.062f, 41.013f)
                curveTo(38.062f, 31.044f, 43.592f, 19.985f, 50.413f, 16.313f)
                curveTo(57.233f, 12.64f, 62.763f, 17.744f, 62.763f, 27.713f)
                curveTo(62.763f, 37.681f, 57.233f, 48.74f, 50.413f, 52.413f)
                curveTo(43.592f, 56.085f, 38.063f, 50.981f, 38.062f, 41.013f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(39.205f, 40.495f)
                curveTo(39.205f, 31.365f, 44.268f, 21.238f, 50.514f, 17.875f)
                curveTo(56.761f, 14.511f, 61.825f, 19.185f, 61.825f, 28.315f)
                curveTo(61.825f, 37.444f, 56.761f, 47.571f, 50.514f, 50.935f)
                curveTo(44.268f, 54.298f, 39.205f, 49.624f, 39.205f, 40.495f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(154.2f, 125.9f)
                lineTo(154.2f, 123.05f)
                lineTo(164.4f, 127.25f)
                lineTo(164.4f, 130.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(40.867f, 39.697f)
                curveTo(40.867f, 31.826f, 45.232f, 23.096f, 50.617f, 20.197f)
                curveTo(56.001f, 17.297f, 60.367f, 21.326f, 60.367f, 29.197f)
                curveTo(60.367f, 37.067f, 56.001f, 45.797f, 50.617f, 48.697f)
                curveTo(45.232f, 51.596f, 40.867f, 47.567f, 40.867f, 39.697f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(48.769f, 35.539f)
                curveTo(48.769f, 33.964f, 49.642f, 32.218f, 50.718f, 31.639f)
                curveTo(51.795f, 31.059f, 52.669f, 31.864f, 52.669f, 33.439f)
                curveTo(52.669f, 35.013f, 51.795f, 36.759f, 50.718f, 37.339f)
                curveTo(49.642f, 37.918f, 48.769f, 37.113f, 48.769f, 35.539f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(52.1805f, 31.564f)
                lineTo(64.838f, 36.775f)
                lineTo(61.913f, 38.35f)
                lineTo(49.2555f, 33.139f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(49.2555f, 33.139f)
                lineTo(61.913f, 38.35f)
                lineTo(61.913f, 42.625f)
                lineTo(49.2555f, 37.414f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(81.075f, 5.35f)
                lineTo(87.025f, 7.8f)
                lineTo(82.475f, 10.25f)
                lineTo(76.525f, 7.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(168.95f, 127.65f)
                lineTo(164.4f, 130.1f)
                lineTo(164.4f, 127.25f)
                lineTo(168.95f, 124.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(85.3f, 7.85f)
                lineTo(80.75f, 10.3f)
                lineTo(71.4f, 6.45f)
                lineTo(75.95f, 4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(166.4f, 123.75f)
                lineTo(161.85f, 126.2f)
                lineTo(161.85f, 119.55f)
                lineTo(166.4f, 117.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(80.75f, 10.3f)
                lineTo(80.75f, 16.95f)
                lineTo(71.4f, 13.1f)
                lineTo(71.4f, 6.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(85.3f, 7.85f)
                lineTo(85.3f, 14.5f)
                lineTo(80.75f, 16.95f)
                lineTo(80.75f, 10.3f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(78.8f, 14.675f)
                curveTo(78.8f, 11.265f, 80.692f, 7.481f, 83.025f, 6.225f)
                curveTo(85.358f, 4.969f, 87.25f, 6.715f, 87.25f, 10.125f)
                curveTo(87.25f, 13.535f, 85.358f, 17.319f, 83.025f, 18.575f)
                curveTo(80.692f, 19.831f, 78.8f, 18.085f, 78.8f, 14.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(80.985f, 13.66f)
                curveTo(80.985f, 11.876f, 81.974f, 9.897f, 83.195f, 9.24f)
                curveTo(84.416f, 8.583f, 85.405f, 9.496f, 85.405f, 11.28f)
                curveTo(85.405f, 13.064f, 84.416f, 15.043f, 83.195f, 15.7f)
                curveTo(81.974f, 16.357f, 80.985f, 15.444f, 80.985f, 13.66f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(63.203f, 115.905f)
                lineTo(58.103f, 113.805f)
                lineTo(55.503f, 109.505f)
                lineTo(60.603f, 111.605f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(66.447f, 110.995f)
                lineTo(63.203f, 115.905f)
                lineTo(60.603f, 111.605f)
                lineTo(63.847f, 106.695f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(55.756f, 101.516f)
                lineTo(56.244f, 101.254f)
                lineTo(68.144f, 106.154f)
                lineTo(67.656f, 106.416f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(55.756f, 102.276f)
                lineTo(55.756f, 101.516f)
                lineTo(67.656f, 106.416f)
                lineTo(67.656f, 107.176f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(58.747f, 104.595f)
                lineTo(63.847f, 106.695f)
                lineTo(60.603f, 111.605f)
                lineTo(55.503f, 109.505f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(79.496f, 11.477f)
                lineTo(82.254f, 14.023f)
                lineTo(66.654f, 45.223f)
                lineTo(63.896f, 42.677f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(74.396f, 9.377f)
                lineTo(79.496f, 11.477f)
                lineTo(63.896f, 42.677f)
                lineTo(58.796f, 40.577f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(48.125f, 104.675f)
                lineTo(55.925f, 100.475f)
                lineTo(71.225f, 106.775f)
                lineTo(63.425f, 110.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(58.796f, 40.577f)
                lineTo(63.896f, 42.677f)
                lineTo(66.654f, 45.223f)
                lineTo(61.554f, 43.123f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(53.481f, 102.741f)
                lineTo(53.969f, 102.479f)
                lineTo(65.869f, 107.379f)
                lineTo(65.381f, 107.641f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(53.481f, 103.501f)
                lineTo(53.481f, 102.741f)
                lineTo(65.381f, 107.641f)
                lineTo(65.381f, 108.401f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(64.838f, 36.775f)
                lineTo(64.838f, 41.05f)
                lineTo(61.913f, 42.625f)
                lineTo(61.913f, 38.35f)
                close()
            }

            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(51.206f, 103.966f)
                lineTo(51.694f, 103.704f)
                lineTo(63.594f, 108.604f)
                lineTo(63.106f, 108.866f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(51.206f, 104.726f)
                lineTo(51.206f, 103.966f)
                lineTo(63.106f, 108.866f)
                lineTo(63.106f, 109.626f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(48.125f, 107.525f)
                lineTo(48.125f, 104.675f)
                lineTo(63.425f, 110.975f)
                lineTo(63.425f, 113.825f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(65.869f, 108.139f)
                lineTo(65.381f, 108.401f)
                lineTo(65.381f, 107.641f)
                lineTo(65.869f, 107.379f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(71.225f, 109.625f)
                lineTo(63.425f, 113.825f)
                lineTo(63.425f, 110.975f)
                lineTo(71.225f, 106.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(63.594f, 109.364f)
                lineTo(63.106f, 109.626f)
                lineTo(63.106f, 108.866f)
                lineTo(63.594f, 108.604f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(50.125f, 108.3f)
                lineTo(54.025f, 106.2f)
                lineTo(125.425f, 135.6f)
                lineTo(121.525f, 137.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(50.125f, 114f)
                lineTo(50.125f, 108.3f)
                lineTo(121.525f, 137.7f)
                lineTo(121.525f, 143.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(156.825f, 99.55f)
                lineTo(152.925f, 101.65f)
                lineTo(152.925f, 95.95f)
                lineTo(156.825f, 93.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(60.451f, 38.88f)
                lineTo(65.551f, 40.98f)
                lineTo(52.451f, 49.08f)
                lineTo(47.351f, 46.98f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(65.551f, 40.98f)
                lineTo(64.999f, 46.92f)
                lineTo(51.899f, 55.02f)
                lineTo(52.451f, 49.08f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(79.5f, 7.6f)
                lineTo(84.05f, 5.15f)
                lineTo(157.15f, 35.25f)
                lineTo(152.6f, 37.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(79.5f, 14.25f)
                lineTo(79.5f, 7.6f)
                lineTo(152.6f, 37.7f)
                lineTo(152.6f, 44.35f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(155.659f, 123.488f)
                lineTo(151.409f, 121.738f)
                lineTo(134.509f, 94.738f)
                lineTo(138.759f, 96.488f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(158.341f, 119.362f)
                lineTo(155.659f, 123.488f)
                lineTo(138.759f, 96.488f)
                lineTo(141.441f, 92.362f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(160.125f, 121.5f)
                lineTo(155.575f, 123.95f)
                lineTo(155.575f, 40.35f)
                lineTo(160.125f, 37.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(155.575f, 123.95f)
                lineTo(149.625f, 121.5f)
                lineTo(149.625f, 37.9f)
                lineTo(155.575f, 40.35f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(47.351f, 46.98f)
                lineTo(52.451f, 49.08f)
                lineTo(51.899f, 55.02f)
                lineTo(46.799f, 52.92f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(4f, 132.85f)
                lineTo(8.55f, 130.4f)
                lineTo(18.75f, 134.6f)
                lineTo(14.2f, 137.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(4f, 135.7f)
                lineTo(4f, 132.85f)
                lineTo(14.2f, 137.05f)
                lineTo(14.2f, 139.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(18.75f, 137.45f)
                lineTo(14.2f, 139.9f)
                lineTo(14.2f, 137.05f)
                lineTo(18.75f, 134.6f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(10.1f, 126.55f)
                lineTo(16.05f, 129f)
                lineTo(16.05f, 135.65f)
                lineTo(10.1f, 133.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(114.203f, 136.905f)
                lineTo(109.103f, 134.805f)
                lineTo(106.503f, 130.505f)
                lineTo(111.603f, 132.605f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(54.325f, 93.55f)
                lineTo(60.125f, 92.85f)
                lineTo(60.125f, 88.1f)
                lineTo(54.325f, 88.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(117.447f, 131.995f)
                lineTo(114.203f, 136.905f)
                lineTo(111.603f, 132.605f)
                lineTo(114.847f, 127.695f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(106.756f, 122.516f)
                lineTo(107.244f, 122.254f)
                lineTo(119.144f, 127.154f)
                lineTo(118.656f, 127.416f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(106.756f, 123.276f)
                lineTo(106.756f, 122.516f)
                lineTo(118.656f, 127.416f)
                lineTo(118.656f, 128.176f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(48.137f, 49.734f)
                lineTo(50.737f, 48.334f)
                lineTo(54.487f, 52.634f)
                lineTo(51.887f, 54.034f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(125.425f, 141.3f)
                lineTo(121.525f, 143.4f)
                lineTo(121.525f, 137.7f)
                lineTo(125.425f, 135.6f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(109.747f, 125.595f)
                lineTo(114.847f, 127.695f)
                lineTo(111.603f, 132.605f)
                lineTo(106.503f, 130.505f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(48.513f, 53.666f)
                lineTo(48.137f, 49.734f)
                lineTo(51.887f, 54.034f)
                lineTo(52.263f, 57.966f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(52.945f, 59.21f)
                lineTo(56.6f, 60.715f)
                lineTo(53.805f, 62.22f)
                lineTo(50.15f, 60.715f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(69.875f, 57.2f)
                lineTo(74.375f, 61.95f)
                lineTo(71.125f, 63.7f)
                lineTo(66.625f, 58.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(99.125f, 125.675f)
                lineTo(106.925f, 121.475f)
                lineTo(122.225f, 127.775f)
                lineTo(114.425f, 131.975f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(104.481f, 123.741f)
                lineTo(104.969f, 123.479f)
                lineTo(116.869f, 128.379f)
                lineTo(116.381f, 128.641f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(104.481f, 124.501f)
                lineTo(104.481f, 123.741f)
                lineTo(116.381f, 128.641f)
                lineTo(116.381f, 129.401f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(56.6f, 61.95f)
                lineTo(53.805f, 63.455f)
                lineTo(53.805f, 62.22f)
                lineTo(56.6f, 60.715f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(53.805f, 63.455f)
                lineTo(50.15f, 61.95f)
                lineTo(50.15f, 60.715f)
                lineTo(53.805f, 62.22f)
                close()
            }

            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(156.9f, 115.35f)
                lineTo(162.85f, 117.8f)
                lineTo(87.45f, 158.4f)
                lineTo(81.5f, 155.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(162.85f, 117.8f)
                lineTo(162.85f, 124.45f)
                lineTo(87.45f, 165.05f)
                lineTo(87.45f, 158.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(54.863f, 56.566f)
                lineTo(52.263f, 57.966f)
                lineTo(51.887f, 54.034f)
                lineTo(54.487f, 52.634f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(102.206f, 124.966f)
                lineTo(102.694f, 124.704f)
                lineTo(114.594f, 129.604f)
                lineTo(114.106f, 129.866f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(102.206f, 125.726f)
                lineTo(102.206f, 124.966f)
                lineTo(114.106f, 129.866f)
                lineTo(114.106f, 130.626f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(137.191f, 90.612f)
                lineTo(141.441f, 92.362f)
                lineTo(138.759f, 96.488f)
                lineTo(134.509f, 94.738f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(99.125f, 128.525f)
                lineTo(99.125f, 125.675f)
                lineTo(114.425f, 131.975f)
                lineTo(114.425f, 134.825f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(55.925f, 61f)
                lineTo(53.715f, 62.19f)
                lineTo(53.715f, 47.94f)
                lineTo(55.925f, 46.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(116.869f, 129.139f)
                lineTo(116.381f, 129.401f)
                lineTo(116.381f, 128.641f)
                lineTo(116.869f, 128.379f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(122.225f, 130.625f)
                lineTo(114.425f, 134.825f)
                lineTo(114.425f, 131.975f)
                lineTo(122.225f, 127.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(53.715f, 62.19f)
                lineTo(50.825f, 61f)
                lineTo(50.825f, 46.75f)
                lineTo(53.715f, 47.94f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(60.125f, 92.85f)
                lineTo(89.025f, 104.75f)
                lineTo(89.025f, 100f)
                lineTo(60.125f, 88.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(114.594f, 130.364f)
                lineTo(114.106f, 130.626f)
                lineTo(114.106f, 129.866f)
                lineTo(114.594f, 129.604f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(53.035f, 45.56f)
                lineTo(55.925f, 46.75f)
                lineTo(53.715f, 47.94f)
                lineTo(50.825f, 46.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(157.15f, 41.9f)
                lineTo(152.6f, 44.35f)
                lineTo(152.6f, 37.7f)
                lineTo(157.15f, 35.25f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(74.375f, 61.95f)
                lineTo(99.875f, 72.45f)
                lineTo(96.625f, 74.2f)
                lineTo(71.125f, 63.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(39.375f, 101.6f)
                lineTo(54.325f, 93.55f)
                lineTo(54.325f, 88.8f)
                lineTo(39.375f, 96.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(84.807f, 66.368f)
                lineTo(90.757f, 68.818f)
                lineTo(86.843f, 67.532f)
                lineTo(80.893f, 65.082f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(52.945f, 44.295f)
                lineTo(56.6f, 45.8f)
                lineTo(53.805f, 47.305f)
                lineTo(50.15f, 45.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(68.007f, 137.118f)
                lineTo(64.093f, 135.832f)
                lineTo(86.843f, 67.532f)
                lineTo(90.757f, 68.818f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(154.175f, 35.45f)
                lineTo(160.125f, 37.9f)
                lineTo(155.575f, 40.35f)
                lineTo(149.625f, 37.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(56.6f, 47.13f)
                lineTo(53.805f, 48.635f)
                lineTo(53.805f, 47.305f)
                lineTo(56.6f, 45.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(53.805f, 48.635f)
                lineTo(50.15f, 47.13f)
                lineTo(50.15f, 45.8f)
                lineTo(53.805f, 47.305f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(64.093f, 135.832f)
                lineTo(58.143f, 133.382f)
                lineTo(80.893f, 65.082f)
                lineTo(86.843f, 67.532f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(63.375f, 38.85f)
                lineTo(69.875f, 57.2f)
                lineTo(66.625f, 58.95f)
                lineTo(60.125f, 40.6f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(153.6f, 45.475f)
                curveTo(153.6f, 42.065f, 155.492f, 38.281f, 157.825f, 37.025f)
                curveTo(160.158f, 35.769f, 162.05f, 37.515f, 162.05f, 40.925f)
                curveTo(162.05f, 44.335f, 160.158f, 48.119f, 157.825f, 49.375f)
                curveTo(155.492f, 50.631f, 153.6f, 48.885f, 153.6f, 45.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(155.785f, 44.46f)
                curveTo(155.785f, 42.676f, 156.774f, 40.697f, 157.995f, 40.04f)
                curveTo(159.216f, 39.383f, 160.205f, 40.296f, 160.205f, 42.08f)
                curveTo(160.205f, 43.864f, 159.216f, 45.843f, 157.995f, 46.5f)
                curveTo(156.774f, 47.157f, 155.785f, 46.244f, 155.785f, 44.46f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(148.75f, 38.3f)
                lineTo(153.3f, 35.85f)
                lineTo(162.65f, 39.7f)
                lineTo(158.1f, 42.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(70.925f, 134.8f)
                lineTo(66.375f, 137.25f)
                lineTo(66.375f, 105.9f)
                lineTo(70.925f, 103.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(66.375f, 137.25f)
                lineTo(60.425f, 134.8f)
                lineTo(60.425f, 103.45f)
                lineTo(66.375f, 105.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(148.75f, 44.95f)
                lineTo(148.75f, 38.3f)
                lineTo(158.1f, 42.15f)
                lineTo(158.1f, 48.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(162.65f, 46.35f)
                lineTo(158.1f, 48.8f)
                lineTo(158.1f, 42.15f)
                lineTo(162.65f, 39.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(72.375f, 55.95f)
                lineTo(74.975f, 54.55f)
                lineTo(95.375f, 62.95f)
                lineTo(92.775f, 64.35f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(72.375f, 59.75f)
                lineTo(72.375f, 55.95f)
                lineTo(92.775f, 64.35f)
                lineTo(92.775f, 68.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(90.297f, 73.131f)
                lineTo(86.453f, 76.169f)
                lineTo(83.203f, 50.369f)
                lineTo(87.047f, 47.331f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(86.453f, 76.169f)
                lineTo(81.353f, 74.069f)
                lineTo(78.103f, 48.269f)
                lineTo(83.203f, 50.369f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(154.296f, 42.277f)
                lineTo(157.054f, 44.823f)
                lineTo(141.454f, 76.023f)
                lineTo(138.696f, 73.477f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(149.196f, 40.177f)
                lineTo(154.296f, 42.277f)
                lineTo(138.696f, 73.477f)
                lineTo(133.596f, 71.377f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(133.596f, 71.377f)
                lineTo(138.696f, 73.477f)
                lineTo(141.454f, 76.023f)
                lineTo(136.354f, 73.923f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(64.975f, 101f)
                lineTo(70.925f, 103.45f)
                lineTo(66.375f, 105.9f)
                lineTo(60.425f, 103.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(60.062f, 118.525f)
                lineTo(62.188f, 119.4f)
                lineTo(58.938f, 121.15f)
                lineTo(56.812f, 120.275f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(62.188f, 119.4f)
                lineTo(62.188f, 121.775f)
                lineTo(58.938f, 123.525f)
                lineTo(58.938f, 121.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(50.65f, 98.95f)
                lineTo(53.9f, 97.2f)
                lineTo(79.4f, 107.7f)
                lineTo(76.15f, 109.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(39.525f, 104.75f)
                lineTo(39.375f, 101.6f)
                lineTo(39.375f, 96.85f)
                lineTo(39.525f, 100f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(99.875f, 72.45f)
                lineTo(100.475f, 69.8f)
                lineTo(97.225f, 71.55f)
                lineTo(96.625f, 74.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(50.65f, 103.7f)
                lineTo(50.65f, 98.95f)
                lineTo(76.15f, 109.45f)
                lineTo(76.15f, 114.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(89.025f, 104.75f)
                lineTo(89.175f, 107.9f)
                lineTo(89.175f, 103.15f)
                lineTo(89.025f, 100f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(56.812f, 120.275f)
                lineTo(58.938f, 121.15f)
                lineTo(58.938f, 123.525f)
                lineTo(56.812f, 122.65f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(59.675f, 88.96f)
                lineTo(86.875f, 100.16f)
                lineTo(86.825f, 102.61f)
                lineTo(73.825f, 109.61f)
                lineTo(67.825f, 109.61f)
                lineTo(42.325f, 99.11f)
                lineTo(42.375f, 96.66f)
                lineTo(55.375f, 89.66f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(59.945f, 123.37f)
                curveTo(59.945f, 121.691f, 58.727f, 119.829f, 57.225f, 119.21f)
                curveTo(55.723f, 118.591f, 54.505f, 119.451f, 54.505f, 121.13f)
                curveTo(54.505f, 122.809f, 55.723f, 124.671f, 57.225f, 125.29f)
                curveTo(58.727f, 125.909f, 59.945f, 125.049f, 59.945f, 123.37f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(95.375f, 66.75f)
                lineTo(92.775f, 68.15f)
                lineTo(92.775f, 64.35f)
                lineTo(95.375f, 62.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(79.4f, 112.45f)
                lineTo(76.15f, 114.2f)
                lineTo(76.15f, 109.45f)
                lineTo(79.4f, 107.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(60.125f, 88.1f)
                lineTo(89.025f, 100f)
                lineTo(89.175f, 103.15f)
                lineTo(74.225f, 111.2f)
                lineTo(66.725f, 111.2f)
                lineTo(39.525f, 100f)
                lineTo(39.375f, 96.85f)
                lineTo(54.325f, 88.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(6.55f, 127.25f)
                lineTo(11.1f, 124.8f)
                lineTo(91f, 157.7f)
                lineTo(86.45f, 160.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(81.947f, 45.231f)
                lineTo(87.047f, 47.331f)
                lineTo(83.203f, 50.369f)
                lineTo(78.103f, 48.269f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(6.55f, 133.9f)
                lineTo(6.55f, 127.25f)
                lineTo(86.45f, 160.15f)
                lineTo(86.45f, 166.8f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(71.125f, 63.7f)
                lineTo(96.625f, 74.2f)
                lineTo(97.225f, 71.55f)
                lineTo(90.725f, 53.2f)
                lineTo(86.025f, 47.75f)
                lineTo(62.225f, 37.95f)
                lineTo(60.125f, 40.6f)
                lineTo(66.625f, 58.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(72.83f, 62.355f)
                lineTo(93.23f, 70.755f)
                lineTo(94.025f, 69.9f)
                lineTo(88.175f, 53.1f)
                lineTo(84.975f, 49.55f)
                lineTo(64.575f, 41.15f)
                lineTo(62.675f, 42.6f)
                lineTo(68.525f, 59.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(65.475f, 36.2f)
                lineTo(63.375f, 38.85f)
                lineTo(60.125f, 40.6f)
                lineTo(62.225f, 37.95f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(138.787f, 82.488f)
                curveTo(138.787f, 72.519f, 144.317f, 61.46f, 151.137f, 57.788f)
                curveTo(157.958f, 54.115f, 163.488f, 59.219f, 163.488f, 69.188f)
                curveTo(163.488f, 79.156f, 157.958f, 90.215f, 151.137f, 93.888f)
                curveTo(144.317f, 97.56f, 138.787f, 92.456f, 138.787f, 82.488f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(135.251f, 69.68f)
                lineTo(140.351f, 71.78f)
                lineTo(117.051f, 75.68f)
                lineTo(111.951f, 73.58f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(140.351f, 71.78f)
                lineTo(139.799f, 77.72f)
                lineTo(116.499f, 81.62f)
                lineTo(117.051f, 75.68f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(157.869f, 87.346f)
                lineTo(156.46f, 89.259f)
                lineTo(154.96f, 90.943f)
                lineTo(153.394f, 92.368f)
                lineTo(151.79f, 93.511f)
                lineTo(150.175f, 94.351f)
                lineTo(148.576f, 94.874f)
                lineTo(147.021f, 95.072f)
                lineTo(145.536f, 94.94f)
                lineTo(144.148f, 94.482f)
                lineTo(142.879f, 93.705f)
                lineTo(141.751f, 92.622f)
                lineTo(140.783f, 91.251f)
                lineTo(139.993f, 89.617f)
                lineTo(139.394f, 87.748f)
                lineTo(138.995f, 85.674f)
                lineTo(138.805f, 83.432f)
                lineTo(138.825f, 81.061f)
                lineTo(139.056f, 78.599f)
                lineTo(139.494f, 76.091f)
                lineTo(140.131f, 73.578f)
                lineTo(140.956f, 71.104f)
                lineTo(141.955f, 68.711f)
                lineTo(143.112f, 66.44f)
                lineTo(144.406f, 64.329f)
                lineTo(149.081f, 66.254f)
                lineTo(147.787f, 68.365f)
                lineTo(146.63f, 70.636f)
                lineTo(145.631f, 73.029f)
                lineTo(144.806f, 75.503f)
                lineTo(144.169f, 78.016f)
                lineTo(143.731f, 80.524f)
                lineTo(143.5f, 82.986f)
                lineTo(143.48f, 85.357f)
                lineTo(143.67f, 87.599f)
                lineTo(144.069f, 89.673f)
                lineTo(144.668f, 91.542f)
                lineTo(145.458f, 93.176f)
                lineTo(146.426f, 94.547f)
                lineTo(147.554f, 95.63f)
                lineTo(148.823f, 96.407f)
                lineTo(150.211f, 96.865f)
                lineTo(151.696f, 96.997f)
                lineTo(153.251f, 96.799f)
                lineTo(154.85f, 96.276f)
                lineTo(156.465f, 95.436f)
                lineTo(158.069f, 94.293f)
                lineTo(159.635f, 92.868f)
                lineTo(161.135f, 91.184f)
                lineTo(162.544f, 89.271f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(143.463f, 84.413f)
                curveTo(143.463f, 74.444f, 148.992f, 63.385f, 155.812f, 59.713f)
                curveTo(162.633f, 56.04f, 168.162f, 61.144f, 168.162f, 71.113f)
                curveTo(168.162f, 81.081f, 162.633f, 92.14f, 155.812f, 95.813f)
                curveTo(148.992f, 99.485f, 143.463f, 94.381f, 143.463f, 84.413f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(136.713f, 69.15f)
                lineTo(139.637f, 67.575f)
                lineTo(171.938f, 80.875f)
                lineTo(169.012f, 82.45f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(144.605f, 83.895f)
                curveTo(144.605f, 74.765f, 149.668f, 64.638f, 155.915f, 61.275f)
                curveTo(162.161f, 57.911f, 167.225f, 62.585f, 167.225f, 71.715f)
                curveTo(167.225f, 80.844f, 162.161f, 90.971f, 155.915f, 94.335f)
                curveTo(149.668f, 97.698f, 144.605f, 93.024f, 144.605f, 83.895f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(146.267f, 83.097f)
                curveTo(146.267f, 75.226f, 150.632f, 66.496f, 156.017f, 63.597f)
                curveTo(161.401f, 60.697f, 165.767f, 64.726f, 165.767f, 72.597f)
                curveTo(165.767f, 80.467f, 161.401f, 89.197f, 156.017f, 92.097f)
                curveTo(150.632f, 94.996f, 146.267f, 90.967f, 146.267f, 83.097f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(154.168f, 78.939f)
                curveTo(154.168f, 77.364f, 155.042f, 75.618f, 156.118f, 75.038f)
                curveTo(157.195f, 74.459f, 158.069f, 75.264f, 158.069f, 76.839f)
                curveTo(158.069f, 78.413f, 157.195f, 80.159f, 156.118f, 80.739f)
                curveTo(155.042f, 81.318f, 154.168f, 80.513f, 154.168f, 78.939f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(136.713f, 73.425f)
                lineTo(136.713f, 69.15f)
                lineTo(169.012f, 82.45f)
                lineTo(169.012f, 86.725f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(144.738f, 84.938f)
                curveTo(144.738f, 74.969f, 150.267f, 63.91f, 157.088f, 60.238f)
                curveTo(163.908f, 56.565f, 169.438f, 61.669f, 169.438f, 71.638f)
                curveTo(169.438f, 81.606f, 163.908f, 92.665f, 157.088f, 96.338f)
                curveTo(150.267f, 100.01f, 144.738f, 94.906f, 144.738f, 84.938f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(89.175f, 107.9f)
                lineTo(74.225f, 115.95f)
                lineTo(74.225f, 111.2f)
                lineTo(89.175f, 103.15f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(66.725f, 115.95f)
                lineTo(39.525f, 104.75f)
                lineTo(39.525f, 100f)
                lineTo(66.725f, 111.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(100.475f, 69.8f)
                lineTo(93.975f, 51.45f)
                lineTo(90.725f, 53.2f)
                lineTo(97.225f, 71.55f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(102.245f, 79.51f)
                lineTo(105.9f, 81.015f)
                lineTo(103.105f, 82.52f)
                lineTo(99.45f, 81.015f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(163.819f, 89.796f)
                lineTo(162.41f, 91.709f)
                lineTo(160.91f, 93.393f)
                lineTo(159.344f, 94.818f)
                lineTo(157.74f, 95.961f)
                lineTo(156.125f, 96.801f)
                lineTo(154.526f, 97.324f)
                lineTo(152.971f, 97.522f)
                lineTo(151.486f, 97.39f)
                lineTo(150.098f, 96.932f)
                lineTo(148.829f, 96.155f)
                lineTo(147.701f, 95.072f)
                lineTo(146.733f, 93.701f)
                lineTo(145.943f, 92.067f)
                lineTo(145.344f, 90.198f)
                lineTo(144.945f, 88.124f)
                lineTo(144.755f, 85.882f)
                lineTo(144.775f, 83.511f)
                lineTo(145.006f, 81.049f)
                lineTo(145.444f, 78.541f)
                lineTo(146.081f, 76.028f)
                lineTo(146.906f, 73.554f)
                lineTo(147.905f, 71.161f)
                lineTo(149.062f, 68.89f)
                lineTo(150.356f, 66.779f)
                lineTo(155.031f, 68.704f)
                lineTo(153.737f, 70.815f)
                lineTo(152.58f, 73.086f)
                lineTo(151.581f, 75.479f)
                lineTo(150.756f, 77.953f)
                lineTo(150.119f, 80.466f)
                lineTo(149.681f, 82.974f)
                lineTo(149.45f, 85.436f)
                lineTo(149.43f, 87.807f)
                lineTo(149.62f, 90.049f)
                lineTo(150.019f, 92.123f)
                lineTo(150.618f, 93.992f)
                lineTo(151.408f, 95.626f)
                lineTo(152.376f, 96.997f)
                lineTo(153.504f, 98.08f)
                lineTo(154.773f, 98.857f)
                lineTo(156.161f, 99.315f)
                lineTo(157.646f, 99.447f)
                lineTo(159.201f, 99.249f)
                lineTo(160.8f, 98.726f)
                lineTo(162.415f, 97.886f)
                lineTo(164.019f, 96.743f)
                lineTo(165.585f, 95.318f)
                lineTo(167.085f, 93.634f)
                lineTo(168.494f, 91.721f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(149.412f, 86.863f)
                curveTo(149.412f, 76.894f, 154.942f, 65.835f, 161.762f, 62.163f)
                curveTo(168.583f, 58.49f, 174.113f, 63.594f, 174.113f, 73.563f)
                curveTo(174.113f, 83.531f, 168.583f, 94.59f, 161.762f, 98.263f)
                curveTo(154.942f, 101.935f, 149.412f, 96.831f, 149.412f, 86.863f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(150.555f, 86.345f)
                curveTo(150.555f, 77.215f, 155.618f, 67.088f, 161.864f, 63.725f)
                curveTo(168.111f, 60.361f, 173.174f, 65.035f, 173.174f, 74.165f)
                curveTo(173.174f, 83.294f, 168.111f, 93.421f, 161.865f, 96.785f)
                curveTo(155.618f, 100.148f, 150.555f, 95.474f, 150.555f, 86.345f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(152.216f, 85.547f)
                curveTo(152.216f, 77.676f, 156.582f, 68.946f, 161.966f, 66.047f)
                curveTo(167.351f, 63.147f, 171.716f, 67.176f, 171.716f, 75.047f)
                curveTo(171.716f, 82.917f, 167.351f, 91.647f, 161.966f, 94.547f)
                curveTo(156.582f, 97.446f, 152.216f, 93.417f, 152.216f, 85.547f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(160.118f, 81.389f)
                curveTo(160.118f, 79.814f, 160.992f, 78.068f, 162.069f, 77.489f)
                curveTo(163.145f, 76.909f, 164.018f, 77.714f, 164.018f, 79.289f)
                curveTo(164.018f, 80.863f, 163.145f, 82.609f, 162.069f, 83.189f)
                curveTo(160.992f, 83.768f, 160.118f, 82.963f, 160.118f, 81.389f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(163.5315f, 77.414f)
                lineTo(171.938f, 80.875f)
                lineTo(169.012f, 82.45f)
                lineTo(160.6055f, 78.989f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(160.6055f, 78.989f)
                lineTo(169.012f, 82.45f)
                lineTo(169.012f, 86.725f)
                lineTo(160.6055f, 83.264f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(105.9f, 82.25f)
                lineTo(103.105f, 83.755f)
                lineTo(103.105f, 82.52f)
                lineTo(105.9f, 81.015f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(103.105f, 83.755f)
                lineTo(99.45f, 82.25f)
                lineTo(99.45f, 81.015f)
                lineTo(103.105f, 82.52f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(89.275f, 46f)
                lineTo(65.475f, 36.2f)
                lineTo(62.225f, 37.95f)
                lineTo(86.025f, 47.75f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(111.951f, 73.58f)
                lineTo(117.051f, 75.68f)
                lineTo(116.499f, 81.62f)
                lineTo(111.399f, 79.52f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(115.713f, 75.089f)
                lineTo(115.337f, 78.711f)
                lineTo(112.737f, 80.111f)
                lineTo(113.113f, 76.489f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(115.713f, 75.089f)
                lineTo(113.113f, 76.489f)
                lineTo(101.563f, 74.489f)
                lineTo(104.163f, 73.089f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(113.113f, 76.489f)
                lineTo(112.737f, 80.111f)
                lineTo(101.187f, 78.111f)
                lineTo(101.563f, 74.489f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(105.225f, 81.3f)
                lineTo(103.015f, 82.49f)
                lineTo(103.015f, 68.24f)
                lineTo(105.225f, 67.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(103.015f, 82.49f)
                lineTo(100.125f, 81.3f)
                lineTo(100.125f, 67.05f)
                lineTo(103.015f, 68.24f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(171.938f, 85.15f)
                lineTo(169.012f, 86.725f)
                lineTo(169.012f, 82.45f)
                lineTo(171.938f, 80.875f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(164.6f, 80.55f)
                lineTo(167.85f, 78.8f)
                lineTo(172.95f, 80.9f)
                lineTo(169.7f, 82.65f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(172.95f, 85.65f)
                lineTo(169.7f, 87.4f)
                lineTo(169.7f, 82.65f)
                lineTo(172.95f, 80.9f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(164.6f, 85.3f)
                lineTo(164.6f, 80.55f)
                lineTo(169.7f, 82.65f)
                lineTo(169.7f, 87.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(74.225f, 115.95f)
                lineTo(66.725f, 115.95f)
                lineTo(66.725f, 111.2f)
                lineTo(74.225f, 111.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(102.335f, 65.86f)
                lineTo(105.225f, 67.05f)
                lineTo(103.015f, 68.24f)
                lineTo(100.125f, 67.05f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(102.245f, 64.595f)
                lineTo(105.9f, 66.1f)
                lineTo(103.105f, 67.605f)
                lineTo(99.45f, 66.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(105.9f, 67.43f)
                lineTo(103.105f, 68.935f)
                lineTo(103.105f, 67.605f)
                lineTo(105.9f, 66.1f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(93.975f, 51.45f)
                lineTo(89.275f, 46f)
                lineTo(86.025f, 47.75f)
                lineTo(90.725f, 53.2f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(103.105f, 68.935f)
                lineTo(99.45f, 67.43f)
                lineTo(99.45f, 66.1f)
                lineTo(103.105f, 67.605f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(78.8f, 163.65f)
                lineTo(83.35f, 161.2f)
                lineTo(93.55f, 165.4f)
                lineTo(89f, 167.85f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(81.5f, 155.95f)
                lineTo(87.45f, 158.4f)
                lineTo(87.45f, 165.05f)
                lineTo(81.5f, 162.6f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(78.8f, 166.5f)
                lineTo(78.8f, 163.65f)
                lineTo(89f, 167.85f)
                lineTo(89f, 170.7f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(93.55f, 168.25f)
                lineTo(89f, 170.7f)
                lineTo(89f, 167.85f)
                lineTo(93.55f, 165.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(91f, 164.35f)
                lineTo(86.45f, 166.8f)
                lineTo(86.45f, 160.15f)
                lineTo(91f, 157.7f)
                close()
            }
        }.build()
        return _PlateLoadedRow!!
    }
