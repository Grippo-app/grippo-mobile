package com.grippo.design.resources.provider.muscles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public fun legsSplit(
    colors: MuscleColorPreset
): ImageVector {
    val outlineColor: Color = colors.outline
    val backgroundFront: Color = colors.backgroundFront
    val backgroundBack: Color = colors.backgroundBack
    val quadriceps: Color = colors.quadriceps
    val gluteal: Color = colors.gluteal
    val hamstrings: Color = colors.hamstrings
    val calf: Color = colors.calf
    val adductors: Color = colors.adductors
    val abductors: Color = colors.abductors
    val other: Color = colors.other

    return ImageVector.Builder(
        name = "Legssplit",
        defaultWidth = 332.dp,
        defaultHeight = 447.dp,
        viewportWidth = 332f,
        viewportHeight = 447f
    ).apply {
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundFront),
            strokeLineWidth = 1f
        ) {
            moveTo(32.25f, 413.81f)
            lineTo(35.25f, 401.31f)
            lineTo(34.25f, 371.81f)
            curveTo(30.75f, 351.14f, 22.75f, 313.81f, 14.75f, 297.81f)
            curveTo(7.38f, 283.08f, 12.42f, 237.48f, 16.75f, 219.31f)
            lineTo(12.75f, 187.81f)
            curveTo(-6.45f, 132.21f, 2.06f, 53.21f, 9.06f, 13.04f)
            curveTo(54.7f, -1.38f, 81.98f, -3.01f, 128.06f, 9.04f)
            curveTo(130.06f, 17.38f, 133.85f, 33.91f, 136.25f, 86.31f)
            curveTo(138.65f, 138.71f, 129.25f, 178.81f, 124.25f, 192.31f)
            curveTo(119.45f, 215.91f, 120.58f, 226.48f, 121.75f, 228.81f)
            curveTo(130.15f, 251.21f, 126.92f, 278.81f, 124.25f, 289.81f)
            curveTo(121.42f, 299.31f, 115.55f, 318.61f, 114.75f, 319.81f)
            curveTo(113.75f, 321.31f, 104.25f, 371.31f, 102.25f, 374.81f)
            curveTo(100.65f, 377.61f, 101.58f, 389.31f, 102.25f, 394.81f)
            lineTo(103.25f, 401.31f)
            lineTo(104.25f, 410.81f)
            lineTo(121.75f, 434.81f)
            lineTo(126.75f, 439.81f)
            verticalLineTo(443.31f)
            lineTo(123.25f, 444.89f)
            lineTo(86.75f, 445.81f)
            lineTo(80.75f, 439.81f)
            lineTo(81.75f, 425.81f)
            lineTo(83.25f, 410.81f)
            curveTo(84.08f, 400.48f, 85.25f, 376.31f, 83.25f, 362.31f)
            curveTo(80.75f, 344.81f, 76.75f, 299.31f, 75.75f, 289.81f)
            curveTo(74.95f, 282.21f, 82.08f, 243.31f, 85.75f, 224.81f)
            curveTo(85.92f, 218.14f, 86.15f, 204.51f, 85.75f, 203.31f)
            curveTo(85.35f, 202.11f, 78.58f, 145.14f, 75.25f, 116.81f)
            curveTo(74.08f, 100.81f, 70.95f, 69.91f, 67.75f, 74.31f)
            curveTo(64.55f, 78.71f, 58.08f, 147.48f, 55.25f, 181.31f)
            curveTo(49.65f, 195.31f, 50.92f, 217.48f, 52.25f, 226.81f)
            curveTo(60.65f, 260.01f, 62.08f, 287.98f, 61.75f, 297.81f)
            curveTo(60.42f, 313.14f, 57.15f, 344.61f, 54.75f, 347.81f)
            curveTo(52.35f, 351.01f, 53.75f, 373.48f, 54.75f, 384.31f)
            verticalLineTo(413.81f)
            lineTo(57.75f, 436.81f)
            lineTo(54.75f, 443.81f)
            curveTo(52.25f, 444.31f, 43.75f, 444.81f, 37.75f, 444.81f)
            curveTo(14.52f, 444.81f, 12.75f, 441.98f, 14.75f, 439.81f)
            lineTo(32.25f, 413.81f)
            close()
        }
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundBack),
            strokeLineWidth = 1f
        ) {
            moveTo(207.65f, 437.81f)
            curveTo(204.45f, 439.41f, 207.31f, 443.14f, 209.15f, 444.81f)
            curveTo(218.31f, 445.48f, 238.25f, 446.51f, 244.65f, 445.31f)
            curveTo(251.05f, 444.11f, 251.98f, 439.81f, 251.65f, 437.81f)
            verticalLineTo(426.81f)
            curveTo(250.48f, 423.31f, 248.35f, 414.91f, 249.15f, 409.31f)
            curveTo(249.95f, 403.71f, 248.81f, 398.31f, 248.15f, 396.31f)
            curveTo(247.75f, 391.91f, 249.31f, 367.14f, 250.15f, 355.31f)
            curveTo(256.65f, 300.81f, 256.15f, 303.81f, 256.65f, 284.31f)
            curveTo(257.15f, 264.81f, 246.15f, 218.81f, 246.15f, 216.81f)
            curveTo(246.15f, 215.21f, 247.15f, 204.81f, 247.65f, 199.81f)
            lineTo(250.65f, 175.81f)
            curveTo(254.31f, 144.98f, 261.85f, 82.01f, 262.65f, 76.81f)
            curveTo(263.45f, 71.61f, 264.31f, 71.98f, 264.65f, 72.81f)
            lineTo(273.15f, 133.81f)
            curveTo(275.31f, 155.14f, 279.95f, 198.31f, 281.15f, 200.31f)
            curveTo(282.35f, 202.31f, 281.65f, 213.48f, 281.15f, 218.81f)
            lineTo(278.65f, 236.81f)
            lineTo(271.15f, 279.31f)
            curveTo(270.81f, 290.14f, 270.75f, 314.01f, 273.15f, 322.81f)
            curveTo(275.55f, 331.61f, 278.15f, 375.48f, 279.15f, 396.31f)
            curveTo(277.55f, 398.71f, 278.15f, 406.31f, 278.65f, 409.81f)
            curveTo(278.15f, 413.81f, 276.85f, 424.01f, 275.65f, 432.81f)
            curveTo(274.45f, 441.61f, 277.48f, 444.81f, 279.15f, 445.31f)
            curveTo(290.15f, 445.48f, 313.25f, 445.71f, 317.65f, 445.31f)
            curveTo(322.05f, 444.91f, 321.81f, 440.48f, 321.15f, 438.31f)
            curveTo(318.98f, 437.81f, 314.15f, 435.41f, 312.15f, 429.81f)
            curveTo(310.15f, 424.21f, 303.31f, 415.14f, 300.15f, 411.31f)
            verticalLineTo(407.81f)
            curveTo(300.55f, 402.61f, 298.65f, 397.31f, 297.65f, 395.31f)
            curveTo(297.48f, 392.31f, 297.25f, 384.31f, 297.65f, 376.31f)
            curveTo(298.05f, 368.31f, 305.48f, 338.98f, 309.15f, 325.31f)
            curveTo(310.65f, 322.64f, 314.85f, 311.91f, 319.65f, 290.31f)
            curveTo(324.45f, 268.71f, 318.65f, 233.64f, 315.15f, 218.81f)
            curveTo(317.55f, 215.21f, 318.15f, 206.98f, 318.15f, 203.31f)
            curveTo(322.95f, 191.71f, 328.48f, 152.81f, 330.65f, 134.81f)
            curveTo(330.31f, 115.14f, 328.15f, 37.81f, 328.15f, 37.81f)
            curveTo(317.06f, -5.46f, 206.56f, -11.46f, 199.15f, 41.31f)
            curveTo(199.15f, 41.31f, 197.05f, 106.01f, 196.65f, 126.81f)
            curveTo(196.25f, 147.61f, 205.15f, 184.14f, 209.65f, 199.81f)
            curveTo(208.85f, 203.41f, 210.98f, 213.31f, 212.15f, 217.81f)
            curveTo(211.15f, 225.48f, 208.55f, 244.21f, 206.15f, 257.81f)
            curveTo(203.15f, 274.81f, 212.65f, 307.81f, 213.15f, 308.81f)
            curveTo(213.55f, 309.61f, 219.98f, 331.14f, 223.15f, 341.81f)
            curveTo(224.81f, 350.48f, 228.55f, 368.71f, 230.15f, 372.31f)
            curveTo(231.75f, 375.91f, 230.81f, 389.81f, 230.15f, 396.31f)
            curveTo(226.55f, 399.51f, 227.31f, 406.31f, 228.15f, 409.31f)
            curveTo(227.65f, 411.14f, 226.15f, 414.91f, 224.15f, 415.31f)
            curveTo(222.15f, 415.71f, 218.31f, 424.48f, 216.65f, 428.81f)
            curveTo(214.98f, 431.14f, 210.85f, 436.21f, 207.65f, 437.81f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(15.06f, 23.5f)
            curveTo(17.06f, 18.5f, 20.06f, 23.5f, 21.06f, 31.5f)
            curveTo(28.06f, 62.5f, 36.06f, 114.5f, 37.06f, 158.5f)
            curveTo(37.06f, 176.5f, 31.06f, 189.5f, 26.06f, 189.5f)
            curveTo(18.06f, 189.5f, 11.06f, 160.5f, 9.06f, 137.5f)
            curveTo(6.06f, 104.5f, 7.06f, 56.5f, 15.06f, 23.5f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(122.06f, 23.5f)
            curveTo(120.06f, 18.5f, 117.06f, 23.5f, 116.06f, 31.5f)
            curveTo(109.06f, 62.5f, 101.06f, 114.5f, 100.06f, 158.5f)
            curveTo(100.06f, 176.5f, 106.06f, 189.5f, 111.06f, 189.5f)
            curveTo(119.06f, 189.5f, 126.06f, 160.5f, 128.06f, 137.5f)
            curveTo(131.06f, 104.5f, 130.06f, 56.5f, 122.06f, 23.5f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(29.06f, 23.5f)
            curveTo(34.06f, 24.5f, 45.06f, 43.5f, 50.06f, 59.5f)
            curveTo(56.06f, 76.5f, 59.06f, 100.5f, 55.06f, 113.5f)
            curveTo(53.06f, 120.5f, 48.06f, 119.5f, 45.06f, 113.5f)
            curveTo(36.06f, 96.5f, 26.06f, 56.5f, 25.06f, 33.5f)
            curveTo(24.06f, 25.5f, 25.06f, 21.5f, 29.06f, 23.5f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(108.06f, 23.5f)
            curveTo(103.06f, 24.5f, 92.06f, 43.5f, 87.06f, 59.5f)
            curveTo(81.06f, 76.5f, 78.06f, 100.5f, 82.06f, 113.5f)
            curveTo(84.06f, 120.5f, 89.06f, 119.5f, 92.06f, 113.5f)
            curveTo(101.06f, 96.5f, 111.06f, 56.5f, 112.06f, 33.5f)
            curveTo(113.06f, 25.5f, 112.06f, 21.5f, 108.06f, 23.5f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(46.06f, 129.5f)
            curveTo(49.06f, 125.5f, 52.06f, 128.5f, 53.06f, 134.5f)
            curveTo(55.06f, 150.5f, 52.06f, 169.5f, 49.06f, 180.5f)
            curveTo(47.06f, 188.5f, 43.06f, 189.5f, 41.06f, 182.5f)
            curveTo(39.06f, 169.5f, 40.06f, 141.5f, 46.06f, 129.5f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(91.06f, 129.5f)
            curveTo(88.06f, 125.5f, 85.06f, 128.5f, 84.06f, 134.5f)
            curveTo(82.06f, 150.5f, 85.06f, 169.5f, 88.06f, 180.5f)
            curveTo(90.06f, 188.5f, 94.06f, 189.5f, 96.06f, 182.5f)
            curveTo(98.06f, 169.5f, 97.06f, 141.5f, 91.06f, 129.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(35.06f, 17.5f)
            curveTo(41.06f, 17.5f, 49.06f, 22.5f, 54.06f, 29.5f)
            curveTo(59.06f, 36.5f, 61.06f, 48.5f, 60.06f, 55.5f)
            curveTo(59.06f, 61.5f, 56.06f, 60.5f, 53.06f, 55.5f)
            curveTo(48.06f, 46.5f, 40.06f, 33.5f, 35.06f, 24.5f)
            curveTo(32.06f, 19.5f, 32.06f, 17.5f, 35.06f, 17.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(102.06f, 17.5f)
            curveTo(96.06f, 17.5f, 88.06f, 22.5f, 83.06f, 29.5f)
            curveTo(78.06f, 36.5f, 76.06f, 48.5f, 77.06f, 55.5f)
            curveTo(78.06f, 61.5f, 81.06f, 60.5f, 84.06f, 55.5f)
            curveTo(89.06f, 46.5f, 97.06f, 33.5f, 102.06f, 24.5f)
            curveTo(105.06f, 19.5f, 105.06f, 17.5f, 102.06f, 17.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(58.06f, 59.5f)
            curveTo(60.06f, 55.5f, 62.06f, 59.5f, 62.06f, 64.5f)
            curveTo(62.06f, 74.5f, 60.06f, 86.5f, 57.06f, 90.5f)
            curveTo(55.06f, 93.5f, 54.06f, 88.5f, 55.06f, 82.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(79.06f, 59.5f)
            curveTo(77.06f, 55.5f, 75.06f, 59.5f, 75.06f, 64.5f)
            curveTo(75.06f, 74.5f, 77.06f, 86.5f, 80.06f, 90.5f)
            curveTo(82.06f, 93.5f, 83.06f, 88.5f, 82.06f, 82.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(31.06f, 202.5f)
            curveTo(38.06f, 198.5f, 43.06f, 202.5f, 44.06f, 209.5f)
            curveTo(45.06f, 217.5f, 40.06f, 223.5f, 36.06f, 222.5f)
            curveTo(31.06f, 221.5f, 27.06f, 207.5f, 31.06f, 202.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(106.06f, 202.5f)
            curveTo(99.06f, 198.5f, 94.06f, 202.5f, 93.06f, 209.5f)
            curveTo(92.06f, 217.5f, 97.06f, 223.5f, 101.06f, 222.5f)
            curveTo(106.06f, 221.5f, 110.06f, 207.5f, 106.06f, 202.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(26.06f, 237.5f)
            curveTo(32.06f, 236.5f, 38.06f, 247.5f, 42.06f, 259.5f)
            curveTo(51.06f, 286.5f, 52.06f, 318.5f, 48.06f, 342.5f)
            curveTo(47.06f, 352.5f, 43.06f, 356.5f, 39.06f, 348.5f)
            curveTo(28.06f, 326.5f, 21.06f, 294.5f, 19.06f, 269.5f)
            curveTo(17.06f, 255.5f, 19.06f, 240.5f, 26.06f, 237.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(111.06f, 237.5f)
            curveTo(105.06f, 236.5f, 99.06f, 247.5f, 95.06f, 259.5f)
            curveTo(86.06f, 286.5f, 85.06f, 318.5f, 89.06f, 342.5f)
            curveTo(90.06f, 352.5f, 94.06f, 356.5f, 98.06f, 348.5f)
            curveTo(109.06f, 326.5f, 116.06f, 294.5f, 118.06f, 269.5f)
            curveTo(120.06f, 255.5f, 118.06f, 240.5f, 111.06f, 237.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(44.06f, 378.5f)
            curveTo(47.06f, 376.5f, 50.06f, 379.5f, 49.06f, 385.5f)
            curveTo(48.06f, 399.5f, 50.06f, 413.5f, 51.06f, 423.5f)
            curveTo(51.06f, 429.5f, 48.06f, 430.5f, 45.06f, 426.5f)
            curveTo(40.06f, 419.5f, 39.06f, 402.5f, 41.06f, 388.5f)
            curveTo(42.06f, 382.5f, 42.06f, 379.5f, 44.06f, 378.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(93.06f, 378.5f)
            curveTo(90.06f, 376.5f, 87.06f, 379.5f, 88.06f, 385.5f)
            curveTo(89.06f, 399.5f, 87.06f, 413.5f, 86.06f, 423.5f)
            curveTo(86.06f, 429.5f, 89.06f, 430.5f, 92.06f, 426.5f)
            curveTo(97.06f, 419.5f, 98.06f, 402.5f, 96.06f, 388.5f)
            curveTo(95.06f, 382.5f, 95.06f, 379.5f, 93.06f, 378.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(34.06f, 422.5f)
            curveTo(37.06f, 419.5f, 40.06f, 423.5f, 40.06f, 428.5f)
            curveTo(40.06f, 432.5f, 41.06f, 438.5f, 38.06f, 440.5f)
            curveTo(33.06f, 442.5f, 24.06f, 441.5f, 21.06f, 439.5f)
            curveTo(19.06f, 437.5f, 29.06f, 427.5f, 34.06f, 422.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(103.06f, 422.5f)
            curveTo(100.06f, 419.5f, 97.06f, 423.5f, 97.06f, 428.5f)
            curveTo(97.06f, 432.5f, 96.06f, 438.5f, 99.06f, 440.5f)
            curveTo(104.06f, 442.5f, 113.06f, 441.5f, 116.06f, 439.5f)
            curveTo(118.06f, 437.5f, 108.06f, 427.5f, 103.06f, 422.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(46.06f, 432.5f)
            curveTo(49.06f, 430.5f, 52.06f, 432.5f, 52.06f, 436.5f)
            curveTo(52.06f, 440.5f, 49.06f, 442.5f, 45.06f, 441.5f)
            curveTo(42.06f, 439.5f, 43.06f, 434.5f, 46.06f, 432.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(91.06f, 432.5f)
            curveTo(88.06f, 430.5f, 85.06f, 432.5f, 85.06f, 436.5f)
            curveTo(85.06f, 440.5f, 88.06f, 442.5f, 92.06f, 441.5f)
            curveTo(95.06f, 439.5f, 94.06f, 434.5f, 91.06f, 432.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(45.06f, 6.04f)
            curveTo(54.56f, 32.04f, 50.06f, 22.54f, 54.25f, 31.31f)
            curveTo(59.45f, 35.31f, 62.42f, 31.31f, 63.25f, 28.81f)
            verticalLineTo(3.54f)
            curveTo(52.06f, 3.54f, 50.56f, 5.04f, 45.06f, 6.04f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(93.06f, 4.54f)
            curveTo(83.56f, 30.54f, 87.25f, 22.54f, 83.06f, 31.31f)
            curveTo(77.86f, 35.31f, 74.9f, 31.31f, 74.06f, 28.81f)
            verticalLineTo(3.04f)
            curveTo(85.25f, 3.04f, 87.56f, 3.54f, 93.06f, 4.54f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(226.15f, 438.5f)
            curveTo(227.15f, 431.5f, 230.15f, 421.5f, 235.15f, 419.5f)
            curveTo(241.15f, 417.5f, 245.15f, 428.5f, 244.15f, 438.5f)
            curveTo(239.15f, 440.5f, 231.15f, 441.5f, 226.15f, 440.5f)
            curveTo(225.15f, 440.5f, 225.15f, 439.5f, 226.15f, 438.5f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(301.15f, 438.5f)
            curveTo(300.15f, 431.5f, 297.15f, 421.5f, 292.15f, 419.5f)
            curveTo(286.15f, 417.5f, 282.15f, 428.5f, 283.15f, 438.5f)
            curveTo(288.15f, 440.5f, 296.15f, 441.5f, 301.15f, 440.5f)
            curveTo(302.15f, 440.5f, 302.15f, 439.5f, 301.15f, 438.5f)
            close()
        }
        path(fill = SolidColor(calf)) {
            moveTo(219.15f, 200.5f)
            curveTo(215.15f, 216.5f, 211.15f, 244.5f, 212.15f, 264.5f)
            curveTo(213.15f, 283.5f, 220.15f, 298.5f, 225.15f, 315.5f)
            curveTo(230.15f, 330.5f, 232.15f, 344.5f, 234.15f, 350.5f)
            curveTo(236.15f, 355.5f, 240.15f, 353.5f, 240.15f, 347.5f)
            curveTo(239.15f, 333.5f, 235.15f, 318.5f, 232.15f, 302.5f)
            curveTo(228.15f, 283.5f, 227.15f, 252.5f, 227.15f, 226.5f)
            curveTo(227.15f, 212.5f, 225.15f, 201.5f, 222.15f, 198.5f)
            curveTo(221.15f, 196.5f, 220.15f, 197.5f, 219.15f, 200.5f)
            close()
        }
        path(fill = SolidColor(calf)) {
            moveTo(308.15f, 200.5f)
            curveTo(312.15f, 216.5f, 316.15f, 244.5f, 315.15f, 264.5f)
            curveTo(314.15f, 283.5f, 307.15f, 298.5f, 302.15f, 315.5f)
            curveTo(297.15f, 330.5f, 295.15f, 344.5f, 293.15f, 350.5f)
            curveTo(291.15f, 355.5f, 287.15f, 353.5f, 287.15f, 347.5f)
            curveTo(288.15f, 333.5f, 292.15f, 318.5f, 295.15f, 302.5f)
            curveTo(299.15f, 283.5f, 300.15f, 252.5f, 300.15f, 226.5f)
            curveTo(300.15f, 212.5f, 302.15f, 201.5f, 305.15f, 198.5f)
            curveTo(306.15f, 196.5f, 307.15f, 197.5f, 308.15f, 200.5f)
            close()
        }
        path(fill = SolidColor(calf)) {
            moveTo(235.15f, 206.5f)
            curveTo(232.15f, 209.5f, 232.15f, 223.5f, 233.15f, 238.5f)
            curveTo(234.15f, 258.5f, 234.15f, 274.5f, 239.15f, 288.5f)
            curveTo(242.15f, 297.5f, 243.15f, 306.5f, 243.15f, 318.5f)
            curveTo(242.15f, 329.5f, 241.15f, 341.5f, 242.15f, 348.5f)
            curveTo(243.15f, 354.5f, 248.15f, 354.5f, 248.15f, 347.5f)
            curveTo(250.15f, 328.5f, 252.15f, 311.5f, 253.15f, 295.5f)
            curveTo(254.15f, 278.5f, 251.15f, 257.5f, 247.15f, 238.5f)
            curveTo(243.15f, 221.5f, 239.15f, 210.5f, 237.15f, 206.5f)
            curveTo(236.15f, 204.5f, 236.15f, 204.5f, 235.15f, 206.5f)
            close()
        }
        path(fill = SolidColor(calf)) {
            moveTo(292.15f, 206.5f)
            curveTo(295.15f, 209.5f, 295.15f, 223.5f, 294.15f, 238.5f)
            curveTo(293.15f, 258.5f, 293.15f, 274.5f, 288.15f, 288.5f)
            curveTo(285.15f, 297.5f, 284.15f, 306.5f, 284.15f, 318.5f)
            curveTo(285.15f, 329.5f, 286.15f, 341.5f, 285.15f, 348.5f)
            curveTo(284.15f, 354.5f, 279.15f, 354.5f, 279.15f, 347.5f)
            curveTo(277.15f, 328.5f, 275.15f, 311.5f, 274.15f, 295.5f)
            curveTo(273.15f, 278.5f, 276.15f, 257.5f, 280.15f, 238.5f)
            curveTo(284.15f, 221.5f, 288.15f, 210.5f, 290.15f, 206.5f)
            curveTo(291.15f, 204.5f, 291.15f, 204.5f, 292.15f, 206.5f)
            close()
        }
        path(fill = SolidColor(hamstrings)) {
            moveTo(227.15f, 85.5f)
            curveTo(221.15f, 84.5f, 215.15f, 96.5f, 212.15f, 114.5f)
            curveTo(209.15f, 133.5f, 211.15f, 158.5f, 216.15f, 173.5f)
            curveTo(219.15f, 182.5f, 224.15f, 187.5f, 230.15f, 191.5f)
            curveTo(235.15f, 195.5f, 239.15f, 192.5f, 241.15f, 183.5f)
            curveTo(245.15f, 169.5f, 242.15f, 145.5f, 239.15f, 124.5f)
            curveTo(236.15f, 109.5f, 236.15f, 95.5f, 233.15f, 89.5f)
            curveTo(231.15f, 86.5f, 229.15f, 85.5f, 227.15f, 85.5f)
            close()
        }
        path(fill = SolidColor(hamstrings)) {
            moveTo(300.15f, 85.5f)
            curveTo(306.15f, 84.5f, 312.15f, 96.5f, 315.15f, 114.5f)
            curveTo(318.15f, 133.5f, 316.15f, 158.5f, 311.15f, 173.5f)
            curveTo(308.15f, 182.5f, 303.15f, 187.5f, 297.15f, 191.5f)
            curveTo(292.15f, 195.5f, 288.15f, 192.5f, 286.15f, 183.5f)
            curveTo(282.15f, 169.5f, 285.15f, 145.5f, 288.15f, 124.5f)
            curveTo(291.15f, 109.5f, 291.15f, 95.5f, 294.15f, 89.5f)
            curveTo(296.15f, 86.5f, 298.15f, 85.5f, 300.15f, 85.5f)
            close()
        }
        path(fill = SolidColor(adductors)) {
            moveTo(248.15f, 70.5f)
            curveTo(244.15f, 72.5f, 242.15f, 75.5f, 242.15f, 82.5f)
            curveTo(243.15f, 97.5f, 245.15f, 116.5f, 248.15f, 126.5f)
            curveTo(250.15f, 132.5f, 253.15f, 131.5f, 254.15f, 124.5f)
            curveTo(259.15f, 104.5f, 259.15f, 86.5f, 256.15f, 76.5f)
            curveTo(255.15f, 70.5f, 253.15f, 68.5f, 248.15f, 70.5f)
            close()
        }
        path(fill = SolidColor(adductors)) {
            moveTo(279.15f, 70.5f)
            curveTo(283.15f, 72.5f, 285.15f, 75.5f, 285.15f, 82.5f)
            curveTo(284.15f, 97.5f, 282.15f, 116.5f, 279.15f, 126.5f)
            curveTo(277.15f, 132.5f, 274.15f, 131.5f, 273.15f, 124.5f)
            curveTo(268.15f, 104.5f, 268.15f, 86.5f, 271.15f, 76.5f)
            curveTo(272.15f, 70.5f, 274.15f, 68.5f, 279.15f, 70.5f)
            close()
        }
        path(fill = SolidColor(abductors)) {
            moveTo(204.15f, 78.5f)
            curveTo(202.15f, 75.5f, 201.15f, 80.5f, 201.15f, 91.5f)
            curveTo(201.15f, 101.5f, 201.15f, 112.5f, 203.15f, 117.5f)
            curveTo(205.15f, 120.5f, 207.15f, 116.5f, 207.15f, 110.5f)
            curveTo(207.15f, 102.5f, 210.15f, 94.5f, 208.15f, 87.5f)
            curveTo(207.15f, 83.5f, 206.15f, 80.5f, 204.15f, 78.5f)
            close()
        }
        path(fill = SolidColor(abductors)) {
            moveTo(323.15f, 78.5f)
            curveTo(325.15f, 75.5f, 326.15f, 80.5f, 326.15f, 91.5f)
            curveTo(326.15f, 101.5f, 326.15f, 112.5f, 324.15f, 117.5f)
            curveTo(322.15f, 120.5f, 320.15f, 116.5f, 320.15f, 110.5f)
            curveTo(320.15f, 102.5f, 317.15f, 94.5f, 319.15f, 87.5f)
            curveTo(320.15f, 83.5f, 321.15f, 80.5f, 323.15f, 78.5f)
            close()
        }
        path(fill = SolidColor(gluteal)) {
            moveTo(253.15f, 15.5f)
            curveTo(237.15f, 14.5f, 217.15f, 21.5f, 211.15f, 32.5f)
            curveTo(207.15f, 41.5f, 207.15f, 57.5f, 209.15f, 66.5f)
            curveTo(210.15f, 72.5f, 213.15f, 73.5f, 218.15f, 69.5f)
            curveTo(228.15f, 63.5f, 244.15f, 59.5f, 252.15f, 51.5f)
            curveTo(257.15f, 46.5f, 258.15f, 37.5f, 258.15f, 25.5f)
            curveTo(258.15f, 19.5f, 258.15f, 16.5f, 253.15f, 15.5f)
            close()
        }
        path(fill = SolidColor(gluteal)) {
            moveTo(274.15f, 15.5f)
            curveTo(290.15f, 14.5f, 310.15f, 21.5f, 316.15f, 32.5f)
            curveTo(320.15f, 41.5f, 320.15f, 57.5f, 318.15f, 66.5f)
            curveTo(317.15f, 72.5f, 314.15f, 73.5f, 309.15f, 69.5f)
            curveTo(299.15f, 63.5f, 283.15f, 59.5f, 275.15f, 51.5f)
            curveTo(270.15f, 46.5f, 269.15f, 37.5f, 269.15f, 25.5f)
            curveTo(269.15f, 19.5f, 269.15f, 16.5f, 274.15f, 15.5f)
            close()
        }
    }.build()
}
