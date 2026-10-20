package com.grippo.design.resources.provider.muscles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public fun fullFront(
    colors: MuscleColorPreset
): ImageVector {

    val outlineColor: Color = colors.outline
    val backgroundFront: Color = colors.backgroundFront
    val biceps: Color = colors.biceps
    val forearm: Color = colors.forearm
    val lateralDeltoid: Color = colors.lateralDeltoid
    val anteriorDeltoid: Color = colors.anteriorDeltoid
    val rectusAbdominis: Color = colors.rectusAbdominis
    val obliquesAbdominis: Color = colors.obliquesAbdominis
    val pectoralisMajorAbdominal: Color = colors.pectoralisMajorAbdominal
    val pectoralisMajorClavicular: Color = colors.pectoralisMajorClavicular
    val pectoralisMajorSternocostal: Color = colors.pectoralisMajorSternocostal

    val quadriceps: Color = colors.quadriceps

    val other: Color = colors.other

    return ImageVector.Builder(
        name = "Fullfront",
        defaultWidth = 411.dp,
        defaultHeight = 773.dp,
        viewportWidth = 411f,
        viewportHeight = 773f
    ).apply {
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundFront),
            strokeLineWidth = 1f
        ) {
            moveTo(172.19f, 727.77f)
            lineTo(169.19f, 740.27f)
            lineTo(151.69f, 766.27f)
            curveTo(149.69f, 768.43f, 151.46f, 771.27f, 174.69f, 771.27f)
            curveTo(180.69f, 771.27f, 189.19f, 770.77f, 191.69f, 770.27f)
            lineTo(194.69f, 763.27f)
            lineTo(191.69f, 740.27f)
            verticalLineTo(710.77f)
            curveTo(190.69f, 699.93f, 189.29f, 677.47f, 191.69f, 674.27f)
            curveTo(194.09f, 671.07f, 197.35f, 639.6f, 198.69f, 624.27f)
            curveTo(199.02f, 614.43f, 197.59f, 586.47f, 189.19f, 553.27f)
            curveTo(188f, 538f, 189.19f, 522f, 192.19f, 507.77f)
            curveTo(195.02f, 473.93f, 201.49f, 405.17f, 204.69f, 400.77f)
            curveTo(207.89f, 396.37f, 211.02f, 427.27f, 212.19f, 443.27f)
            curveTo(215.52f, 471.6f, 222.29f, 528.57f, 222.69f, 529.77f)
            curveTo(223.09f, 530.97f, 222.85f, 544.6f, 222.69f, 551.27f)
            curveTo(219.02f, 569.77f, 211.89f, 608.67f, 212.69f, 616.27f)
            curveTo(213.69f, 625.77f, 217.69f, 671.27f, 220.19f, 688.77f)
            curveTo(222.19f, 702.77f, 221.02f, 726.93f, 220.19f, 737.27f)
            lineTo(218.69f, 752.27f)
            lineTo(217.69f, 766.27f)
            lineTo(223.69f, 772.27f)
            lineTo(260.19f, 771.34f)
            lineTo(263.69f, 769.77f)
            verticalLineTo(766.27f)
            lineTo(258.69f, 761.27f)
            lineTo(241.19f, 737.27f)
            lineTo(240.19f, 727.77f)
            lineTo(239.19f, 721.27f)
            curveTo(238.52f, 715.77f, 237.59f, 704.07f, 239.19f, 701.27f)
            curveTo(241.19f, 697.77f, 250.69f, 647.77f, 251.69f, 646.27f)
            curveTo(252.49f, 645.07f, 258.35f, 625.77f, 261.19f, 616.27f)
            curveTo(263.85f, 605.27f, 267.09f, 577.67f, 258.69f, 555.27f)
            curveTo(257.52f, 552.93f, 256.39f, 542.37f, 261.19f, 518.77f)
            curveTo(266.19f, 505.27f, 275.59f, 465.17f, 273.19f, 412.77f)
            curveTo(270.79f, 360.37f, 266.19f, 330.6f, 264.19f, 322.27f)
            lineTo(257.19f, 300.77f)
            lineTo(257.69f, 263.77f)
            curveTo(272.69f, 243.77f, 274.69f, 214.77f, 275.19f, 222.77f)
            curveTo(275.59f, 229.17f, 286.02f, 252.1f, 291.19f, 262.77f)
            curveTo(290.79f, 273.17f, 300.69f, 293.43f, 305.69f, 302.27f)
            curveTo(314.69f, 314.1f, 334.09f, 339.17f, 339.69f, 344.77f)
            curveTo(346.69f, 351.77f, 354.69f, 368.77f, 355.69f, 375.77f)
            curveTo(356.69f, 382.77f, 361.69f, 394.77f, 362.19f, 397.77f)
            curveTo(362.69f, 400.77f, 371.69f, 418.27f, 374.19f, 421.27f)
            curveTo(376.19f, 423.67f, 377.69f, 421.6f, 378.19f, 420.27f)
            curveTo(376.52f, 415.77f, 373.09f, 406.27f, 372.69f, 404.27f)
            curveTo(372.29f, 402.27f, 373.19f, 402.77f, 373.69f, 403.27f)
            lineTo(383.69f, 426.27f)
            curveTo(387.69f, 430.27f, 389.69f, 426.6f, 390.19f, 424.27f)
            curveTo(386.35f, 415.77f, 378.59f, 398.47f, 378.19f, 397.27f)
            curveTo(377.79f, 396.07f, 378.35f, 396.43f, 378.69f, 396.77f)
            lineTo(392.19f, 426.77f)
            curveTo(397.39f, 430.37f, 398.35f, 425.27f, 398.19f, 422.27f)
            lineTo(384.19f, 391.27f)
            curveTo(387.69f, 399.77f, 395.59f, 417.17f, 399.19f, 418.77f)
            curveTo(402.79f, 420.37f, 403.02f, 416.77f, 402.69f, 414.77f)
            lineTo(388.19f, 374.27f)
            curveTo(391.85f, 376.77f, 400.49f, 381.77f, 405.69f, 381.77f)
            curveTo(410.89f, 381.77f, 410.52f, 377.77f, 409.69f, 375.77f)
            curveTo(405.85f, 374.43f, 396.69f, 370.07f, 390.69f, 363.27f)
            curveTo(384.69f, 356.47f, 376.52f, 354.43f, 373.19f, 354.27f)
            curveTo(363.19f, 345.47f, 354.35f, 323.6f, 351.19f, 313.77f)
            curveTo(347.99f, 292.17f, 328.19f, 261.77f, 318.69f, 249.27f)
            curveTo(321.49f, 231.27f, 312.52f, 203.43f, 307.69f, 191.77f)
            verticalLineTo(156.77f)
            curveTo(301.19f, 123.77f, 259.69f, 124.27f, 257.19f, 124.27f)
            curveTo(254.69f, 124.27f, 238.19f, 112.77f, 233.19f, 107.27f)
            curveTo(229.19f, 102.87f, 229.52f, 97.43f, 230.19f, 95.27f)
            lineTo(234.19f, 89.77f)
            lineTo(235.69f, 82.27f)
            lineTo(236.19f, 74.77f)
            curveTo(237.39f, 75.17f, 238.02f, 74.27f, 238.19f, 73.77f)
            curveTo(239.52f, 69.6f, 242.49f, 60.67f, 243.69f, 58.27f)
            curveTo(244.89f, 55.87f, 240.85f, 53.6f, 238.69f, 52.77f)
            curveTo(243.19f, 15.77f, 220.19f, -6.23f, 194.19f, 3.27f)
            curveTo(173.39f, 10.87f, 170.85f, 40.1f, 172.19f, 53.77f)
            curveTo(166.99f, 54.97f, 166.35f, 56.6f, 166.69f, 57.27f)
            curveTo(167.52f, 59.77f, 169.59f, 65.97f, 171.19f, 70.77f)
            curveTo(172.79f, 75.57f, 174.52f, 76.1f, 175.19f, 75.77f)
            lineTo(176.69f, 89.77f)
            lineTo(182.19f, 95.27f)
            curveTo(183.02f, 97.6f, 183.79f, 103.27f, 180.19f, 107.27f)
            curveTo(176.59f, 111.27f, 160.69f, 120.27f, 153.19f, 124.27f)
            curveTo(116.39f, 126.27f, 104.52f, 146.43f, 103.19f, 156.27f)
            verticalLineTo(191.77f)
            curveTo(96.39f, 198.97f, 93.02f, 234.1f, 92.19f, 250.77f)
            curveTo(90.35f, 253.27f, 84.79f, 260.77f, 77.19f, 270.77f)
            curveTo(67.69f, 283.27f, 65.19f, 296.27f, 59.19f, 316.27f)
            curveTo(54.39f, 332.27f, 42.19f, 349.27f, 36.69f, 355.77f)
            curveTo(33.49f, 355.37f, 30.02f, 356.27f, 28.69f, 356.77f)
            lineTo(1.19f, 375.77f)
            curveTo(0.69f, 378.27f, 0.89f, 383.07f, 5.69f, 382.27f)
            curveTo(11.69f, 381.27f, 21.69f, 374.27f, 23.19f, 373.77f)
            curveTo(24.39f, 373.37f, 24.02f, 373.93f, 23.69f, 374.27f)
            lineTo(19.69f, 384.77f)
            curveTo(16.69f, 392.27f, 10.29f, 408.67f, 8.69f, 414.27f)
            curveTo(7.09f, 419.87f, 10.69f, 419.27f, 12.69f, 418.27f)
            lineTo(24.19f, 395.27f)
            curveTo(25.79f, 393.27f, 26.19f, 394.77f, 26.19f, 395.77f)
            curveTo(22.52f, 403.27f, 14.69f, 419.37f, 12.69f, 423.77f)
            curveTo(10.69f, 428.17f, 15.52f, 428.27f, 18.19f, 427.77f)
            lineTo(30.19f, 401.27f)
            curveTo(32.59f, 398.47f, 32.19f, 400.77f, 31.69f, 402.27f)
            curveTo(28.85f, 409.27f, 22.89f, 423.57f, 21.69f, 424.77f)
            curveTo(20.19f, 426.27f, 23.19f, 431.77f, 26.19f, 428.77f)
            curveTo(28.59f, 426.37f, 34.19f, 411.77f, 36.69f, 404.77f)
            curveTo(38.29f, 402.77f, 38.69f, 403.93f, 38.69f, 404.77f)
            curveTo(38.85f, 405.1f, 38.29f, 407.77f, 34.69f, 415.77f)
            curveTo(31.09f, 423.77f, 35.52f, 422.77f, 38.19f, 421.27f)
            curveTo(48.59f, 407.27f, 53.52f, 384.77f, 54.69f, 375.27f)
            lineTo(61.19f, 357.77f)
            curveTo(61.69f, 356.43f, 68.19f, 347.47f, 90.19f, 322.27f)
            curveTo(112.19f, 297.07f, 119.02f, 271.43f, 119.69f, 261.77f)
            curveTo(126.49f, 256.57f, 132.85f, 234.27f, 135.19f, 223.77f)
            curveTo(134.79f, 226.97f, 147.69f, 252.1f, 154.19f, 264.27f)
            curveTo(154.19f, 270.77f, 154.19f, 285.97f, 154.19f, 294.77f)
            curveTo(154.19f, 303.57f, 149.19f, 318.1f, 146.69f, 324.27f)
            curveTo(139.69f, 364.43f, 130.49f, 458.67f, 149.69f, 514.27f)
            lineTo(153.69f, 545.77f)
            curveTo(149.35f, 563.93f, 144.32f, 609.53f, 151.69f, 624.27f)
            curveTo(159.69f, 640.27f, 167.69f, 677.6f, 171.19f, 698.27f)
            lineTo(172.19f, 727.77f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(152f, 350f)
            curveTo(154f, 345f, 157f, 350f, 158f, 358f)
            curveTo(165f, 389f, 173f, 441f, 174f, 485f)
            curveTo(174f, 503f, 168f, 516f, 163f, 516f)
            curveTo(155f, 516f, 148f, 487f, 146f, 464f)
            curveTo(143f, 431f, 144f, 383f, 152f, 350f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(259f, 350f)
            curveTo(257f, 345f, 254f, 350f, 253f, 358f)
            curveTo(246f, 389f, 238f, 441f, 237f, 485f)
            curveTo(237f, 503f, 243f, 516f, 248f, 516f)
            curveTo(256f, 516f, 263f, 487f, 265f, 464f)
            curveTo(268f, 431f, 267f, 383f, 259f, 350f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(166f, 350f)
            curveTo(171f, 351f, 182f, 370f, 187f, 386f)
            curveTo(193f, 403f, 196f, 427f, 192f, 440f)
            curveTo(190f, 447f, 185f, 446f, 182f, 440f)
            curveTo(173f, 423f, 163f, 383f, 162f, 360f)
            curveTo(161f, 352f, 162f, 348f, 166f, 350f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(245f, 350f)
            curveTo(240f, 351f, 229f, 370f, 224f, 386f)
            curveTo(218f, 403f, 215f, 427f, 219f, 440f)
            curveTo(221f, 447f, 226f, 446f, 229f, 440f)
            curveTo(238f, 423f, 248f, 383f, 249f, 360f)
            curveTo(250f, 352f, 249f, 348f, 245f, 350f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(183f, 456f)
            curveTo(186f, 452f, 189f, 455f, 190f, 461f)
            curveTo(192f, 477f, 189f, 496f, 186f, 507f)
            curveTo(184f, 515f, 180f, 516f, 178f, 509f)
            curveTo(176f, 496f, 177f, 468f, 183f, 456f)
            close()
        }
        path(fill = SolidColor(quadriceps)) {
            moveTo(228f, 456f)
            curveTo(225f, 452f, 222f, 455f, 221f, 461f)
            curveTo(219f, 477f, 222f, 496f, 225f, 507f)
            curveTo(227f, 515f, 231f, 516f, 233f, 509f)
            curveTo(235f, 496f, 234f, 468f, 228f, 456f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(172f, 344f)
            curveTo(178f, 344f, 186f, 349f, 191f, 356f)
            curveTo(196f, 363f, 198f, 375f, 197f, 382f)
            curveTo(196f, 388f, 193f, 387f, 190f, 382f)
            curveTo(185f, 373f, 177f, 360f, 172f, 351f)
            curveTo(169f, 346f, 169f, 344f, 172f, 344f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(239f, 344f)
            curveTo(233f, 344f, 225f, 349f, 220f, 356f)
            curveTo(215f, 363f, 213f, 375f, 214f, 382f)
            curveTo(215f, 388f, 218f, 387f, 221f, 382f)
            curveTo(226f, 373f, 234f, 360f, 239f, 351f)
            curveTo(242f, 346f, 242f, 344f, 239f, 344f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(195f, 386f)
            curveTo(197f, 382f, 199f, 386f, 199f, 391f)
            curveTo(199f, 401f, 197f, 413f, 194f, 417f)
            curveTo(192f, 420f, 191f, 415f, 192f, 409f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(216f, 386f)
            curveTo(214f, 382f, 212f, 386f, 212f, 391f)
            curveTo(212f, 401f, 214f, 413f, 217f, 417f)
            curveTo(219f, 420f, 220f, 415f, 219f, 409f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(168f, 529f)
            curveTo(175f, 525f, 180f, 529f, 181f, 536f)
            curveTo(182f, 544f, 177f, 550f, 173f, 549f)
            curveTo(168f, 548f, 164f, 534f, 168f, 529f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(243f, 529f)
            curveTo(236f, 525f, 231f, 529f, 230f, 536f)
            curveTo(229f, 544f, 234f, 550f, 238f, 549f)
            curveTo(243f, 548f, 247f, 534f, 243f, 529f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(163f, 564f)
            curveTo(169f, 563f, 175f, 574f, 179f, 586f)
            curveTo(188f, 613f, 189f, 645f, 185f, 669f)
            curveTo(184f, 679f, 180f, 683f, 176f, 675f)
            curveTo(165f, 653f, 158f, 621f, 156f, 596f)
            curveTo(154f, 582f, 156f, 567f, 163f, 564f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(248f, 564f)
            curveTo(242f, 563f, 236f, 574f, 232f, 586f)
            curveTo(223f, 613f, 222f, 645f, 226f, 669f)
            curveTo(227f, 679f, 231f, 683f, 235f, 675f)
            curveTo(246f, 653f, 253f, 621f, 255f, 596f)
            curveTo(257f, 582f, 255f, 567f, 248f, 564f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(181f, 705f)
            curveTo(184f, 703f, 187f, 706f, 186f, 712f)
            curveTo(185f, 726f, 187f, 740f, 188f, 750f)
            curveTo(188f, 756f, 185f, 757f, 182f, 753f)
            curveTo(177f, 746f, 176f, 729f, 178f, 715f)
            curveTo(179f, 709f, 179f, 706f, 181f, 705f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(230f, 705f)
            curveTo(227f, 703f, 224f, 706f, 225f, 712f)
            curveTo(226f, 726f, 224f, 740f, 223f, 750f)
            curveTo(223f, 756f, 226f, 757f, 229f, 753f)
            curveTo(234f, 746f, 235f, 729f, 233f, 715f)
            curveTo(232f, 709f, 232f, 706f, 230f, 705f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(171f, 749f)
            curveTo(174f, 746f, 177f, 750f, 177f, 755f)
            curveTo(177f, 759f, 178f, 765f, 175f, 767f)
            curveTo(170f, 769f, 161f, 768f, 158f, 766f)
            curveTo(156f, 764f, 166f, 754f, 171f, 749f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(240f, 749f)
            curveTo(237f, 746f, 234f, 750f, 234f, 755f)
            curveTo(234f, 759f, 233f, 765f, 236f, 767f)
            curveTo(241f, 769f, 250f, 768f, 253f, 766f)
            curveTo(255f, 764f, 245f, 754f, 240f, 749f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(183f, 759f)
            curveTo(186f, 757f, 189f, 759f, 189f, 763f)
            curveTo(189f, 767f, 186f, 769f, 182f, 768f)
            curveTo(179f, 766f, 180f, 761f, 183f, 759f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(228f, 759f)
            curveTo(225f, 757f, 222f, 759f, 222f, 763f)
            curveTo(222f, 767f, 225f, 769f, 229f, 768f)
            curveTo(232f, 766f, 231f, 761f, 228f, 759f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 215.06f)
            curveTo(185f, 211.06f, 194f, 210.06f, 199f, 213.06f)
            curveTo(202f, 215.06f, 202f, 221.06f, 202f, 225.06f)
            curveTo(202f, 230.06f, 203f, 236.06f, 198f, 237.06f)
            curveTo(192f, 236.06f, 184f, 236.06f, 178f, 237.06f)
            curveTo(174f, 237.06f, 174f, 232.06f, 174f, 228.06f)
            curveTo(174f, 222.06f, 176f, 218.06f, 180f, 215.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 215.06f)
            curveTo(226f, 211.06f, 217f, 210.06f, 212f, 213.06f)
            curveTo(209f, 215.06f, 209f, 221.06f, 209f, 225.06f)
            curveTo(209f, 230.06f, 208f, 236.06f, 213f, 237.06f)
            curveTo(219f, 236.06f, 227f, 236.06f, 233f, 237.06f)
            curveTo(237f, 237.06f, 237f, 232.06f, 237f, 228.06f)
            curveTo(237f, 222.06f, 235f, 218.06f, 231f, 215.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 247.06f)
            curveTo(185f, 244.06f, 195f, 243.06f, 199f, 246.06f)
            curveTo(202f, 248.06f, 202f, 253.06f, 202f, 257.06f)
            curveTo(202f, 262.06f, 203f, 267.06f, 198f, 268.06f)
            curveTo(192f, 267.06f, 184f, 267.06f, 178f, 268.06f)
            curveTo(174f, 268.06f, 174f, 264.06f, 174f, 260.06f)
            curveTo(174f, 254.06f, 176f, 250.06f, 180f, 247.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 247.06f)
            curveTo(226f, 244.06f, 216f, 243.06f, 212f, 246.06f)
            curveTo(209f, 248.06f, 209f, 253.06f, 209f, 257.06f)
            curveTo(209f, 262.06f, 208f, 267.06f, 213f, 268.06f)
            curveTo(219f, 267.06f, 227f, 267.06f, 233f, 268.06f)
            curveTo(237f, 268.06f, 237f, 264.06f, 237f, 260.06f)
            curveTo(237f, 254.06f, 235f, 250.06f, 231f, 247.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 278.06f)
            curveTo(185f, 275.06f, 195f, 274.06f, 199f, 277.06f)
            curveTo(202f, 279.06f, 202f, 284.06f, 202f, 288.06f)
            curveTo(202f, 293.06f, 203f, 298.06f, 198f, 299.06f)
            curveTo(192f, 298.06f, 184f, 298.06f, 179f, 299.06f)
            curveTo(175f, 299.06f, 175f, 295.06f, 175f, 291.06f)
            curveTo(175f, 285.06f, 176f, 281.06f, 180f, 278.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 278.06f)
            curveTo(226f, 275.06f, 216f, 274.06f, 212f, 277.06f)
            curveTo(209f, 279.06f, 209f, 284.06f, 209f, 288.06f)
            curveTo(209f, 293.06f, 208f, 298.06f, 213f, 299.06f)
            curveTo(219f, 298.06f, 227f, 298.06f, 232f, 299.06f)
            curveTo(236f, 299.06f, 236f, 295.06f, 236f, 291.06f)
            curveTo(236f, 285.06f, 235f, 281.06f, 231f, 278.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(183f, 306.06f)
            curveTo(188f, 304.06f, 195f, 305.06f, 199f, 308.06f)
            curveTo(202f, 311.06f, 202f, 317.06f, 202f, 325.06f)
            curveTo(202f, 332.06f, 202f, 342.06f, 201f, 348.06f)
            curveTo(200f, 355.06f, 194f, 356.06f, 190f, 350.06f)
            curveTo(183f, 339.06f, 180f, 326.06f, 179f, 316.06f)
            curveTo(178f, 310.06f, 179f, 307.06f, 183f, 306.06f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(228f, 306.06f)
            curveTo(223f, 304.06f, 216f, 305.06f, 212f, 308.06f)
            curveTo(209f, 311.06f, 209f, 317.06f, 209f, 325.06f)
            curveTo(209f, 332.06f, 209f, 342.06f, 210f, 348.06f)
            curveTo(211f, 355.06f, 217f, 356.06f, 221f, 350.06f)
            curveTo(228f, 339.06f, 231f, 326.06f, 232f, 316.06f)
            curveTo(233f, 310.06f, 232f, 307.06f, 228f, 306.06f)
            close()
        }
        path(fill = SolidColor(obliquesAbdominis)) {
            moveTo(154f, 207.06f)
            curveTo(158f, 205.06f, 163f, 208.06f, 166f, 214.06f)
            curveTo(171f, 225.06f, 170f, 240.06f, 168f, 256.06f)
            curveTo(166f, 272.06f, 168f, 303.06f, 169f, 322.06f)
            curveTo(170f, 330.06f, 164f, 331.06f, 160f, 326.06f)
            curveTo(157f, 322.06f, 156f, 316.06f, 156f, 308.06f)
            curveTo(157f, 295.06f, 157f, 280.06f, 157f, 269.06f)
            curveTo(157f, 254.06f, 148f, 242.06f, 149f, 230.06f)
            curveTo(150f, 220.06f, 149f, 210.06f, 154f, 207.06f)
            close()
        }
        path(fill = SolidColor(obliquesAbdominis)) {
            moveTo(257f, 207.06f)
            curveTo(253f, 205.06f, 248f, 208.06f, 245f, 214.06f)
            curveTo(240f, 225.06f, 241f, 240.06f, 243f, 256.06f)
            curveTo(245f, 272.06f, 243f, 303.06f, 242f, 322.06f)
            curveTo(241f, 330.06f, 247f, 331.06f, 251f, 326.06f)
            curveTo(254f, 322.06f, 255f, 316.06f, 255f, 308.06f)
            curveTo(254f, 295.06f, 254f, 280.06f, 254f, 269.06f)
            curveTo(254f, 254.06f, 263f, 242.06f, 262f, 230.06f)
            curveTo(261f, 220.06f, 262f, 210.06f, 257f, 207.06f)
            close()
        }
        path(fill = SolidColor(lateralDeltoid)) {
            moveTo(113f, 151.06f)
            curveTo(116f, 144.06f, 122f, 140.06f, 129f, 139.06f)
            curveTo(134f, 138.06f, 135f, 142.06f, 131f, 146.06f)
            curveTo(124f, 154.06f, 118f, 163.06f, 114f, 172.06f)
            curveTo(111f, 178.06f, 108f, 175.06f, 109f, 169.06f)
            curveTo(109f, 163.06f, 110f, 157.06f, 113f, 151.06f)
            close()
        }
        path(fill = SolidColor(lateralDeltoid)) {
            moveTo(298f, 151.06f)
            curveTo(295f, 144.06f, 289f, 140.06f, 282f, 139.06f)
            curveTo(277f, 138.06f, 276f, 142.06f, 280f, 146.06f)
            curveTo(287f, 154.06f, 293f, 163.06f, 297f, 172.06f)
            curveTo(300f, 178.06f, 303f, 175.06f, 302f, 169.06f)
            curveTo(302f, 163.06f, 301f, 157.06f, 298f, 151.06f)
            close()
        }
        path(fill = SolidColor(anteriorDeltoid)) {
            moveTo(141f, 145.06f)
            curveTo(147f, 141.06f, 151f, 142.06f, 149f, 148.06f)
            curveTo(145f, 159.06f, 135f, 170.06f, 124f, 176.06f)
            curveTo(119f, 179.06f, 116f, 176.06f, 119f, 171.06f)
            curveTo(123f, 161.06f, 133f, 150.06f, 141f, 145.06f)
            close()
        }
        path(fill = SolidColor(anteriorDeltoid)) {
            moveTo(270f, 145.06f)
            curveTo(264f, 141.06f, 260f, 142.06f, 262f, 148.06f)
            curveTo(266f, 159.06f, 276f, 170.06f, 287f, 176.06f)
            curveTo(292f, 179.06f, 295f, 176.06f, 292f, 171.06f)
            curveTo(288f, 161.06f, 278f, 150.06f, 270f, 145.06f)
            close()
        }
        path(fill = SolidColor(biceps)) {
            moveTo(117f, 188.06f)
            curveTo(123f, 185.06f, 129f, 188.06f, 130f, 195.06f)
            curveTo(131f, 209.06f, 121f, 229.06f, 110f, 239.06f)
            curveTo(104f, 246.06f, 99f, 244.06f, 100f, 236.06f)
            curveTo(101f, 217.06f, 105f, 194.06f, 117f, 188.06f)
            close()
        }
        path(fill = SolidColor(biceps)) {
            moveTo(294f, 188.06f)
            curveTo(288f, 185.06f, 282f, 188.06f, 281f, 195.06f)
            curveTo(280f, 209.06f, 290f, 229.06f, 301f, 239.06f)
            curveTo(307f, 246.06f, 312f, 244.06f, 311f, 236.06f)
            curveTo(310f, 217.06f, 306f, 194.06f, 294f, 188.06f)
            close()
        }
        path(fill = SolidColor(forearm)) {
            moveTo(95f, 266.06f)
            curveTo(101f, 262.06f, 108f, 267.06f, 109f, 274.06f)
            curveTo(109f, 286.06f, 98f, 304.06f, 87f, 317.06f)
            curveTo(78f, 327.06f, 69f, 337.06f, 64f, 339.06f)
            curveTo(58f, 342.06f, 56f, 338.06f, 59f, 332.06f)
            curveTo(67f, 313.06f, 74f, 288.06f, 86f, 274.06f)
            curveTo(90f, 270.06f, 92f, 268.06f, 95f, 266.06f)
            close()
        }
        path(fill = SolidColor(forearm)) {
            moveTo(316f, 266.06f)
            curveTo(310f, 262.06f, 303f, 267.06f, 302f, 274.06f)
            curveTo(302f, 286.06f, 313f, 304.06f, 324f, 317.06f)
            curveTo(333f, 327.06f, 342f, 337.06f, 347f, 339.06f)
            curveTo(353f, 342.06f, 355f, 338.06f, 352f, 332.06f)
            curveTo(344f, 313.06f, 337f, 288.06f, 325f, 274.06f)
            curveTo(321f, 270.06f, 319f, 268.06f, 316f, 266.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorClavicular)) {
            moveTo(152f, 153.06f)
            curveTo(159f, 145.06f, 168f, 141.06f, 177f, 142.06f)
            curveTo(186f, 143.06f, 195f, 147.06f, 199f, 152.06f)
            curveTo(202f, 156.06f, 203f, 163.06f, 198f, 164.06f)
            curveTo(185f, 158.06f, 168f, 158.06f, 155f, 165.06f)
            curveTo(150f, 168.06f, 146f, 166.06f, 148f, 161.06f)
            curveTo(149f, 158.06f, 150f, 155.06f, 152f, 153.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorClavicular)) {
            moveTo(259f, 153.06f)
            curveTo(252f, 145.06f, 243f, 141.06f, 234f, 142.06f)
            curveTo(225f, 143.06f, 216f, 147.06f, 212f, 152.06f)
            curveTo(209f, 156.06f, 208f, 163.06f, 213f, 164.06f)
            curveTo(226f, 158.06f, 243f, 158.06f, 256f, 165.06f)
            curveTo(261f, 168.06f, 265f, 166.06f, 263f, 161.06f)
            curveTo(262f, 158.06f, 261f, 155.06f, 259f, 153.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorSternocostal)) {
            moveTo(153f, 173.06f)
            curveTo(164f, 165.06f, 181f, 164.06f, 195f, 170.06f)
            curveTo(200f, 172.06f, 202f, 176.06f, 202f, 182.06f)
            curveTo(202f, 187.06f, 203f, 192.06f, 197f, 193.06f)
            curveTo(181f, 190.06f, 165f, 185.06f, 153f, 180.06f)
            curveTo(148f, 178.06f, 149f, 176.06f, 153f, 173.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorSternocostal)) {
            moveTo(258f, 173.06f)
            curveTo(247f, 165.06f, 230f, 164.06f, 216f, 170.06f)
            curveTo(211f, 172.06f, 209f, 176.06f, 209f, 182.06f)
            curveTo(209f, 187.06f, 208f, 192.06f, 214f, 193.06f)
            curveTo(230f, 190.06f, 246f, 185.06f, 258f, 180.06f)
            curveTo(263f, 178.06f, 262f, 176.06f, 258f, 173.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorAbdominal)) {
            moveTo(150f, 187.06f)
            curveTo(160f, 189.06f, 176f, 195.06f, 190f, 198.06f)
            curveTo(197f, 200.06f, 198f, 204.06f, 191f, 206.06f)
            curveTo(175f, 207.06f, 160f, 201.06f, 151f, 195.06f)
            curveTo(145f, 191.06f, 145f, 186.06f, 150f, 187.06f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorAbdominal)) {
            moveTo(261f, 187.06f)
            curveTo(251f, 189.06f, 235f, 195.06f, 221f, 198.06f)
            curveTo(214f, 200.06f, 213f, 204.06f, 220f, 206.06f)
            curveTo(236f, 207.06f, 251f, 201.06f, 260f, 195.06f)
            curveTo(266f, 191.06f, 266f, 186.06f, 261f, 187.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(187f, 105.06f)
            curveTo(188f, 102.06f, 190f, 106.06f, 192f, 111.06f)
            curveTo(196f, 120.06f, 199f, 128.06f, 200f, 135.06f)
            curveTo(201f, 140.06f, 199f, 141.06f, 197f, 137.06f)
            curveTo(192f, 129.06f, 186f, 116.06f, 187f, 105.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(224f, 105.06f)
            curveTo(223f, 102.06f, 221f, 106.06f, 219f, 111.06f)
            curveTo(215f, 120.06f, 212f, 128.06f, 211f, 135.06f)
            curveTo(210f, 140.06f, 212f, 141.06f, 214f, 137.06f)
            curveTo(219f, 129.06f, 225f, 116.06f, 224f, 105.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(177f, 119.06f)
            curveTo(182f, 116.06f, 184f, 118.06f, 180f, 122.06f)
            curveTo(174f, 128.06f, 163f, 131.06f, 157f, 130.06f)
            curveTo(153f, 129.06f, 157f, 126.06f, 160f, 125.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(234f, 119.06f)
            curveTo(229f, 116.06f, 227f, 118.06f, 231f, 122.06f)
            curveTo(237f, 128.06f, 248f, 131.06f, 254f, 130.06f)
            curveTo(258f, 129.06f, 254f, 126.06f, 251f, 125.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(139f, 196.06f)
            curveTo(141f, 193.06f, 143f, 198.06f, 144f, 205.06f)
            curveTo(146f, 215.06f, 146f, 224.06f, 144f, 229.06f)
            curveTo(142f, 233.06f, 140f, 229.06f, 140f, 224.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(272f, 196.06f)
            curveTo(270f, 193.06f, 268f, 198.06f, 267f, 205.06f)
            curveTo(265f, 215.06f, 265f, 224.06f, 267f, 229.06f)
            curveTo(269f, 233.06f, 271f, 229.06f, 271f, 224.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(125f, 238.06f)
            curveTo(128f, 232.06f, 130f, 235.06f, 128f, 241.06f)
            curveTo(125f, 251.06f, 115f, 257.06f, 108f, 260.06f)
            curveTo(104f, 262.06f, 104f, 258.06f, 108f, 255.06f)
            curveTo(116f, 248.06f, 120f, 243.06f, 125f, 238.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(286f, 238.06f)
            curveTo(283f, 232.06f, 281f, 235.06f, 283f, 241.06f)
            curveTo(286f, 251.06f, 296f, 257.06f, 303f, 260.06f)
            curveTo(307f, 262.06f, 307f, 258.06f, 303f, 255.06f)
            curveTo(295f, 248.06f, 291f, 243.06f, 286f, 238.06f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(40.19f, 396.27f)
            lineTo(28.19f, 383.27f)
            curveTo(26.59f, 382.07f, 26.52f, 380.1f, 26.69f, 379.27f)
            lineTo(33.69f, 368.27f)
            curveTo(39.52f, 361.27f, 51.79f, 349.87f, 54.19f, 360.27f)
            curveTo(57.19f, 373.27f, 49.19f, 388.27f, 47.19f, 394.27f)
            curveTo(45.59f, 399.07f, 41.85f, 397.6f, 40.19f, 396.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(35.69f, 362.27f)
            curveTo(18.09f, 376.27f, 6.69f, 378.77f, 3.19f, 378.27f)
            curveTo(2.79f, 377.07f, 3.69f, 376.44f, 4.19f, 376.27f)
            lineTo(31.69f, 358.27f)
            curveTo(37.69f, 356.67f, 36.85f, 360.27f, 35.69f, 362.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(11.69f, 410.27f)
            lineTo(22.69f, 386.27f)
            curveTo(24.02f, 385.94f, 26.69f, 385.87f, 26.69f, 388.27f)
            curveTo(26.69f, 391.27f, 16.69f, 408.27f, 14.19f, 413.77f)
            curveTo(12.19f, 418.17f, 11.69f, 413.27f, 11.69f, 410.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(30.19f, 398.77f)
            curveTo(31.79f, 392.77f, 29.19f, 393.6f, 27.69f, 394.77f)
            curveTo(26.92f, 395.27f, 17.69f, 418.27f, 16.69f, 421.77f)
            curveTo(15.89f, 424.57f, 18.35f, 424.94f, 19.69f, 424.77f)
            curveTo(22.52f, 418.6f, 28.59f, 404.77f, 30.19f, 398.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 403.77f)
            curveTo(37.89f, 400.97f, 35.52f, 399.6f, 34.19f, 399.27f)
            lineTo(32.69f, 400.27f)
            curveTo(30.19f, 405.77f, 25.09f, 417.57f, 24.69f, 420.77f)
            curveTo(24.29f, 423.97f, 26.85f, 423.77f, 28.19f, 423.27f)
            curveTo(30.52f, 417.94f, 35.49f, 406.57f, 36.69f, 403.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 413.27f)
            lineTo(42.69f, 401.77f)
            curveTo(45.49f, 400.17f, 45.52f, 403.44f, 45.19f, 405.27f)
            curveTo(44.35f, 407.44f, 42.09f, 412.67f, 39.69f, 416.27f)
            curveTo(37.29f, 419.87f, 36.69f, 415.77f, 36.69f, 413.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(372.35f, 396.96f)
            lineTo(384.35f, 383.96f)
            curveTo(385.95f, 382.76f, 386.02f, 380.79f, 385.85f, 379.96f)
            lineTo(378.85f, 368.96f)
            curveTo(373.02f, 361.96f, 360.75f, 350.56f, 358.35f, 360.96f)
            curveTo(355.35f, 373.96f, 363.35f, 388.96f, 365.35f, 394.96f)
            curveTo(366.95f, 399.76f, 370.69f, 398.29f, 372.35f, 396.96f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(376.85f, 362.96f)
            curveTo(394.45f, 376.96f, 405.85f, 379.46f, 409.35f, 378.96f)
            curveTo(409.75f, 377.76f, 408.85f, 377.12f, 408.35f, 376.96f)
            lineTo(380.85f, 358.96f)
            curveTo(374.85f, 357.36f, 375.69f, 360.96f, 376.85f, 362.96f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(400.85f, 410.96f)
            lineTo(389.85f, 386.96f)
            curveTo(388.52f, 386.62f, 385.85f, 386.56f, 385.85f, 388.96f)
            curveTo(385.85f, 391.96f, 395.85f, 408.96f, 398.35f, 414.46f)
            curveTo(400.35f, 418.86f, 400.85f, 413.96f, 400.85f, 410.96f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(382.35f, 399.46f)
            curveTo(380.75f, 393.46f, 383.35f, 394.29f, 384.85f, 395.46f)
            curveTo(385.62f, 395.96f, 394.85f, 418.96f, 395.85f, 422.46f)
            curveTo(396.65f, 425.26f, 394.19f, 425.62f, 392.85f, 425.46f)
            curveTo(390.02f, 419.29f, 383.95f, 405.46f, 382.35f, 399.46f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(375.85f, 404.46f)
            curveTo(374.65f, 401.66f, 377.02f, 400.29f, 378.35f, 399.96f)
            lineTo(379.85f, 400.96f)
            curveTo(382.35f, 406.46f, 387.45f, 418.26f, 387.85f, 421.46f)
            curveTo(388.25f, 424.66f, 385.69f, 424.46f, 384.35f, 423.96f)
            curveTo(382.02f, 418.62f, 377.05f, 407.26f, 375.85f, 404.46f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(375.85f, 413.96f)
            lineTo(369.85f, 402.46f)
            curveTo(367.05f, 400.86f, 367.02f, 404.12f, 367.35f, 405.96f)
            curveTo(368.19f, 408.12f, 370.45f, 413.36f, 372.85f, 416.96f)
            curveTo(375.25f, 420.56f, 375.85f, 416.46f, 375.85f, 413.96f)
            close()
        }
    }.build()
}
