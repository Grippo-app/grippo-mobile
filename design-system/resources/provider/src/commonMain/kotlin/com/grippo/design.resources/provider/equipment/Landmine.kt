package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _Landmine: ImageVector? = null

public val Landmine: ImageVector
    get() {
        if (_Landmine != null) {
            return _Landmine!!
        }
        _Landmine = ImageVector.Builder(
            name = "Landmine",
            defaultWidth = 256.dp,
            defaultHeight = 216.dp,
            viewportWidth = 256f,
            viewportHeight = 216f,
        ).apply {
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(12f, 174f)
                lineTo(70f, 201f)
                lineTo(70f, 209f)
                lineTo(12f, 182f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(70f, 201f)
                lineTo(115f, 175f)
                lineTo(115f, 183f)
                lineTo(70f, 209f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(12f, 174f)
                lineTo(57f, 148f)
                lineTo(115f, 175f)
                lineTo(70f, 201f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N800)) {
                moveTo(16f, 174f)
                lineTo(57f, 150f)
                lineTo(110f, 175f)
                lineTo(70f, 198f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(19f, 174f)
                lineTo(57f, 152f)
                lineTo(106f, 175f)
                lineTo(70f, 195f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(38.2f, 175.9f)
                curveTo(36.93f, 176.618f, 34.02f, 176.349f, 31.7f, 175.3f)
                curveTo(29.38f, 174.251f, 28.53f, 172.818f, 29.8f, 172.1f)
                curveTo(31.07f, 171.382f, 33.98f, 171.651f, 36.3f, 172.7f)
                curveTo(38.62f, 173.749f, 39.47f, 175.182f, 38.2f, 175.9f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(36.7f, 174.8f)
                curveTo(35.872f, 175.242f, 33.991f, 175.063f, 32.5f, 174.4f)
                curveTo(31.009f, 173.737f, 30.472f, 172.842f, 31.3f, 172.4f)
                curveTo(32.128f, 171.958f, 34.009f, 172.137f, 35.5f, 172.8f)
                curveTo(36.991f, 173.463f, 37.528f, 174.358f, 36.7f, 174.8f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(63.2f, 160.9f)
                curveTo(61.93f, 161.618f, 59.02f, 161.349f, 56.7f, 160.3f)
                curveTo(54.38f, 159.251f, 53.53f, 157.818f, 54.8f, 157.1f)
                curveTo(56.07f, 156.382f, 58.98f, 156.651f, 61.3f, 157.7f)
                curveTo(63.62f, 158.749f, 64.47f, 160.182f, 63.2f, 160.9f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(61.7f, 159.8f)
                curveTo(60.872f, 160.242f, 58.991f, 160.063f, 57.5f, 159.4f)
                curveTo(56.009f, 158.737f, 55.472f, 157.842f, 56.3f, 157.4f)
                curveTo(57.128f, 156.958f, 59.009f, 157.137f, 60.5f, 157.8f)
                curveTo(61.991f, 158.463f, 62.528f, 159.358f, 61.7f, 159.8f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(98.2f, 177.9f)
                curveTo(96.93f, 178.618f, 94.02f, 178.349f, 91.7f, 177.3f)
                curveTo(89.38f, 176.251f, 88.53f, 174.818f, 89.8f, 174.1f)
                curveTo(91.07f, 173.382f, 93.98f, 173.651f, 96.3f, 174.7f)
                curveTo(98.62f, 175.749f, 99.47f, 177.182f, 98.2f, 177.9f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(96.7f, 176.8f)
                curveTo(95.872f, 177.242f, 93.991f, 177.063f, 92.5f, 176.4f)
                curveTo(91.009f, 175.737f, 90.472f, 174.842f, 91.3f, 174.4f)
                curveTo(92.128f, 173.958f, 94.009f, 174.137f, 95.5f, 174.8f)
                curveTo(96.991f, 175.463f, 97.528f, 176.358f, 96.7f, 176.8f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(74.2f, 191.9f)
                curveTo(72.93f, 192.618f, 70.02f, 192.349f, 67.7f, 191.3f)
                curveTo(65.38f, 190.251f, 64.53f, 188.818f, 65.8f, 188.1f)
                curveTo(67.07f, 187.382f, 69.98f, 187.651f, 72.3f, 188.7f)
                curveTo(74.62f, 189.749f, 75.47f, 191.182f, 74.2f, 191.9f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(72.7f, 190.8f)
                curveTo(71.872f, 191.242f, 69.991f, 191.063f, 68.5f, 190.4f)
                curveTo(67.009f, 189.737f, 66.472f, 188.842f, 67.3f, 188.4f)
                curveTo(68.128f, 187.958f, 70.009f, 188.137f, 71.5f, 188.8f)
                curveTo(72.991f, 189.463f, 73.528f, 190.358f, 72.7f, 190.8f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(45f, 164f)
                curveTo(45f, 158.5f, 53.5f, 154f, 64f, 154f)
                curveTo(74.5f, 154f, 83f, 158.5f, 83f, 164f)
                lineTo(83f, 173f)
                curveTo(83f, 178.5f, 74.5f, 183f, 64f, 183f)
                curveTo(53.5f, 183f, 45f, 178.5f, 45f, 173f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(64f, 165f)
                lineTo(83f, 164f)
                lineTo(83f, 173f)
                curveTo(83f, 178.5f, 74.5f, 183f, 64f, 183f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(83f, 164f)
                curveTo(83f, 169.523f, 74.493f, 174f, 64f, 174f)
                curveTo(53.507f, 174f, 45f, 169.523f, 45f, 164f)
                curveTo(45f, 158.477f, 53.507f, 154f, 64f, 154f)
                curveTo(74.493f, 154f, 83f, 158.477f, 83f, 164f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(79.5f, 163f)
                curveTo(79.5f, 167.253f, 72.56f, 170.7f, 64f, 170.7f)
                curveTo(55.44f, 170.7f, 48.5f, 167.253f, 48.5f, 163f)
                curveTo(48.5f, 158.747f, 55.44f, 155.3f, 64f, 155.3f)
                curveTo(72.56f, 155.3f, 79.5f, 158.747f, 79.5f, 163f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(54f, 161f)
                lineTo(62f, 156f)
                lineTo(81f, 165f)
                lineTo(73f, 170f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(54f, 161f)
                lineTo(73f, 170f)
                lineTo(73f, 175f)
                lineTo(54f, 166f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(73f, 170f)
                lineTo(81f, 165f)
                lineTo(81f, 170f)
                lineTo(73f, 175f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(53f, 146f)
                lineTo(59f, 142f)
                lineTo(65f, 145f)
                lineTo(65f, 168f)
                lineTo(59f, 172f)
                lineTo(53f, 169f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(53f, 146f)
                lineTo(59f, 149f)
                lineTo(59f, 172f)
                lineTo(53f, 169f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(53f, 146f)
                lineTo(59f, 142f)
                lineTo(65f, 145f)
                lineTo(59f, 149f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(63.5f, 154.7f)
                curveTo(63.5f, 157.461f, 61.485f, 158.939f, 59f, 158f)
                curveTo(56.515f, 157.061f, 54.5f, 154.061f, 54.5f, 151.3f)
                curveTo(54.5f, 148.539f, 56.515f, 147.061f, 59f, 148f)
                curveTo(61.485f, 148.939f, 63.5f, 151.939f, 63.5f, 154.7f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(60.112f, 148.664f)
                lineTo(95.112f, 121.664f)
                lineTo(104.888f, 134.336f)
                lineTo(69.888f, 161.336f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N550)) {
                moveTo(60.112f, 148.664f)
                lineTo(95.112f, 121.664f)
                lineTo(98.778f, 126.416f)
                lineTo(63.778f, 153.416f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(66.833f, 157.376f)
                lineTo(101.833f, 130.376f)
                lineTo(104.888f, 134.336f)
                lineTo(69.888f, 161.336f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(104.888f, 134.336f)
                curveTo(106.419f, 133.155f, 105.472f, 129.361f, 102.772f, 125.862f)
                curveTo(100.072f, 122.362f, 96.643f, 120.483f, 95.112f, 121.664f)
                curveTo(93.581f, 122.845f, 94.528f, 126.639f, 97.228f, 130.138f)
                curveTo(99.928f, 133.638f, 103.357f, 135.517f, 104.888f, 134.336f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N200)) {
                moveTo(103.483f, 132.514f)
                curveTo(104.576f, 131.671f, 103.903f, 128.966f, 101.98f, 126.472f)
                curveTo(100.057f, 123.979f, 97.611f, 122.642f, 96.517f, 123.486f)
                curveTo(95.424f, 124.329f, 96.097f, 127.034f, 98.02f, 129.528f)
                curveTo(99.943f, 132.021f, 102.389f, 133.358f, 103.483f, 132.514f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(97.862f, 125.228f)
                lineTo(202.862f, 44.228f)
                lineTo(207.138f, 49.772f)
                lineTo(102.138f, 130.772f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(97.862f, 125.228f)
                lineTo(202.862f, 44.228f)
                lineTo(204.756f, 46.683f)
                lineTo(99.756f, 127.683f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(100.916f, 129.188f)
                lineTo(205.917f, 48.188f)
                lineTo(207.138f, 49.772f)
                lineTo(102.138f, 130.772f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(126.5f, 103f)
                lineTo(153.5f, 82.2f)
                lineTo(157.4f, 87.3f)
                lineTo(130.4f, 108.1f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(127.9f, 102f)
                lineTo(153.5f, 82.2f)
                lineTo(155.1f, 84.3f)
                lineTo(129.5f, 104.1f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(201.884f, 42.961f)
                lineTo(240.884f, 12.961f)
                lineTo(247.116f, 21.039f)
                lineTo(208.116f, 51.039f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(201.884f, 42.961f)
                lineTo(240.884f, 12.961f)
                lineTo(243.206f, 15.97f)
                lineTo(204.206f, 45.97f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(204.206f, 45.97f)
                lineTo(243.206f, 15.97f)
                lineTo(245.222f, 18.584f)
                lineTo(206.222f, 48.584f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(247.116f, 21.039f)
                curveTo(247.903f, 20.432f, 247.147f, 18.131f, 245.426f, 15.9f)
                curveTo(243.705f, 13.669f, 241.671f, 12.353f, 240.884f, 12.961f)
                curveTo(240.097f, 13.568f, 240.853f, 15.869f, 242.574f, 18.1f)
                curveTo(244.295f, 20.331f, 246.329f, 21.647f, 247.116f, 21.039f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(246.138f, 19.772f)
                curveTo(246.62f, 19.401f, 246.052f, 17.859f, 244.871f, 16.328f)
                curveTo(243.69f, 14.797f, 242.343f, 13.857f, 241.862f, 14.228f)
                curveTo(241.38f, 14.599f, 241.948f, 16.141f, 243.129f, 17.672f)
                curveTo(244.31f, 19.203f, 245.657f, 20.143f, 246.138f, 19.772f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(206.399f, 54.902f)
                curveTo(207.799f, 53.823f, 206.964f, 50.394f, 204.534f, 47.245f)
                curveTo(202.105f, 44.095f, 199.001f, 42.418f, 197.601f, 43.498f)
                curveTo(196.201f, 44.577f, 197.036f, 48.006f, 199.466f, 51.155f)
                curveTo(201.895f, 54.305f, 204.999f, 55.982f, 206.399f, 54.902f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(209.599f, 52.402f)
                curveTo(210.999f, 51.323f, 210.164f, 47.894f, 207.734f, 44.745f)
                curveTo(205.305f, 41.595f, 202.201f, 39.918f, 200.801f, 40.998f)
                curveTo(199.401f, 42.077f, 200.236f, 45.506f, 202.666f, 48.655f)
                curveTo(205.095f, 51.805f, 208.199f, 53.482f, 209.599f, 52.402f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(227.608f, 66.676f)
                curveTo(232.42f, 62.964f, 228.66f, 50.026f, 219.212f, 37.779f)
                curveTo(209.764f, 25.532f, 198.204f, 18.612f, 193.392f, 22.324f)
                curveTo(188.58f, 26.036f, 192.34f, 38.974f, 201.788f, 51.221f)
                curveTo(211.236f, 63.468f, 222.796f, 70.388f, 227.608f, 66.676f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N200)) {
                moveTo(193.4f, 22.3f)
                lineTo(198.9f, 17.8f)
                lineTo(233.1f, 62.2f)
                lineTo(227.6f, 66.7f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(233.108f, 62.176f)
                curveTo(237.92f, 58.464f, 234.16f, 45.526f, 224.712f, 33.279f)
                curveTo(215.264f, 21.032f, 203.704f, 14.112f, 198.892f, 17.824f)
                curveTo(194.08f, 21.536f, 197.84f, 34.474f, 207.288f, 46.721f)
                curveTo(216.736f, 58.968f, 228.296f, 65.888f, 233.108f, 62.176f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(230.964f, 58.808f)
                curveTo(234.944f, 55.737f, 231.606f, 44.738f, 223.507f, 34.24f)
                curveTo(215.408f, 23.742f, 205.616f, 17.721f, 201.636f, 20.792f)
                curveTo(197.656f, 23.863f, 200.994f, 34.862f, 209.093f, 45.36f)
                curveTo(217.192f, 55.858f, 226.984f, 61.879f, 230.964f, 58.808f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(229.064f, 55.757f)
                curveTo(232.432f, 53.158f, 229.582f, 43.818f, 222.698f, 34.895f)
                curveTo(215.815f, 25.972f, 207.504f, 20.845f, 204.136f, 23.443f)
                curveTo(200.768f, 26.042f, 203.618f, 35.382f, 210.502f, 44.305f)
                curveTo(217.385f, 53.228f, 225.696f, 58.355f, 229.064f, 55.757f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(220.755f, 44.986f)
                curveTo(222.417f, 43.703f, 221.904f, 40.253f, 219.61f, 37.278f)
                curveTo(217.315f, 34.304f, 214.107f, 32.932f, 212.445f, 34.214f)
                curveTo(210.783f, 35.497f, 211.296f, 38.947f, 213.59f, 41.922f)
                curveTo(215.885f, 44.896f, 219.093f, 46.268f, 220.755f, 44.986f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N200)) {
                moveTo(219.655f, 43.56f)
                curveTo(220.792f, 42.683f, 220.346f, 40.198f, 218.659f, 38.011f)
                curveTo(216.972f, 35.824f, 214.682f, 34.763f, 213.545f, 35.64f)
                curveTo(212.408f, 36.517f, 212.854f, 39.002f, 214.541f, 41.189f)
                curveTo(216.228f, 43.376f, 218.518f, 44.437f, 219.655f, 43.56f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(213.728f, 35.878f)
                lineTo(241.128f, 13.278f)
                lineTo(246.872f, 20.722f)
                lineTo(219.472f, 43.322f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(213.728f, 35.878f)
                lineTo(241.128f, 13.278f)
                lineTo(243.206f, 15.97f)
                lineTo(215.806f, 38.57f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(215.806f, 38.57f)
                lineTo(243.206f, 15.97f)
                lineTo(245.222f, 18.584f)
                lineTo(217.822f, 41.184f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(246.872f, 20.722f)
                curveTo(247.659f, 20.115f, 247.012f, 17.956f, 245.426f, 15.9f)
                curveTo(243.84f, 13.844f, 241.916f, 12.67f, 241.128f, 13.278f)
                curveTo(240.341f, 13.885f, 240.988f, 16.044f, 242.574f, 18.1f)
                curveTo(244.16f, 20.156f, 246.084f, 21.33f, 246.872f, 20.722f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(245.894f, 19.455f)
                curveTo(246.375f, 19.084f, 245.917f, 17.684f, 244.871f, 16.328f)
                curveTo(243.825f, 14.972f, 242.587f, 14.174f, 242.106f, 14.545f)
                curveTo(241.625f, 14.916f, 242.083f, 16.316f, 243.129f, 17.672f)
                curveTo(244.175f, 19.028f, 245.413f, 19.826f, 245.894f, 19.455f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N300)) {
                moveTo(68f, 154f)
                lineTo(74f, 150f)
                lineTo(80f, 153f)
                lineTo(80f, 175f)
                lineTo(74f, 179f)
                lineTo(68f, 176f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(68f, 154f)
                lineTo(74f, 157f)
                lineTo(74f, 179f)
                lineTo(68f, 176f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200)) {
                moveTo(68f, 154f)
                lineTo(74f, 150f)
                lineTo(80f, 153f)
                lineTo(74f, 157f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(78.8f, 163.8f)
                curveTo(78.8f, 166.782f, 76.651f, 168.394f, 74f, 167.4f)
                curveTo(71.349f, 166.406f, 69.2f, 163.182f, 69.2f, 160.2f)
                curveTo(69.2f, 157.218f, 71.349f, 155.606f, 74f, 156.6f)
                curveTo(76.651f, 157.594f, 78.8f, 160.818f, 78.8f, 163.8f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(77.1f, 163.2f)
                curveTo(77.1f, 165.133f, 75.712f, 166.163f, 74f, 165.5f)
                curveTo(72.288f, 164.837f, 70.9f, 162.733f, 70.9f, 160.8f)
                curveTo(70.9f, 158.867f, 72.288f, 157.837f, 74f, 158.5f)
                curveTo(75.712f, 159.163f, 77.1f, 161.267f, 77.1f, 163.2f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(72.4f, 159.6f)
                lineTo(74f, 159f)
                lineTo(75.6f, 161f)
                lineTo(75.6f, 164f)
                lineTo(74f, 164.7f)
                lineTo(72.4f, 162.7f)
                close()
            }
        }.build()
        return _Landmine!!
    }

