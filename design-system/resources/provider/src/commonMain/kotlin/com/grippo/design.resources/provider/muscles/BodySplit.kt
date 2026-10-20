package com.grippo.design.resources.provider.muscles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public fun bodySplit(
    colors: MuscleColorPreset
): ImageVector {

    val outlineColor: Color = colors.outline
    val backgroundFront: Color = colors.backgroundFront
    val backgroundBack: Color = colors.backgroundBack
    val teresMajor: Color = colors.teresMajor
    val rhomboids: Color = colors.rhomboids
    val latissimus: Color = colors.latissimus
    val trapezius: Color = colors.trapezius
    val triceps: Color = colors.triceps
    val biceps: Color = colors.biceps
    val forearmFront: Color = colors.forearmFront
    val forearmBack: Color = colors.forearmBack
    val posteriorDeltoid: Color = colors.posteriorDeltoid
    val lateralDeltoidFront: Color = colors.lateralDeltoid
    val anteriorDeltoid: Color = colors.anteriorDeltoid
    val rectusAbdominis: Color = colors.rectusAbdominis
    val obliquesAbdominis: Color = colors.obliquesAbdominis
    val pectoralisMajorAbdominal: Color = colors.pectoralisMajorAbdominal
    val pectoralisMajorClavicular: Color = colors.pectoralisMajorClavicular
    val pectoralisMajorSternocostal: Color = colors.pectoralisMajorSternocostal
    val other: Color = colors.other

    return ImageVector.Builder(
        name = "Bodysplit",
        defaultWidth = 419.dp,
        defaultHeight = 430.dp,
        viewportWidth = 419f,
        viewportHeight = 430f
    ).apply {
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundFront),
            strokeLineWidth = 1f
        ) {
            moveTo(145.69f, 346.77f)
            lineTo(149.5f, 324.5f)
            curveTo(149.5f, 324.5f, 154.19f, 308.3f, 154.19f, 299.5f)
            verticalLineTo(262.27f)
            curveTo(147.69f, 250.1f, 134.79f, 224.97f, 135.19f, 221.77f)
            curveTo(132.85f, 232.27f, 126.49f, 254.57f, 119.69f, 259.77f)
            curveTo(119.02f, 269.43f, 112.19f, 295.07f, 90.19f, 320.27f)
            curveTo(68.19f, 345.47f, 61.69f, 354.43f, 61.19f, 355.77f)
            lineTo(54.69f, 373.27f)
            curveTo(53.52f, 382.77f, 48.59f, 405.27f, 38.19f, 419.27f)
            curveTo(35.52f, 420.77f, 31.09f, 421.77f, 34.69f, 413.77f)
            curveTo(38.29f, 405.77f, 38.85f, 403.1f, 38.69f, 402.77f)
            curveTo(38.69f, 401.93f, 38.29f, 400.77f, 36.69f, 402.77f)
            curveTo(34.19f, 409.77f, 28.59f, 424.37f, 26.19f, 426.77f)
            curveTo(23.19f, 429.77f, 20.19f, 424.27f, 21.69f, 422.77f)
            curveTo(22.89f, 421.57f, 28.85f, 407.27f, 31.69f, 400.27f)
            curveTo(32.19f, 398.77f, 32.59f, 396.47f, 30.19f, 399.27f)
            lineTo(18.19f, 425.77f)
            curveTo(15.52f, 426.27f, 10.69f, 426.17f, 12.69f, 421.77f)
            curveTo(14.69f, 417.37f, 22.52f, 401.27f, 26.19f, 393.77f)
            curveTo(26.19f, 392.77f, 25.79f, 391.27f, 24.19f, 393.27f)
            lineTo(12.69f, 416.27f)
            curveTo(10.69f, 417.27f, 7.09f, 417.87f, 8.69f, 412.27f)
            curveTo(10.29f, 406.67f, 16.69f, 390.27f, 19.69f, 382.77f)
            lineTo(23.69f, 372.27f)
            curveTo(24.02f, 371.93f, 24.39f, 371.37f, 23.19f, 371.77f)
            curveTo(21.69f, 372.27f, 11.69f, 379.27f, 5.69f, 380.27f)
            curveTo(0.89f, 381.07f, 0.69f, 376.27f, 1.19f, 373.77f)
            lineTo(28.69f, 354.77f)
            curveTo(30.02f, 354.27f, 33.49f, 353.37f, 36.69f, 353.77f)
            curveTo(42.19f, 347.27f, 54.39f, 330.27f, 59.19f, 314.27f)
            curveTo(65.19f, 294.27f, 67.69f, 281.27f, 77.19f, 268.77f)
            curveTo(84.79f, 258.77f, 90.35f, 251.27f, 92.19f, 248.77f)
            curveTo(93.02f, 232.1f, 96.39f, 196.97f, 103.19f, 189.77f)
            verticalLineTo(154.27f)
            curveTo(104.52f, 144.43f, 116.39f, 124.27f, 153.19f, 122.27f)
            curveTo(160.69f, 118.27f, 176.59f, 109.27f, 180.19f, 105.27f)
            curveTo(183.79f, 101.27f, 183.02f, 95.6f, 182.19f, 93.27f)
            lineTo(176.69f, 87.77f)
            lineTo(175.19f, 73.77f)
            curveTo(174.52f, 74.1f, 172.79f, 73.57f, 171.19f, 68.77f)
            lineTo(166.69f, 55.27f)
            curveTo(166.35f, 54.6f, 166.99f, 52.97f, 172.19f, 51.77f)
            curveTo(170.85f, 38.1f, 173.39f, 8.87f, 194.19f, 1.27f)
            horizontalLineTo(204.69f)
            verticalLineTo(370.5f)
            curveTo(185.24f, 372.5f, 156.92f, 352.51f, 145.69f, 346.77f)
            close()
        }
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundBack),
            strokeLineWidth = 1f
        ) {
            moveTo(213f, 1.27f)
            curveTo(245.4f, -2.33f, 250.17f, 31.43f, 248.5f, 48.77f)
            curveTo(250.83f, 49.6f, 255.3f, 51.87f, 254.5f, 54.27f)
            curveTo(253.7f, 56.67f, 250.83f, 66.93f, 249.5f, 71.77f)
            horizontalLineTo(246.5f)
            lineTo(242f, 93.77f)
            horizontalLineTo(240f)
            verticalLineTo(88.27f)
            verticalLineTo(104.77f)
            curveTo(246.5f, 112.93f, 264.8f, 129.07f, 286f, 128.27f)
            curveTo(298.83f, 129.77f, 322.7f, 144.37f, 315.5f, 190.77f)
            curveTo(320.33f, 204.6f, 329.4f, 235.97f, 327f, 250.77f)
            curveTo(334.83f, 259.1f, 352.3f, 283.67f, 359.5f, 315.27f)
            curveTo(362.5f, 325.1f, 371.3f, 346.87f, 382.5f, 355.27f)
            curveTo(384.83f, 354.77f, 391.3f, 355.87f, 398.5f, 364.27f)
            lineTo(417.5f, 376.77f)
            curveTo(418.5f, 378.93f, 419f, 383.07f, 413f, 382.27f)
            curveTo(407f, 381.47f, 399.17f, 376.93f, 396f, 374.77f)
            lineTo(411f, 414.77f)
            curveTo(411.5f, 417.93f, 411.1f, 423.07f, 405.5f, 418.27f)
            lineTo(392.5f, 390.77f)
            lineTo(404.5f, 417.27f)
            curveTo(406f, 422.77f, 407.1f, 432.07f, 399.5f, 425.27f)
            lineTo(387.5f, 397.27f)
            lineTo(398.5f, 423.77f)
            curveTo(398.67f, 426.93f, 397.5f, 431.97f, 391.5f, 426.77f)
            lineTo(382.5f, 403.27f)
            curveTo(381.67f, 402.43f, 380.2f, 401.37f, 381f, 403.77f)
            curveTo(381.8f, 406.17f, 384.33f, 414.43f, 385.5f, 418.27f)
            curveTo(386f, 420.6f, 386.1f, 424.47f, 382.5f, 421.27f)
            curveTo(376f, 408.77f, 362.6f, 380.07f, 361f, 365.27f)
            lineTo(360.5f, 361.77f)
            lineTo(320.5f, 311.27f)
            curveTo(312.83f, 300.93f, 298.1f, 277.17f, 300.5f, 264.77f)
            curveTo(295.67f, 256.77f, 285.7f, 236.87f, 284.5f, 221.27f)
            curveTo(281f, 233.43f, 272.3f, 259.17f, 265.5f, 264.77f)
            lineTo(266f, 302.77f)
            curveTo(269.67f, 310.93f, 277.75f, 328f, 278.5f, 347f)
            curveTo(270.44f, 354.94f, 246.6f, 370.83f, 215.67f, 370.83f)
            curveTo(215.33f, 370f, 213f, 1.27f, 213f, 1.27f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 213.06f)
            curveTo(185f, 209.06f, 194f, 208.06f, 199f, 211.06f)
            curveTo(202f, 213.06f, 202f, 219.06f, 202f, 223.06f)
            curveTo(202f, 228.06f, 203f, 234.06f, 198f, 235.06f)
            curveTo(192f, 234.06f, 184f, 234.06f, 178f, 235.06f)
            curveTo(174f, 235.06f, 174f, 230.06f, 174f, 226.06f)
            curveTo(174f, 220.06f, 176f, 216.06f, 180f, 213.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 245.06f)
            curveTo(185f, 242.06f, 195f, 241.06f, 199f, 244.06f)
            curveTo(202f, 246.06f, 202f, 251.06f, 202f, 255.06f)
            curveTo(202f, 260.06f, 203f, 265.06f, 198f, 266.06f)
            curveTo(192f, 265.06f, 184f, 265.06f, 178f, 266.06f)
            curveTo(174f, 266.06f, 174f, 262.06f, 174f, 258.06f)
            curveTo(174f, 252.06f, 176f, 248.06f, 180f, 245.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 276.06f)
            curveTo(185f, 273.06f, 195f, 272.06f, 199f, 275.06f)
            curveTo(202f, 277.06f, 202f, 282.06f, 202f, 286.06f)
            curveTo(202f, 291.06f, 203f, 296.06f, 198f, 297.06f)
            curveTo(192f, 296.06f, 184f, 296.06f, 179f, 297.06f)
            curveTo(175f, 297.06f, 175f, 293.06f, 175f, 289.06f)
            curveTo(175f, 283.06f, 176f, 279.06f, 180f, 276.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(183f, 304.06f)
            curveTo(188f, 302.06f, 195f, 303.06f, 199f, 306.06f)
            curveTo(202f, 309.06f, 202f, 315.06f, 202f, 323.06f)
            curveTo(202f, 330.06f, 202f, 340.06f, 201f, 346.06f)
            curveTo(200f, 353.06f, 194f, 354.06f, 190f, 348.06f)
            curveTo(183f, 337.06f, 180f, 324.06f, 179f, 314.06f)
            curveTo(178f, 308.06f, 179f, 305.06f, 183f, 304.06f)
            close()
        }
        path(fill = SolidColor(obliquesAbdominis)) {
            moveTo(154f, 205.06f)
            curveTo(158f, 203.06f, 163f, 206.06f, 166f, 212.06f)
            curveTo(171f, 223.06f, 170f, 238.06f, 168f, 254.06f)
            curveTo(166f, 270.06f, 168f, 301.06f, 169f, 320.06f)
            curveTo(170f, 328.06f, 164f, 329.06f, 160f, 324.06f)
            curveTo(157f, 320.06f, 156f, 314.06f, 156f, 306.06f)
            curveTo(157f, 293.06f, 157f, 278.06f, 157f, 267.06f)
            curveTo(157f, 252.06f, 148f, 240.06f, 149f, 228.06f)
            curveTo(150f, 218.06f, 149f, 208.06f, 154f, 205.06f)
            close()
        }
        path(fill = SolidColor(lateralDeltoidFront)) {
            moveTo(113f, 149.06f)
            curveTo(116f, 142.06f, 122f, 138.06f, 129f, 137.06f)
            curveTo(134f, 136.06f, 135f, 140.06f, 131f, 144.06f)
            curveTo(124f, 152.06f, 118f, 161.06f, 114f, 170.06f)
            curveTo(111f, 176.06f, 108f, 173.06f, 109f, 167.06f)
            curveTo(109f, 161.06f, 110f, 155.06f, 113f, 149.06f)
            close()
        }
        path(fill = SolidColor(anteriorDeltoid)) {
            moveTo(141f, 143.06f)
            curveTo(147f, 139.06f, 151f, 140.06f, 149f, 146.06f)
            curveTo(145f, 157.06f, 135f, 168.06f, 124f, 174.06f)
            curveTo(119f, 177.06f, 116f, 174.06f, 119f, 169.06f)
            curveTo(123f, 159.06f, 133f, 148.06f, 141f, 143.06f)
            close()
        }
        path(fill = SolidColor(biceps)) {
            moveTo(117f, 186.06f)
            curveTo(123f, 183.06f, 129f, 186.06f, 130f, 193.06f)
            curveTo(131f, 207.06f, 121f, 227.06f, 110f, 237.06f)
            curveTo(104f, 244.06f, 99f, 242.06f, 100f, 234.06f)
            curveTo(101f, 215.06f, 105f, 192.06f, 117f, 186.06f)
            close()
        }
        path(fill = SolidColor(forearmFront)) {
            moveTo(95f, 264.06f)
            curveTo(101f, 260.06f, 108f, 265.06f, 109f, 272.06f)
            curveTo(109f, 284.06f, 98f, 302.06f, 87f, 315.06f)
            curveTo(78f, 325.06f, 69f, 335.06f, 64f, 337.06f)
            curveTo(58f, 340.06f, 56f, 336.06f, 59f, 330.06f)
            curveTo(67f, 311.06f, 74f, 286.06f, 86f, 272.06f)
            curveTo(90f, 268.06f, 92f, 266.06f, 95f, 264.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorClavicular)) {
            moveTo(152f, 151.06f)
            curveTo(159f, 143.06f, 168f, 139.06f, 177f, 140.06f)
            curveTo(186f, 141.06f, 195f, 145.06f, 199f, 150.06f)
            curveTo(202f, 154.06f, 203f, 161.06f, 198f, 162.06f)
            curveTo(185f, 156.06f, 168f, 156.06f, 155f, 163.06f)
            curveTo(150f, 166.06f, 146f, 164.06f, 148f, 159.06f)
            curveTo(149f, 156.06f, 150f, 153.06f, 152f, 151.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorSternocostal)) {
            moveTo(153f, 171.06f)
            curveTo(164f, 163.06f, 181f, 162.06f, 195f, 168.06f)
            curveTo(200f, 170.06f, 202f, 174.06f, 202f, 180.06f)
            curveTo(202f, 185.06f, 203f, 190.06f, 197f, 191.06f)
            curveTo(181f, 188.06f, 165f, 183.06f, 153f, 178.06f)
            curveTo(148f, 176.06f, 149f, 174.06f, 153f, 171.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorAbdominal)) {
            moveTo(150f, 185.06f)
            curveTo(160f, 187.06f, 176f, 193.06f, 190f, 196.06f)
            curveTo(197f, 198.06f, 198f, 202.06f, 191f, 204.06f)
            curveTo(175f, 205.06f, 160f, 199.06f, 151f, 193.06f)
            curveTo(145f, 189.06f, 145f, 184.06f, 150f, 185.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(187f, 103.06f)
            curveTo(188f, 100.06f, 190f, 104.06f, 192f, 109.06f)
            curveTo(196f, 118.06f, 199f, 126.06f, 200f, 133.06f)
            curveTo(201f, 138.06f, 199f, 139.06f, 197f, 135.06f)
            curveTo(192f, 127.06f, 186f, 114.06f, 187f, 103.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(177f, 117.06f)
            curveTo(182f, 114.06f, 184f, 116.06f, 180f, 120.06f)
            curveTo(174f, 126.06f, 163f, 129.06f, 157f, 128.06f)
            curveTo(153f, 127.06f, 157f, 124.06f, 160f, 123.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(139f, 194.06f)
            curveTo(141f, 191.06f, 143f, 196.06f, 144f, 203.06f)
            curveTo(146f, 213.06f, 146f, 222.06f, 144f, 227.06f)
            curveTo(142f, 231.06f, 140f, 227.06f, 140f, 222.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(125f, 236.06f)
            curveTo(128f, 230.06f, 130f, 233.06f, 128f, 239.06f)
            curveTo(125f, 249.06f, 115f, 255.06f, 108f, 258.06f)
            curveTo(104f, 260.06f, 104f, 256.06f, 108f, 253.06f)
            curveTo(116f, 246.06f, 120f, 241.06f, 125f, 236.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(40.19f, 394.27f)
            lineTo(28.19f, 381.27f)
            curveTo(26.59f, 380.07f, 26.52f, 378.1f, 26.69f, 377.27f)
            lineTo(33.69f, 366.27f)
            curveTo(39.52f, 359.27f, 51.79f, 347.87f, 54.19f, 358.27f)
            curveTo(57.19f, 371.27f, 49.19f, 386.27f, 47.19f, 392.27f)
            curveTo(45.59f, 397.07f, 41.85f, 395.6f, 40.19f, 394.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(35.69f, 360.27f)
            curveTo(18.09f, 374.27f, 6.69f, 376.77f, 3.19f, 376.27f)
            curveTo(2.79f, 375.07f, 3.69f, 374.44f, 4.19f, 374.27f)
            lineTo(31.69f, 356.27f)
            curveTo(37.69f, 354.67f, 36.85f, 358.27f, 35.69f, 360.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(11.69f, 408.27f)
            lineTo(22.69f, 384.27f)
            curveTo(24.02f, 383.94f, 26.69f, 383.87f, 26.69f, 386.27f)
            curveTo(26.69f, 389.27f, 16.69f, 406.27f, 14.19f, 411.77f)
            curveTo(12.19f, 416.17f, 11.69f, 411.27f, 11.69f, 408.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(30.19f, 396.77f)
            curveTo(31.79f, 390.77f, 29.19f, 391.6f, 27.69f, 392.77f)
            curveTo(26.92f, 393.27f, 17.69f, 416.27f, 16.69f, 419.77f)
            curveTo(15.89f, 422.57f, 18.35f, 422.94f, 19.69f, 422.77f)
            curveTo(22.52f, 416.6f, 28.59f, 402.77f, 30.19f, 396.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 401.77f)
            curveTo(37.89f, 398.97f, 35.52f, 397.6f, 34.19f, 397.27f)
            lineTo(32.69f, 398.27f)
            curveTo(30.19f, 403.77f, 25.09f, 415.57f, 24.69f, 418.77f)
            curveTo(24.29f, 421.97f, 26.85f, 421.77f, 28.19f, 421.27f)
            curveTo(30.52f, 415.94f, 35.49f, 404.57f, 36.69f, 401.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 411.27f)
            lineTo(42.69f, 399.77f)
            curveTo(45.49f, 398.17f, 45.52f, 401.44f, 45.19f, 403.27f)
            curveTo(44.35f, 405.44f, 42.09f, 410.67f, 39.69f, 414.27f)
            curveTo(37.29f, 417.87f, 36.69f, 413.77f, 36.69f, 411.27f)
            close()
        }
        path(fill = SolidColor(rhomboids)) {
            moveTo(224.4f, 280f)
            curveTo(229.4f, 282f, 237.4f, 294f, 241.4f, 300f)
            curveTo(244.4f, 305f, 233.4f, 316f, 227.4f, 322f)
            curveTo(224.4f, 325f, 221.4f, 323f, 221.4f, 317f)
            curveTo(220.4f, 305f, 220.4f, 292f, 221.4f, 284f)
            curveTo(221.4f, 281f, 222.4f, 279f, 224.4f, 280f)
            close()
        }
        path(fill = SolidColor(latissimus)) {
            moveTo(280.4f, 197f)
            curveTo(270.4f, 195f, 259.4f, 197f, 253.4f, 202f)
            curveTo(244.4f, 210f, 235.4f, 218f, 228.4f, 222f)
            curveTo(221.4f, 228f, 222.4f, 241f, 226.4f, 254f)
            curveTo(230.4f, 265f, 237.4f, 275f, 242.4f, 277f)
            curveTo(246.4f, 279f, 249.4f, 278f, 251.4f, 273f)
            curveTo(255.4f, 261f, 262.4f, 250f, 269.4f, 239f)
            curveTo(279.4f, 224f, 283.4f, 210f, 282.4f, 202f)
            curveTo(282.4f, 199f, 281.4f, 197f, 280.4f, 197f)
            close()
        }
        path(fill = SolidColor(trapezius)) {
            moveTo(222.4f, 88f)
            curveTo(223.4f, 103f, 234.4f, 118f, 255.4f, 128f)
            curveTo(260.4f, 130f, 265.4f, 132f, 267.4f, 135f)
            curveTo(270.4f, 137f, 266.4f, 140f, 261.4f, 142f)
            curveTo(252.4f, 148f, 248.4f, 166f, 239.4f, 181f)
            curveTo(233.4f, 191f, 228.4f, 197f, 223.4f, 201f)
            curveTo(219.4f, 204f, 217.4f, 201f, 217.4f, 195f)
            curveTo(216.4f, 179f, 216.4f, 163f, 217.4f, 147f)
            curveTo(218.4f, 127f, 216.4f, 104f, 216.4f, 91f)
            curveTo(216.4f, 85f, 220.4f, 82f, 222.4f, 88f)
            close()
        }
        path(fill = SolidColor(posteriorDeltoid)) {
            moveTo(302.4f, 177f)
            curveTo(307.4f, 172f, 308.4f, 158f, 301.4f, 151f)
            curveTo(295.4f, 144f, 285.4f, 144f, 279.4f, 147f)
            curveTo(274.4f, 149f, 282.4f, 159f, 290.4f, 167f)
            curveTo(296.4f, 173f, 299.4f, 179f, 302.4f, 177f)
            close()
        }
        path(fill = SolidColor(teresMajor)) {
            moveTo(268.4f, 159f)
            curveTo(272.4f, 158f, 280.4f, 169f, 282.4f, 176f)
            curveTo(284.4f, 179f, 279.4f, 181f, 273.4f, 182f)
            curveTo(266.4f, 184f, 260.4f, 185f, 259.4f, 181f)
            curveTo(257.4f, 177f, 262.4f, 165f, 265.4f, 161f)
            curveTo(266.4f, 159f, 267.4f, 158f, 268.4f, 159f)
            close()
        }
        path(fill = SolidColor(triceps)) {
            moveTo(299.4f, 188f)
            curveTo(293.4f, 185f, 291.4f, 190f, 290.4f, 199f)
            curveTo(287.4f, 213f, 292.4f, 228f, 300.4f, 239f)
            curveTo(305.4f, 246f, 312.4f, 250f, 314.4f, 244f)
            curveTo(318.4f, 237f, 314.4f, 217f, 311.4f, 207f)
            curveTo(308.4f, 197f, 304.4f, 190f, 299.4f, 188f)
            close()
        }
        path(fill = SolidColor(forearmBack)) {
            moveTo(324.4f, 266f)
            curveTo(318.4f, 262f, 311.4f, 267f, 310.4f, 274f)
            curveTo(310.4f, 286f, 321.4f, 304f, 332.4f, 317f)
            curveTo(341.4f, 327f, 350.4f, 337f, 355.4f, 339f)
            curveTo(361.4f, 342f, 363.4f, 338f, 360.4f, 332f)
            curveTo(352.4f, 313f, 345.4f, 288f, 333.4f, 274f)
            curveTo(329.4f, 270f, 327.4f, 268f, 324.4f, 266f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(260.4f, 287f)
            curveTo(259.4f, 284f, 256.4f, 287f, 253.4f, 295f)
            curveTo(249.4f, 304f, 245.4f, 309f, 240.4f, 313f)
            curveTo(236.4f, 316f, 237.4f, 320f, 242.4f, 322f)
            curveTo(253.4f, 325f, 261.4f, 330f, 266.4f, 336f)
            curveTo(269.4f, 340f, 271.4f, 339f, 270.4f, 334f)
            curveTo(269.4f, 320f, 264.4f, 307f, 263.4f, 298f)
            curveTo(263.4f, 292f, 262.4f, 289f, 260.4f, 287f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(314.4f, 258f)
            curveTo(317.4f, 259f, 319.4f, 262f, 319.4f, 266f)
            curveTo(319.4f, 269f, 316.4f, 268f, 313.4f, 265f)
            curveTo(310.4f, 262f, 308.4f, 259f, 310.4f, 258f)
            curveTo(311.4f, 257f, 313.4f, 257f, 314.4f, 258f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(373.98f, 396.27f)
            curveTo(365.58f, 383.87f, 365.48f, 365.77f, 366.48f, 358.27f)
            curveTo(369.15f, 356.93f, 375.29f, 354.97f, 378.48f, 357.77f)
            curveTo(381.69f, 360.57f, 389.48f, 373.27f, 392.98f, 379.27f)
            curveTo(393.82f, 380.1f, 394.69f, 382.27f, 391.48f, 384.27f)
            curveTo(388.29f, 386.27f, 386.15f, 388.77f, 385.48f, 389.77f)
            curveTo(381.88f, 396.17f, 376.32f, 396.77f, 373.98f, 396.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(377.98f, 401.27f)
            curveTo(375.58f, 397.67f, 374.98f, 400.1f, 374.98f, 401.77f)
            curveTo(376.15f, 404.93f, 378.88f, 412.07f, 380.48f, 415.27f)
            curveTo(382.08f, 418.47f, 383.15f, 415.93f, 383.48f, 414.27f)
            curveTo(382.65f, 411.43f, 380.38f, 404.87f, 377.98f, 401.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(386.98f, 398.77f)
            curveTo(384.19f, 393.17f, 383.15f, 398.1f, 382.98f, 401.27f)
            lineTo(391.48f, 421.27f)
            curveTo(395.48f, 424.87f, 395.48f, 421.1f, 394.98f, 418.77f)
            curveTo(393.48f, 414.43f, 389.79f, 404.37f, 386.98f, 398.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(392.98f, 395.27f)
            curveTo(389.38f, 389.27f, 388.48f, 394.43f, 388.48f, 397.77f)
            lineTo(399.98f, 423.27f)
            curveTo(403.58f, 427.67f, 403.48f, 421.43f, 402.98f, 417.77f)
            curveTo(401.15f, 412.77f, 396.58f, 401.27f, 392.98f, 395.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(398.98f, 387.77f)
            curveTo(396.58f, 382.57f, 394.65f, 386.93f, 393.98f, 389.77f)
            lineTo(404.48f, 413.77f)
            curveTo(408.08f, 417.77f, 408.32f, 412.1f, 407.98f, 408.77f)
            curveTo(405.98f, 403.93f, 401.38f, 392.97f, 398.98f, 387.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(411.98f, 380.27f)
            curveTo(404.38f, 379.87f, 391.48f, 369.43f, 385.98f, 364.27f)
            curveTo(385.32f, 362.93f, 384.38f, 360.17f, 385.98f, 359.77f)
            curveTo(387.58f, 359.37f, 389.65f, 360.6f, 390.48f, 361.27f)
            lineTo(403.48f, 369.27f)
            lineTo(415.48f, 376.27f)
            curveTo(417.48f, 377.77f, 419.58f, 380.67f, 411.98f, 380.27f)
            close()
        }
    }.build()
}
