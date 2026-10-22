package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _PlateLoadedSeatedDip: ImageVector? = null

public val PlateLoadedSeatedDip: ImageVector
    get() {
        if (_PlateLoadedSeatedDip != null) {
            return _PlateLoadedSeatedDip!!
        }
        _PlateLoadedSeatedDip = ImageVector.Builder(
            name = "PlateLoadedSeatedDip",
            defaultWidth = 194.dp,
            defaultHeight = 165.dp,
            viewportWidth = 194f,
            viewportHeight = 165f,
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(75.5f, 82.977f)
                lineTo(80.7f, 80.177f)
                lineTo(90.9f, 84.377f)
                lineTo(85.7f, 87.177f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(75.5f, 85.827f)
                lineTo(75.5f, 82.977f)
                lineTo(85.7f, 87.177f)
                lineTo(85.7f, 90.027f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(90.9f, 87.227f)
                lineTo(85.7f, 90.027f)
                lineTo(85.7f, 87.177f)
                lineTo(90.9f, 84.377f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(65.828f, 19.497f)
                lineTo(62.773f, 21.142f)
                lineTo(55.973f, 18.342f)
                lineTo(59.028f, 16.697f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(62.773f, 21.142f)
                lineTo(62.773f, 25.607f)
                lineTo(55.973f, 22.807f)
                lineTo(55.973f, 18.342f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(65.828f, 19.497f)
                lineTo(65.828f, 23.962f)
                lineTo(62.773f, 25.607f)
                lineTo(62.773f, 21.142f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(54.515f, 30.567f)
                curveTo(54.515f, 20.336f, 60.19f, 8.986f, 67.19f, 5.217f)
                curveTo(74.19f, 1.448f, 79.865f, 6.686f, 79.865f, 16.917f)
                curveTo(79.865f, 27.148f, 74.19f, 38.498f, 67.19f, 42.267f)
                curveTo(60.19f, 46.036f, 54.515f, 40.798f, 54.515f, 30.567f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(74.099f, 35.553f)
                lineTo(72.653f, 37.517f)
                lineTo(71.113f, 39.245f)
                lineTo(69.506f, 40.708f)
                lineTo(67.86f, 41.88f)
                lineTo(66.202f, 42.743f)
                lineTo(64.561f, 43.28f)
                lineTo(62.965f, 43.483f)
                lineTo(61.442f, 43.348f)
                lineTo(60.016f, 42.877f)
                lineTo(58.714f, 42.08f)
                lineTo(57.556f, 40.968f)
                lineTo(56.563f, 39.562f)
                lineTo(55.753f, 37.885f)
                lineTo(55.137f, 35.966f)
                lineTo(54.728f, 33.838f)
                lineTo(54.533f, 31.537f)
                lineTo(54.554f, 29.103f)
                lineTo(54.791f, 26.577f)
                lineTo(55.24f, 24.002f)
                lineTo(55.893f, 21.423f)
                lineTo(56.74f, 18.884f)
                lineTo(57.766f, 16.428f)
                lineTo(58.953f, 14.097f)
                lineTo(60.281f, 11.931f)
                lineTo(64.701f, 13.751f)
                lineTo(63.373f, 15.917f)
                lineTo(62.186f, 18.248f)
                lineTo(61.16f, 20.704f)
                lineTo(60.313f, 23.243f)
                lineTo(59.66f, 25.822f)
                lineTo(59.211f, 28.397f)
                lineTo(58.974f, 30.923f)
                lineTo(58.953f, 33.357f)
                lineTo(59.148f, 35.658f)
                lineTo(59.557f, 37.786f)
                lineTo(60.173f, 39.705f)
                lineTo(60.983f, 41.382f)
                lineTo(61.976f, 42.788f)
                lineTo(63.134f, 43.9f)
                lineTo(64.436f, 44.697f)
                lineTo(65.862f, 45.168f)
                lineTo(67.385f, 45.303f)
                lineTo(68.981f, 45.1f)
                lineTo(70.622f, 44.563f)
                lineTo(72.28f, 43.7f)
                lineTo(73.926f, 42.528f)
                lineTo(75.533f, 41.065f)
                lineTo(77.073f, 39.337f)
                lineTo(78.519f, 37.373f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(58.935f, 32.387f)
                curveTo(58.935f, 22.156f, 64.61f, 10.806f, 71.61f, 7.037f)
                curveTo(78.61f, 3.268f, 84.285f, 8.506f, 84.285f, 18.737f)
                curveTo(84.285f, 28.968f, 78.61f, 40.318f, 71.61f, 44.087f)
                curveTo(64.61f, 47.856f, 58.935f, 42.618f, 58.935f, 32.387f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(60.012f, 31.904f)
                curveTo(60.012f, 22.46f, 65.25f, 11.983f, 71.712f, 8.504f)
                curveTo(78.174f, 5.025f, 83.412f, 9.86f, 83.412f, 19.304f)
                curveTo(83.412f, 28.748f, 78.174f, 39.225f, 71.712f, 42.704f)
                curveTo(65.25f, 46.183f, 60.012f, 41.348f, 60.012f, 31.904f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(61.739f, 31.071f)
                curveTo(61.739f, 22.939f, 66.25f, 13.917f, 71.814f, 10.921f)
                curveTo(77.378f, 7.925f, 81.889f, 12.089f, 81.889f, 20.221f)
                curveTo(81.889f, 28.353f, 77.378f, 37.375f, 71.814f, 40.371f)
                curveTo(66.25f, 43.367f, 61.739f, 39.203f, 61.739f, 31.071f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(69.966f, 26.738f)
                curveTo(69.966f, 25.164f, 70.839f, 23.418f, 71.916f, 22.838f)
                curveTo(72.993f, 22.258f, 73.866f, 23.064f, 73.866f, 24.638f)
                curveTo(73.866f, 26.212f, 72.993f, 27.958f, 71.916f, 28.538f)
                curveTo(70.839f, 29.118f, 69.966f, 28.312f, 69.966f, 26.738f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(60.465f, 33.017f)
                curveTo(60.465f, 22.786f, 66.14f, 11.436f, 73.14f, 7.667f)
                curveTo(80.14f, 3.898f, 85.815f, 9.136f, 85.815f, 19.367f)
                curveTo(85.815f, 29.598f, 80.14f, 40.948f, 73.14f, 44.717f)
                curveTo(66.14f, 48.486f, 60.465f, 43.248f, 60.465f, 33.017f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(87.863f, 28.727f)
                lineTo(84.938f, 30.302f)
                lineTo(56.038f, 18.402f)
                lineTo(58.963f, 16.827f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(84.938f, 30.302f)
                lineTo(84.938f, 34.577f)
                lineTo(56.038f, 22.677f)
                lineTo(56.038f, 18.402f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(80.049f, 38.003f)
                lineTo(78.603f, 39.967f)
                lineTo(77.063f, 41.695f)
                lineTo(75.456f, 43.158f)
                lineTo(73.81f, 44.33f)
                lineTo(72.152f, 45.193f)
                lineTo(70.511f, 45.73f)
                lineTo(68.915f, 45.933f)
                lineTo(67.392f, 45.798f)
                lineTo(65.966f, 45.327f)
                lineTo(64.664f, 44.53f)
                lineTo(63.506f, 43.418f)
                lineTo(62.513f, 42.012f)
                lineTo(61.703f, 40.335f)
                lineTo(61.087f, 38.416f)
                lineTo(60.678f, 36.288f)
                lineTo(60.483f, 33.987f)
                lineTo(60.504f, 31.553f)
                lineTo(60.741f, 29.027f)
                lineTo(61.19f, 26.452f)
                lineTo(61.843f, 23.873f)
                lineTo(62.69f, 21.334f)
                lineTo(63.716f, 18.878f)
                lineTo(64.903f, 16.547f)
                lineTo(66.231f, 14.381f)
                lineTo(70.651f, 16.201f)
                lineTo(69.323f, 18.367f)
                lineTo(68.136f, 20.698f)
                lineTo(67.11f, 23.154f)
                lineTo(66.263f, 25.693f)
                lineTo(65.61f, 28.272f)
                lineTo(65.161f, 30.847f)
                lineTo(64.924f, 33.373f)
                lineTo(64.903f, 35.807f)
                lineTo(65.098f, 38.108f)
                lineTo(65.507f, 40.236f)
                lineTo(66.123f, 42.155f)
                lineTo(66.933f, 43.832f)
                lineTo(67.926f, 45.238f)
                lineTo(69.084f, 46.35f)
                lineTo(70.386f, 47.147f)
                lineTo(71.812f, 47.618f)
                lineTo(73.335f, 47.753f)
                lineTo(74.931f, 47.55f)
                lineTo(76.572f, 47.013f)
                lineTo(78.23f, 46.15f)
                lineTo(79.876f, 44.978f)
                lineTo(81.483f, 43.515f)
                lineTo(83.023f, 41.787f)
                lineTo(84.469f, 39.823f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(64.885f, 34.837f)
                curveTo(64.885f, 24.606f, 70.56f, 13.256f, 77.56f, 9.487f)
                curveTo(84.56f, 5.718f, 90.235f, 10.956f, 90.235f, 21.187f)
                curveTo(90.235f, 31.418f, 84.56f, 42.768f, 77.56f, 46.537f)
                curveTo(70.56f, 50.306f, 64.885f, 45.068f, 64.885f, 34.837f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(65.962f, 34.354f)
                curveTo(65.962f, 24.91f, 71.2f, 14.433f, 77.662f, 10.954f)
                curveTo(84.124f, 7.475f, 89.362f, 12.31f, 89.362f, 21.754f)
                curveTo(89.362f, 31.198f, 84.124f, 41.675f, 77.662f, 45.154f)
                curveTo(71.2f, 48.633f, 65.962f, 43.798f, 65.962f, 34.354f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(67.689f, 33.521f)
                curveTo(67.689f, 25.389f, 72.2f, 16.367f, 77.764f, 13.371f)
                curveTo(83.328f, 10.375f, 87.839f, 14.539f, 87.839f, 22.671f)
                curveTo(87.839f, 30.803f, 83.328f, 39.825f, 77.764f, 42.821f)
                curveTo(72.2f, 45.817f, 67.689f, 41.653f, 67.689f, 33.521f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(75.916f, 29.188f)
                curveTo(75.916f, 27.614f, 76.789f, 25.868f, 77.866f, 25.288f)
                curveTo(78.943f, 24.708f, 79.816f, 25.514f, 79.816f, 27.088f)
                curveTo(79.816f, 28.662f, 78.943f, 30.408f, 77.866f, 30.988f)
                curveTo(76.789f, 31.568f, 75.916f, 30.762f, 75.916f, 29.188f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(79.3285f, 25.213f)
                lineTo(87.863f, 28.727f)
                lineTo(84.938f, 30.302f)
                lineTo(76.4035f, 26.788f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(76.4035f, 26.788f)
                lineTo(84.938f, 30.302f)
                lineTo(84.938f, 34.577f)
                lineTo(76.4035f, 31.063f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(87.863f, 28.727f)
                lineTo(87.863f, 33.002f)
                lineTo(84.938f, 34.577f)
                lineTo(84.938f, 30.302f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(77.525f, 76.852f)
                lineTo(82.075f, 74.402f)
                lineTo(165.375f, 108.702f)
                lineTo(160.825f, 111.152f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(81.416f, 84.96f)
                lineTo(75.466f, 82.51f)
                lineTo(64.416f, 48.56f)
                lineTo(70.366f, 51.01f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(85.634f, 80.194f)
                lineTo(81.416f, 84.96f)
                lineTo(70.366f, 51.01f)
                lineTo(74.584f, 46.244f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(77.525f, 83.502f)
                lineTo(77.525f, 76.852f)
                lineTo(160.825f, 111.152f)
                lineTo(160.825f, 117.802f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(88.512f, 30.161f)
                lineTo(89.388f, 35.243f)
                lineTo(72.488f, 50.043f)
                lineTo(71.612f, 44.961f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(83.412f, 28.061f)
                lineTo(88.512f, 30.161f)
                lineTo(71.612f, 44.961f)
                lineTo(66.512f, 42.861f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(51.827f, 97.066f)
                lineTo(49.123f, 95.888f)
                lineTo(53.373f, 97.638f)
                lineTo(56.077f, 98.816f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(56.077f, 98.816f)
                lineTo(53.373f, 97.638f)
                lineTo(70.273f, 51.488f)
                lineTo(72.977f, 52.666f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(68.634f, 43.794f)
                lineTo(74.584f, 46.244f)
                lineTo(70.366f, 51.01f)
                lineTo(64.416f, 48.56f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(66.512f, 42.861f)
                lineTo(71.612f, 44.961f)
                lineTo(72.488f, 50.043f)
                lineTo(67.388f, 47.943f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(53.373f, 97.638f)
                lineTo(49.123f, 95.888f)
                lineTo(66.023f, 49.738f)
                lineTo(70.273f, 51.488f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(80.225f, 75.802f)
                lineTo(86.175f, 78.252f)
                lineTo(14.675f, 116.752f)
                lineTo(8.725f, 114.302f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(86.175f, 78.252f)
                lineTo(86.175f, 84.902f)
                lineTo(14.675f, 123.402f)
                lineTo(14.675f, 116.752f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(62.45f, 42.552f)
                lineTo(66.35f, 40.452f)
                lineTo(76.55f, 44.652f)
                lineTo(72.65f, 46.752f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(62.45f, 48.252f)
                lineTo(62.45f, 42.552f)
                lineTo(72.65f, 46.752f)
                lineTo(72.65f, 52.452f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(76.55f, 50.352f)
                lineTo(72.65f, 52.452f)
                lineTo(72.65f, 46.752f)
                lineTo(76.55f, 44.652f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(71.52f, 50.372f)
                curveTo(71.52f, 47.749f, 72.975f, 44.839f, 74.77f, 43.872f)
                curveTo(76.565f, 42.906f, 78.02f, 44.249f, 78.02f, 46.872f)
                curveTo(78.02f, 49.495f, 76.565f, 52.406f, 74.77f, 53.372f)
                curveTo(72.975f, 54.339f, 71.52f, 52.995f, 71.52f, 50.372f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(73.725f, 49.427f)
                curveTo(73.725f, 48.378f, 74.307f, 47.214f, 75.025f, 46.827f)
                curveTo(75.743f, 46.44f, 76.325f, 46.978f, 76.325f, 48.027f)
                curveTo(76.325f, 49.076f, 75.743f, 50.24f, 75.025f, 50.627f)
                curveTo(74.307f, 51.014f, 73.725f, 50.476f, 73.725f, 49.427f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(152f, 114.477f)
                lineTo(157.2f, 111.677f)
                lineTo(167.4f, 115.877f)
                lineTo(162.2f, 118.677f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(152f, 117.327f)
                lineTo(152f, 114.477f)
                lineTo(162.2f, 118.677f)
                lineTo(162.2f, 121.527f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(167.4f, 118.727f)
                lineTo(162.2f, 121.527f)
                lineTo(162.2f, 118.677f)
                lineTo(167.4f, 115.877f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(113.561f, 31.606f)
                lineTo(117.114f, 29.123f)
                lineTo(122.314f, 95.673f)
                lineTo(118.761f, 98.156f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(108.886f, 29.681f)
                lineTo(113.561f, 31.606f)
                lineTo(118.761f, 98.156f)
                lineTo(114.086f, 96.231f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(165.375f, 115.352f)
                lineTo(160.825f, 117.802f)
                lineTo(160.825f, 111.152f)
                lineTo(165.375f, 108.702f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(117.175f, 92.252f)
                lineTo(123.125f, 94.702f)
                lineTo(64.625f, 126.202f)
                lineTo(58.675f, 123.752f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(123.125f, 94.702f)
                lineTo(123.125f, 101.352f)
                lineTo(64.625f, 132.852f)
                lineTo(64.625f, 126.202f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(67.77f, 42.375f)
                lineTo(72.87f, 44.475f)
                lineTo(46.22f, 40.775f)
                lineTo(41.12f, 38.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(72.87f, 44.475f)
                lineTo(71.23f, 50.529f)
                lineTo(44.58f, 46.829f)
                lineTo(46.22f, 40.775f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(98.75f, 109.252f)
                lineTo(94.2f, 111.702f)
                lineTo(94.2f, 88.902f)
                lineTo(98.75f, 86.452f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(94.2f, 111.702f)
                lineTo(88.25f, 109.252f)
                lineTo(88.25f, 86.452f)
                lineTo(94.2f, 88.902f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(103.046f, 87.696f)
                lineTo(99.454f, 87.409f)
                lineTo(113.754f, 30.309f)
                lineTo(117.346f, 30.596f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(99.454f, 87.409f)
                lineTo(94.354f, 85.309f)
                lineTo(108.654f, 28.209f)
                lineTo(113.754f, 30.309f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(92.8f, 84.002f)
                lineTo(98.75f, 86.452f)
                lineTo(94.2f, 88.902f)
                lineTo(88.25f, 86.452f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(112.246f, 28.496f)
                lineTo(117.346f, 30.596f)
                lineTo(113.754f, 30.309f)
                lineTo(108.654f, 28.209f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(108.886f, 29.681f)
                lineTo(112.439f, 27.198f)
                lineTo(117.114f, 29.123f)
                lineTo(113.561f, 31.606f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(74.55f, 76.702f)
                lineTo(78.45f, 74.602f)
                lineTo(112.45f, 88.602f)
                lineTo(108.55f, 90.702f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(74.55f, 82.402f)
                lineTo(74.55f, 76.702f)
                lineTo(108.55f, 90.702f)
                lineTo(108.55f, 96.402f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(152.816f, 114.36f)
                lineTo(146.866f, 111.91f)
                lineTo(135.816f, 77.96f)
                lineTo(141.766f, 80.41f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(157.034f, 109.594f)
                lineTo(152.816f, 114.36f)
                lineTo(141.766f, 80.41f)
                lineTo(145.984f, 75.644f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(108.639f, 77.939f)
                lineTo(83.989f, 67.789f)
                lineTo(95.689f, 18.739f)
                lineTo(120.339f, 28.889f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(101.241f, 18.995f)
                lineTo(125.891f, 29.145f)
                lineTo(120.339f, 28.889f)
                lineTo(95.689f, 18.739f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(110.009f, 78.852f)
                lineTo(81.959f, 67.302f)
                lineTo(93.659f, 18.252f)
                lineTo(121.709f, 29.802f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(99.091f, 18.502f)
                lineTo(127.141f, 30.052f)
                lineTo(121.709f, 29.802f)
                lineTo(93.659f, 18.252f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(153.965f, 71.517f)
                curveTo(153.965f, 61.286f, 159.64f, 49.936f, 166.64f, 46.167f)
                curveTo(173.64f, 42.398f, 179.315f, 47.636f, 179.315f, 57.867f)
                curveTo(179.315f, 68.098f, 173.64f, 79.448f, 166.64f, 83.217f)
                curveTo(159.64f, 86.986f, 153.965f, 81.748f, 153.965f, 71.517f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(4f, 121.477f)
                lineTo(9.2f, 118.677f)
                lineTo(19.4f, 122.877f)
                lineTo(14.2f, 125.677f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(114.191f, 78.195f)
                lineTo(108.639f, 77.939f)
                lineTo(120.339f, 28.889f)
                lineTo(125.891f, 29.145f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(8.725f, 114.302f)
                lineTo(14.675f, 116.752f)
                lineTo(14.675f, 123.402f)
                lineTo(8.725f, 120.952f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(112.45f, 94.302f)
                lineTo(108.55f, 96.402f)
                lineTo(108.55f, 90.702f)
                lineTo(112.45f, 88.602f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(41.12f, 38.675f)
                lineTo(46.22f, 40.775f)
                lineTo(44.58f, 46.829f)
                lineTo(39.48f, 44.729f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(4f, 124.327f)
                lineTo(4f, 121.477f)
                lineTo(14.2f, 125.677f)
                lineTo(14.2f, 128.527f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(19.4f, 125.727f)
                lineTo(14.2f, 128.527f)
                lineTo(14.2f, 125.677f)
                lineTo(19.4f, 122.877f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(173.549f, 76.503f)
                lineTo(172.103f, 78.467f)
                lineTo(170.563f, 80.195f)
                lineTo(168.956f, 81.658f)
                lineTo(167.31f, 82.83f)
                lineTo(165.652f, 83.693f)
                lineTo(164.011f, 84.23f)
                lineTo(162.415f, 84.433f)
                lineTo(160.892f, 84.298f)
                lineTo(159.466f, 83.827f)
                lineTo(158.164f, 83.03f)
                lineTo(157.006f, 81.918f)
                lineTo(156.013f, 80.512f)
                lineTo(155.203f, 78.835f)
                lineTo(154.587f, 76.916f)
                lineTo(154.178f, 74.788f)
                lineTo(153.983f, 72.487f)
                lineTo(154.004f, 70.053f)
                lineTo(154.241f, 67.527f)
                lineTo(154.69f, 64.952f)
                lineTo(155.343f, 62.373f)
                lineTo(156.19f, 59.834f)
                lineTo(157.216f, 57.378f)
                lineTo(158.403f, 55.047f)
                lineTo(159.731f, 52.881f)
                lineTo(164.151f, 54.701f)
                lineTo(162.823f, 56.867f)
                lineTo(161.636f, 59.198f)
                lineTo(160.61f, 61.654f)
                lineTo(159.763f, 64.193f)
                lineTo(159.11f, 66.772f)
                lineTo(158.661f, 69.347f)
                lineTo(158.424f, 71.873f)
                lineTo(158.403f, 74.307f)
                lineTo(158.598f, 76.608f)
                lineTo(159.007f, 78.736f)
                lineTo(159.623f, 80.655f)
                lineTo(160.433f, 82.332f)
                lineTo(161.426f, 83.738f)
                lineTo(162.584f, 84.85f)
                lineTo(163.886f, 85.647f)
                lineTo(165.312f, 86.118f)
                lineTo(166.835f, 86.253f)
                lineTo(168.431f, 86.05f)
                lineTo(170.072f, 85.513f)
                lineTo(171.73f, 84.65f)
                lineTo(173.376f, 83.478f)
                lineTo(174.983f, 82.015f)
                lineTo(176.523f, 80.287f)
                lineTo(177.969f, 78.323f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(158.385f, 73.337f)
                curveTo(158.385f, 63.106f, 164.06f, 51.756f, 171.06f, 47.987f)
                curveTo(178.06f, 44.218f, 183.735f, 49.456f, 183.735f, 59.687f)
                curveTo(183.735f, 69.918f, 178.06f, 81.268f, 171.06f, 85.037f)
                curveTo(164.06f, 88.806f, 158.385f, 83.568f, 158.385f, 73.337f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(159.462f, 72.854f)
                curveTo(159.462f, 63.41f, 164.7f, 52.933f, 171.162f, 49.454f)
                curveTo(177.624f, 45.975f, 182.862f, 50.81f, 182.862f, 60.254f)
                curveTo(182.862f, 69.698f, 177.624f, 80.175f, 171.162f, 83.654f)
                curveTo(164.7f, 87.133f, 159.462f, 82.298f, 159.462f, 72.854f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(115.441f, 79.102f)
                lineTo(110.009f, 78.852f)
                lineTo(121.709f, 29.802f)
                lineTo(127.141f, 30.052f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(161.189f, 72.021f)
                curveTo(161.189f, 63.889f, 165.7f, 54.867f, 171.264f, 51.871f)
                curveTo(176.828f, 48.875f, 181.339f, 53.039f, 181.339f, 61.171f)
                curveTo(181.339f, 69.303f, 176.828f, 78.325f, 171.264f, 81.321f)
                curveTo(165.7f, 84.317f, 161.189f, 80.153f, 161.189f, 72.021f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(83.074f, 71.151f)
                lineTo(110.274f, 82.351f)
                lineTo(90.774f, 87.151f)
                lineTo(63.574f, 75.951f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(169.416f, 67.688f)
                curveTo(169.416f, 66.114f, 170.289f, 64.368f, 171.366f, 63.788f)
                curveTo(172.443f, 63.208f, 173.316f, 64.014f, 173.316f, 65.588f)
                curveTo(173.316f, 67.162f, 172.443f, 68.908f, 171.366f, 69.488f)
                curveTo(170.289f, 70.068f, 169.416f, 69.262f, 169.416f, 67.688f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(159.915f, 73.967f)
                curveTo(159.915f, 63.736f, 165.59f, 52.386f, 172.59f, 48.617f)
                curveTo(179.59f, 44.848f, 185.265f, 50.086f, 185.265f, 60.317f)
                curveTo(185.265f, 70.548f, 179.59f, 81.898f, 172.59f, 85.667f)
                curveTo(165.59f, 89.436f, 159.915f, 84.198f, 159.915f, 73.967f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(159.912f, 59.561f)
                lineTo(160.788f, 64.643f)
                lineTo(143.888f, 79.443f)
                lineTo(143.012f, 74.361f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(154.812f, 57.461f)
                lineTo(159.912f, 59.561f)
                lineTo(143.012f, 74.361f)
                lineTo(137.912f, 72.261f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(156.337f, 59.702f)
                lineTo(159.262f, 58.127f)
                lineTo(188.162f, 70.027f)
                lineTo(185.238f, 71.602f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(156.337f, 63.977f)
                lineTo(156.337f, 59.702f)
                lineTo(185.238f, 71.602f)
                lineTo(185.238f, 75.877f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(179.499f, 78.953f)
                lineTo(178.053f, 80.917f)
                lineTo(176.513f, 82.645f)
                lineTo(174.906f, 84.108f)
                lineTo(173.26f, 85.28f)
                lineTo(171.602f, 86.143f)
                lineTo(169.961f, 86.68f)
                lineTo(168.365f, 86.883f)
                lineTo(166.842f, 86.748f)
                lineTo(165.416f, 86.277f)
                lineTo(164.114f, 85.48f)
                lineTo(162.956f, 84.368f)
                lineTo(161.963f, 82.962f)
                lineTo(161.153f, 81.285f)
                lineTo(160.537f, 79.366f)
                lineTo(160.128f, 77.238f)
                lineTo(159.933f, 74.937f)
                lineTo(159.954f, 72.503f)
                lineTo(160.191f, 69.977f)
                lineTo(160.64f, 67.402f)
                lineTo(161.293f, 64.823f)
                lineTo(162.14f, 62.284f)
                lineTo(163.166f, 59.828f)
                lineTo(164.353f, 57.497f)
                lineTo(165.681f, 55.331f)
                lineTo(170.101f, 57.151f)
                lineTo(168.773f, 59.317f)
                lineTo(167.586f, 61.648f)
                lineTo(166.56f, 64.104f)
                lineTo(165.713f, 66.643f)
                lineTo(165.06f, 69.222f)
                lineTo(164.611f, 71.797f)
                lineTo(164.374f, 74.323f)
                lineTo(164.353f, 76.757f)
                lineTo(164.548f, 79.058f)
                lineTo(164.957f, 81.186f)
                lineTo(165.573f, 83.105f)
                lineTo(166.383f, 84.782f)
                lineTo(167.376f, 86.188f)
                lineTo(168.534f, 87.3f)
                lineTo(169.836f, 88.097f)
                lineTo(171.262f, 88.568f)
                lineTo(172.785f, 88.703f)
                lineTo(174.381f, 88.5f)
                lineTo(176.022f, 87.963f)
                lineTo(177.68f, 87.1f)
                lineTo(179.326f, 85.928f)
                lineTo(180.933f, 84.465f)
                lineTo(182.473f, 82.737f)
                lineTo(183.919f, 80.773f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(164.335f, 75.787f)
                curveTo(164.335f, 65.556f, 170.01f, 54.206f, 177.01f, 50.437f)
                curveTo(184.01f, 46.668f, 189.685f, 51.906f, 189.685f, 62.137f)
                curveTo(189.685f, 72.368f, 184.01f, 83.718f, 177.01f, 87.487f)
                curveTo(170.01f, 91.256f, 164.335f, 86.018f, 164.335f, 75.787f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(165.412f, 75.304f)
                curveTo(165.412f, 65.86f, 170.65f, 55.383f, 177.112f, 51.904f)
                curveTo(183.574f, 48.425f, 188.812f, 53.26f, 188.812f, 62.704f)
                curveTo(188.812f, 72.148f, 183.574f, 82.625f, 177.112f, 86.104f)
                curveTo(170.65f, 89.583f, 165.412f, 84.748f, 165.412f, 75.304f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(167.139f, 74.471f)
                curveTo(167.139f, 66.339f, 171.65f, 57.317f, 177.214f, 54.321f)
                curveTo(182.778f, 51.325f, 187.289f, 55.489f, 187.289f, 63.621f)
                curveTo(187.289f, 71.753f, 182.778f, 80.775f, 177.214f, 83.771f)
                curveTo(171.65f, 86.767f, 167.139f, 82.603f, 167.139f, 74.471f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(175.366f, 70.138f)
                curveTo(175.366f, 68.564f, 176.239f, 66.818f, 177.316f, 66.238f)
                curveTo(178.393f, 65.658f, 179.266f, 66.464f, 179.266f, 68.038f)
                curveTo(179.266f, 69.612f, 178.393f, 71.358f, 177.316f, 71.938f)
                curveTo(176.239f, 72.518f, 175.366f, 71.712f, 175.366f, 70.138f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(178.7785f, 66.163f)
                lineTo(188.162f, 70.027f)
                lineTo(185.238f, 71.602f)
                lineTo(175.8535f, 67.738f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(175.8535f, 67.738f)
                lineTo(185.238f, 71.602f)
                lineTo(185.238f, 75.877f)
                lineTo(175.8535f, 72.013f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(41.225f, 41.252f)
                lineTo(44.475f, 39.502f)
                lineTo(47.375f, 44.402f)
                lineTo(44.125f, 46.152f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(123.227f, 126.466f)
                lineTo(120.523f, 125.288f)
                lineTo(124.773f, 127.038f)
                lineTo(127.477f, 128.216f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(41.225f, 46.002f)
                lineTo(41.225f, 41.252f)
                lineTo(44.125f, 46.152f)
                lineTo(44.125f, 50.902f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(127.477f, 128.216f)
                lineTo(124.773f, 127.038f)
                lineTo(141.673f, 80.888f)
                lineTo(144.377f, 82.066f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(140.034f, 73.194f)
                lineTo(145.984f, 75.644f)
                lineTo(141.766f, 80.41f)
                lineTo(135.816f, 77.96f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(137.912f, 72.261f)
                lineTo(143.012f, 74.361f)
                lineTo(143.888f, 79.443f)
                lineTo(138.788f, 77.343f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(124.773f, 127.038f)
                lineTo(120.523f, 125.288f)
                lineTo(137.423f, 79.138f)
                lineTo(141.673f, 80.888f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(110.274f, 82.351f)
                lineTo(109.126f, 91.353f)
                lineTo(89.626f, 96.153f)
                lineTo(90.774f, 87.151f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(47.375f, 49.152f)
                lineTo(44.125f, 50.902f)
                lineTo(44.125f, 46.152f)
                lineTo(47.375f, 44.402f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(35.608f, 109.355f)
                lineTo(36.192f, 109.04f)
                lineTo(53.193f, 116.04f)
                lineTo(52.608f, 116.355f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(35.608f, 110.115f)
                lineTo(35.608f, 109.355f)
                lineTo(52.608f, 116.355f)
                lineTo(52.608f, 117.115f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(133.85f, 71.952f)
                lineTo(137.75f, 69.852f)
                lineTo(147.95f, 74.052f)
                lineTo(144.05f, 76.152f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(178.373f, 68.742f)
                lineTo(181.428f, 67.097f)
                lineTo(188.227f, 69.897f)
                lineTo(185.173f, 71.542f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(133.85f, 77.652f)
                lineTo(133.85f, 71.952f)
                lineTo(144.05f, 76.152f)
                lineTo(144.05f, 81.852f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(178.373f, 73.207f)
                lineTo(178.373f, 68.742f)
                lineTo(185.173f, 71.542f)
                lineTo(185.173f, 76.007f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(188.162f, 74.302f)
                lineTo(185.238f, 75.877f)
                lineTo(185.238f, 71.602f)
                lineTo(188.162f, 70.027f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(188.227f, 74.362f)
                lineTo(185.173f, 76.007f)
                lineTo(185.173f, 71.542f)
                lineTo(188.227f, 69.897f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(147.95f, 79.752f)
                lineTo(144.05f, 81.852f)
                lineTo(144.05f, 76.152f)
                lineTo(147.95f, 74.052f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(156.725f, 107.302f)
                lineTo(162.675f, 109.752f)
                lineTo(91.175f, 148.252f)
                lineTo(85.225f, 145.802f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(142.92f, 79.772f)
                curveTo(142.92f, 77.149f, 144.375f, 74.239f, 146.17f, 73.272f)
                curveTo(147.965f, 72.306f, 149.42f, 73.649f, 149.42f, 76.272f)
                curveTo(149.42f, 78.895f, 147.965f, 81.806f, 146.17f, 82.772f)
                curveTo(144.375f, 83.739f, 142.92f, 82.395f, 142.92f, 79.772f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(58.675f, 123.752f)
                lineTo(64.625f, 126.202f)
                lineTo(64.625f, 132.852f)
                lineTo(58.675f, 130.402f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(162.675f, 109.752f)
                lineTo(162.675f, 116.402f)
                lineTo(91.175f, 154.902f)
                lineTo(91.175f, 148.252f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(145.125f, 78.827f)
                curveTo(145.125f, 77.778f, 145.707f, 76.614f, 146.425f, 76.227f)
                curveTo(147.143f, 75.84f, 147.725f, 76.378f, 147.725f, 77.427f)
                curveTo(147.725f, 78.476f, 147.143f, 79.64f, 146.425f, 80.027f)
                curveTo(145.707f, 80.414f, 145.125f, 79.876f, 145.125f, 78.827f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(63.574f, 75.951f)
                lineTo(90.774f, 87.151f)
                lineTo(89.626f, 96.153f)
                lineTo(62.426f, 84.953f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(29.95f, 108.552f)
                lineTo(33.85f, 106.452f)
                lineTo(93.35f, 130.952f)
                lineTo(89.45f, 133.052f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(29.95f, 114.252f)
                lineTo(29.95f, 108.552f)
                lineTo(89.45f, 133.052f)
                lineTo(89.45f, 138.752f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(36.471f, 107.46f)
                lineTo(59.421f, 116.91f)
                lineTo(45.121f, 117.96f)
                lineTo(22.171f, 108.51f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(31.057f, 109.715f)
                lineTo(31.642f, 109.4f)
                lineTo(48.643f, 116.4f)
                lineTo(48.057f, 116.715f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(31.057f, 110.475f)
                lineTo(31.057f, 109.715f)
                lineTo(48.057f, 116.715f)
                lineTo(48.057f, 117.475f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(43.328f, 43.947f)
                lineTo(48.172f, 45.942f)
                lineTo(37.773f, 51.542f)
                lineTo(32.927f, 49.547f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(48.172f, 45.942f)
                lineTo(48.172f, 51.357f)
                lineTo(37.773f, 56.957f)
                lineTo(37.773f, 51.542f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(48.643f, 117.16f)
                lineTo(48.057f, 117.475f)
                lineTo(48.057f, 116.715f)
                lineTo(48.643f, 116.4f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(32.927f, 49.547f)
                lineTo(37.773f, 51.542f)
                lineTo(37.773f, 56.957f)
                lineTo(32.927f, 54.962f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(59.421f, 116.91f)
                lineTo(58.829f, 119.944f)
                lineTo(44.529f, 120.994f)
                lineTo(45.121f, 117.96f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(26.507f, 110.075f)
                lineTo(27.093f, 109.76f)
                lineTo(44.093f, 116.76f)
                lineTo(43.508f, 117.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(32.108f, 49.637f)
                lineTo(37.293f, 51.772f)
                lineTo(37.293f, 57.567f)
                lineTo(32.108f, 55.432f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(26.507f, 110.835f)
                lineTo(26.507f, 110.075f)
                lineTo(43.508f, 117.075f)
                lineTo(43.508f, 117.835f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(33.407f, 48.937f)
                lineTo(38.593f, 51.072f)
                lineTo(37.293f, 51.772f)
                lineTo(32.108f, 49.637f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(38.593f, 51.072f)
                lineTo(38.593f, 56.867f)
                lineTo(37.293f, 57.567f)
                lineTo(37.293f, 51.772f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(22.171f, 108.51f)
                lineTo(45.121f, 117.96f)
                lineTo(44.529f, 120.994f)
                lineTo(21.579f, 111.544f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(6.025f, 115.352f)
                lineTo(10.575f, 112.902f)
                lineTo(93.875f, 147.202f)
                lineTo(89.325f, 149.652f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(44.093f, 117.52f)
                lineTo(43.508f, 117.835f)
                lineTo(43.508f, 117.075f)
                lineTo(44.093f, 116.76f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(6.025f, 122.002f)
                lineTo(6.025f, 115.352f)
                lineTo(89.325f, 149.652f)
                lineTo(89.325f, 156.302f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(139.17f, 71.775f)
                lineTo(144.27f, 73.875f)
                lineTo(117.62f, 70.175f)
                lineTo(112.52f, 68.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(66.207f, 121.955f)
                lineTo(66.793f, 121.64f)
                lineTo(83.793f, 128.64f)
                lineTo(83.207f, 128.955f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(144.27f, 73.875f)
                lineTo(142.63f, 79.929f)
                lineTo(115.98f, 76.229f)
                lineTo(117.62f, 70.175f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(66.207f, 122.715f)
                lineTo(66.207f, 121.955f)
                lineTo(83.207f, 128.955f)
                lineTo(83.207f, 129.715f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(93.35f, 136.652f)
                lineTo(89.45f, 138.752f)
                lineTo(89.45f, 133.052f)
                lineTo(93.35f, 130.952f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(67.071f, 120.06f)
                lineTo(90.021f, 129.51f)
                lineTo(75.721f, 130.56f)
                lineTo(52.771f, 121.11f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(61.657f, 122.315f)
                lineTo(62.243f, 122f)
                lineTo(79.243f, 129f)
                lineTo(78.657f, 129.315f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(61.657f, 123.075f)
                lineTo(61.657f, 122.315f)
                lineTo(78.657f, 129.315f)
                lineTo(78.657f, 130.075f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(79.243f, 129.76f)
                lineTo(78.657f, 130.075f)
                lineTo(78.657f, 129.315f)
                lineTo(79.243f, 129f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(90.021f, 129.51f)
                lineTo(89.429f, 132.544f)
                lineTo(75.129f, 133.594f)
                lineTo(75.721f, 130.56f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(57.107f, 122.675f)
                lineTo(57.692f, 122.36f)
                lineTo(74.692f, 129.36f)
                lineTo(74.108f, 129.675f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(57.107f, 123.435f)
                lineTo(57.107f, 122.675f)
                lineTo(74.108f, 129.675f)
                lineTo(74.108f, 130.435f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(52.771f, 121.11f)
                lineTo(75.721f, 130.56f)
                lineTo(75.129f, 133.594f)
                lineTo(52.179f, 124.144f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(74.692f, 130.12f)
                lineTo(74.108f, 130.435f)
                lineTo(74.108f, 129.675f)
                lineTo(74.692f, 129.36f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(112.52f, 68.075f)
                lineTo(117.62f, 70.175f)
                lineTo(115.98f, 76.229f)
                lineTo(110.88f, 74.129f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(115.875f, 68.902f)
                lineTo(115.875f, 73.652f)
                lineTo(112.625f, 75.402f)
                lineTo(112.625f, 70.652f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N200),
            ) {
                moveTo(115.875f, 68.902f)
                lineTo(112.625f, 70.652f)
                lineTo(101.925f, 69.952f)
                lineTo(105.175f, 68.202f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(80.5f, 152.977f)
                lineTo(85.7f, 150.177f)
                lineTo(95.9f, 154.377f)
                lineTo(90.7f, 157.177f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(112.625f, 70.652f)
                lineTo(112.625f, 75.402f)
                lineTo(101.925f, 74.702f)
                lineTo(101.925f, 69.952f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N700),
            ) {
                moveTo(85.225f, 145.802f)
                lineTo(91.175f, 148.252f)
                lineTo(91.175f, 154.902f)
                lineTo(85.225f, 152.452f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(80.5f, 155.827f)
                lineTo(80.5f, 152.977f)
                lineTo(90.7f, 157.177f)
                lineTo(90.7f, 160.027f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(95.9f, 157.227f)
                lineTo(90.7f, 160.027f)
                lineTo(90.7f, 157.177f)
                lineTo(95.9f, 154.377f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralLight.N300),
            ) {
                moveTo(93.875f, 153.852f)
                lineTo(89.325f, 156.302f)
                lineTo(89.325f, 149.652f)
                lineTo(93.875f, 147.202f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N450),
            ) {
                moveTo(101.127f, 67.747f)
                lineTo(105.972f, 69.742f)
                lineTo(95.573f, 75.342f)
                lineTo(90.727f, 73.347f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N200),
            ) {
                moveTo(105.972f, 69.742f)
                lineTo(105.972f, 75.157f)
                lineTo(95.573f, 80.757f)
                lineTo(95.573f, 75.342f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
            ) {
                moveTo(90.727f, 73.347f)
                lineTo(95.573f, 75.342f)
                lineTo(95.573f, 80.757f)
                lineTo(90.727f, 78.762f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N400),
            ) {
                moveTo(89.907f, 73.437f)
                lineTo(95.093f, 75.572f)
                lineTo(95.093f, 81.367f)
                lineTo(89.907f, 79.232f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N500),
            ) {
                moveTo(91.207f, 72.737f)
                lineTo(96.392f, 74.872f)
                lineTo(95.093f, 75.572f)
                lineTo(89.907f, 73.437f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N250),
            ) {
                moveTo(96.392f, 74.872f)
                lineTo(96.392f, 80.667f)
                lineTo(95.093f, 81.367f)
                lineTo(95.093f, 75.572f)
                close()
            }
        }.build()
        return _PlateLoadedSeatedDip!!
    }
