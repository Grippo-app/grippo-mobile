package com.grippo.design.resources.provider.muscles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public fun bodyFront(
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
    val other: Color = colors.other

    return ImageVector.Builder(
        name = "Bodyfront",
        defaultWidth = 411.dp,
        defaultHeight = 431.dp,
        viewportWidth = 411f,
        viewportHeight = 431f
    ).apply {
        path(
            stroke = SolidColor(outlineColor),
            fill = SolidColor(backgroundFront),
            strokeLineWidth = 1f
        ) {
            moveTo(262.5f, 330.44f)
            lineTo(257.19f, 300.71f)
            lineTo(257.69f, 263.71f)
            curveTo(272.69f, 243.71f, 274.69f, 214.71f, 275.19f, 222.71f)
            curveTo(275.59f, 229.11f, 286.02f, 252.04f, 291.19f, 262.71f)
            curveTo(290.79f, 273.11f, 300.69f, 293.38f, 305.69f, 302.21f)
            curveTo(314.69f, 314.04f, 334.09f, 339.11f, 339.69f, 344.71f)
            curveTo(346.69f, 351.71f, 354.69f, 368.71f, 355.69f, 375.71f)
            curveTo(356.69f, 382.71f, 361.69f, 394.71f, 362.19f, 397.71f)
            curveTo(362.69f, 400.71f, 371.69f, 418.21f, 374.19f, 421.21f)
            curveTo(376.19f, 423.61f, 377.69f, 421.54f, 378.19f, 420.21f)
            curveTo(376.52f, 415.71f, 373.09f, 406.21f, 372.69f, 404.21f)
            curveTo(372.29f, 402.21f, 373.19f, 402.71f, 373.69f, 403.21f)
            lineTo(383.69f, 426.21f)
            curveTo(387.69f, 430.21f, 389.69f, 426.54f, 390.19f, 424.21f)
            curveTo(386.35f, 415.71f, 378.59f, 398.41f, 378.19f, 397.21f)
            curveTo(377.79f, 396.01f, 378.35f, 396.38f, 378.69f, 396.71f)
            lineTo(392.19f, 426.71f)
            curveTo(397.39f, 430.31f, 398.35f, 425.21f, 398.19f, 422.21f)
            lineTo(384.19f, 391.21f)
            curveTo(387.69f, 399.71f, 395.59f, 417.11f, 399.19f, 418.71f)
            curveTo(402.79f, 420.31f, 403.02f, 416.71f, 402.69f, 414.71f)
            lineTo(388.19f, 374.21f)
            curveTo(391.85f, 376.71f, 400.49f, 381.71f, 405.69f, 381.71f)
            curveTo(410.89f, 381.71f, 410.52f, 377.71f, 409.69f, 375.71f)
            curveTo(405.85f, 374.38f, 396.69f, 370.01f, 390.69f, 363.21f)
            curveTo(384.69f, 356.41f, 376.52f, 354.38f, 373.19f, 354.21f)
            curveTo(363.19f, 345.41f, 354.35f, 323.54f, 351.19f, 313.71f)
            curveTo(347.99f, 292.11f, 328.19f, 261.71f, 318.69f, 249.21f)
            curveTo(321.49f, 231.21f, 312.52f, 203.38f, 307.69f, 191.71f)
            verticalLineTo(156.71f)
            curveTo(301.19f, 123.71f, 259.69f, 124.21f, 257.19f, 124.21f)
            curveTo(254.69f, 124.21f, 238.19f, 112.71f, 233.19f, 107.21f)
            curveTo(229.19f, 102.81f, 229.52f, 97.38f, 230.19f, 95.21f)
            lineTo(234.19f, 89.71f)
            lineTo(235.69f, 82.21f)
            lineTo(236.19f, 74.71f)
            curveTo(237.39f, 75.11f, 238.02f, 74.21f, 238.19f, 73.71f)
            curveTo(239.52f, 69.54f, 242.49f, 60.61f, 243.69f, 58.21f)
            curveTo(244.89f, 55.81f, 240.85f, 53.54f, 238.69f, 52.71f)
            curveTo(243.19f, 15.71f, 220.19f, -6.29f, 194.19f, 3.21f)
            curveTo(173.39f, 10.81f, 170.85f, 40.04f, 172.19f, 53.71f)
            curveTo(166.99f, 54.91f, 166.35f, 56.54f, 166.69f, 57.21f)
            curveTo(167.52f, 59.71f, 169.59f, 65.91f, 171.19f, 70.71f)
            curveTo(172.79f, 75.51f, 174.52f, 76.04f, 175.19f, 75.71f)
            lineTo(176.69f, 89.71f)
            lineTo(182.19f, 95.21f)
            curveTo(183.02f, 97.54f, 183.79f, 103.21f, 180.19f, 107.21f)
            curveTo(176.59f, 111.21f, 160.69f, 120.21f, 153.19f, 124.21f)
            curveTo(116.39f, 126.21f, 104.52f, 146.38f, 103.19f, 156.21f)
            verticalLineTo(191.71f)
            curveTo(96.39f, 198.91f, 93.02f, 234.04f, 92.19f, 250.71f)
            curveTo(90.35f, 253.21f, 84.79f, 260.71f, 77.19f, 270.71f)
            curveTo(67.69f, 283.21f, 65.19f, 296.21f, 59.19f, 316.21f)
            curveTo(54.39f, 332.21f, 42.19f, 349.21f, 36.69f, 355.71f)
            curveTo(33.49f, 355.31f, 30.02f, 356.21f, 28.69f, 356.71f)
            lineTo(1.19f, 375.71f)
            curveTo(0.69f, 378.21f, 0.89f, 383.01f, 5.69f, 382.21f)
            curveTo(11.69f, 381.21f, 21.69f, 374.21f, 23.19f, 373.71f)
            curveTo(24.39f, 373.31f, 24.02f, 373.88f, 23.69f, 374.21f)
            lineTo(19.69f, 384.71f)
            curveTo(16.69f, 392.21f, 10.29f, 408.61f, 8.69f, 414.21f)
            curveTo(7.09f, 419.81f, 10.69f, 419.21f, 12.69f, 418.21f)
            lineTo(24.19f, 395.21f)
            curveTo(25.79f, 393.21f, 26.19f, 394.71f, 26.19f, 395.71f)
            curveTo(22.52f, 403.21f, 14.69f, 419.31f, 12.69f, 423.71f)
            curveTo(10.69f, 428.11f, 15.52f, 428.21f, 18.19f, 427.71f)
            lineTo(30.19f, 401.21f)
            curveTo(32.59f, 398.41f, 32.19f, 400.71f, 31.69f, 402.21f)
            curveTo(28.85f, 409.21f, 22.89f, 423.51f, 21.69f, 424.71f)
            curveTo(20.19f, 426.21f, 23.19f, 431.71f, 26.19f, 428.71f)
            curveTo(28.59f, 426.31f, 34.19f, 411.71f, 36.69f, 404.71f)
            curveTo(38.29f, 402.71f, 38.69f, 403.88f, 38.69f, 404.71f)
            curveTo(38.85f, 405.04f, 38.29f, 407.71f, 34.69f, 415.71f)
            curveTo(31.09f, 423.71f, 35.52f, 422.71f, 38.19f, 421.21f)
            curveTo(48.59f, 407.21f, 53.52f, 384.71f, 54.69f, 375.21f)
            lineTo(61.19f, 357.71f)
            curveTo(61.69f, 356.38f, 68.19f, 347.41f, 90.19f, 322.21f)
            curveTo(112.19f, 297.01f, 119.02f, 271.38f, 119.69f, 261.71f)
            curveTo(126.49f, 256.51f, 132.85f, 234.21f, 135.19f, 223.71f)
            curveTo(134.79f, 226.91f, 147.69f, 252.04f, 154.19f, 264.21f)
            curveTo(154.19f, 270.71f, 154.19f, 285.91f, 154.19f, 294.71f)
            curveTo(154.19f, 303.51f, 152.5f, 327.78f, 150f, 333.94f)
            curveTo(150f, 333.94f, 164f, 340.44f, 173.5f, 348.44f)
            curveTo(185.97f, 358.95f, 195.72f, 373.71f, 195.72f, 373.71f)
            horizontalLineTo(218f)
            curveTo(235.97f, 348.44f, 237.31f, 351.74f, 262.5f, 330.44f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 215f)
            curveTo(185f, 211f, 194f, 210f, 199f, 213f)
            curveTo(202f, 215f, 202f, 221f, 202f, 225f)
            curveTo(202f, 230f, 203f, 236f, 198f, 237f)
            curveTo(192f, 236f, 184f, 236f, 178f, 237f)
            curveTo(174f, 237f, 174f, 232f, 174f, 228f)
            curveTo(174f, 222f, 176f, 218f, 180f, 215f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 215f)
            curveTo(226f, 211f, 217f, 210f, 212f, 213f)
            curveTo(209f, 215f, 209f, 221f, 209f, 225f)
            curveTo(209f, 230f, 208f, 236f, 213f, 237f)
            curveTo(219f, 236f, 227f, 236f, 233f, 237f)
            curveTo(237f, 237f, 237f, 232f, 237f, 228f)
            curveTo(237f, 222f, 235f, 218f, 231f, 215f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 247f)
            curveTo(185f, 244f, 195f, 243f, 199f, 246f)
            curveTo(202f, 248f, 202f, 253f, 202f, 257f)
            curveTo(202f, 262f, 203f, 267f, 198f, 268f)
            curveTo(192f, 267f, 184f, 267f, 178f, 268f)
            curveTo(174f, 268f, 174f, 264f, 174f, 260f)
            curveTo(174f, 254f, 176f, 250f, 180f, 247f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 247f)
            curveTo(226f, 244f, 216f, 243f, 212f, 246f)
            curveTo(209f, 248f, 209f, 253f, 209f, 257f)
            curveTo(209f, 262f, 208f, 267f, 213f, 268f)
            curveTo(219f, 267f, 227f, 267f, 233f, 268f)
            curveTo(237f, 268f, 237f, 264f, 237f, 260f)
            curveTo(237f, 254f, 235f, 250f, 231f, 247f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(180f, 278f)
            curveTo(185f, 275f, 195f, 274f, 199f, 277f)
            curveTo(202f, 279f, 202f, 284f, 202f, 288f)
            curveTo(202f, 293f, 203f, 298f, 198f, 299f)
            curveTo(192f, 298f, 184f, 298f, 179f, 299f)
            curveTo(175f, 299f, 175f, 295f, 175f, 291f)
            curveTo(175f, 285f, 176f, 281f, 180f, 278f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(231f, 278f)
            curveTo(226f, 275f, 216f, 274f, 212f, 277f)
            curveTo(209f, 279f, 209f, 284f, 209f, 288f)
            curveTo(209f, 293f, 208f, 298f, 213f, 299f)
            curveTo(219f, 298f, 227f, 298f, 232f, 299f)
            curveTo(236f, 299f, 236f, 295f, 236f, 291f)
            curveTo(236f, 285f, 235f, 281f, 231f, 278f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(183f, 306f)
            curveTo(188f, 304f, 195f, 305f, 199f, 308f)
            curveTo(202f, 311f, 202f, 317f, 202f, 325f)
            curveTo(202f, 332f, 202f, 342f, 201f, 348f)
            curveTo(200f, 355f, 194f, 356f, 190f, 350f)
            curveTo(183f, 339f, 180f, 326f, 179f, 316f)
            curveTo(178f, 310f, 179f, 307f, 183f, 306f)
            close()
        }
        path(fill = SolidColor(rectusAbdominis)) {
            moveTo(228f, 306f)
            curveTo(223f, 304f, 216f, 305f, 212f, 308f)
            curveTo(209f, 311f, 209f, 317f, 209f, 325f)
            curveTo(209f, 332f, 209f, 342f, 210f, 348f)
            curveTo(211f, 355f, 217f, 356f, 221f, 350f)
            curveTo(228f, 339f, 231f, 326f, 232f, 316f)
            curveTo(233f, 310f, 232f, 307f, 228f, 306f)
            close()
        }
        path(fill = SolidColor(obliquesAbdominis)) {
            moveTo(154f, 207f)
            curveTo(158f, 205f, 163f, 208f, 166f, 214f)
            curveTo(171f, 225f, 170f, 240f, 168f, 256f)
            curveTo(166f, 272f, 168f, 303f, 169f, 322f)
            curveTo(170f, 330f, 164f, 331f, 160f, 326f)
            curveTo(157f, 322f, 156f, 316f, 156f, 308f)
            curveTo(157f, 295f, 157f, 280f, 157f, 269f)
            curveTo(157f, 254f, 148f, 242f, 149f, 230f)
            curveTo(150f, 220f, 149f, 210f, 154f, 207f)
            close()
        }
        path(fill = SolidColor(obliquesAbdominis)) {
            moveTo(257f, 207f)
            curveTo(253f, 205f, 248f, 208f, 245f, 214f)
            curveTo(240f, 225f, 241f, 240f, 243f, 256f)
            curveTo(245f, 272f, 243f, 303f, 242f, 322f)
            curveTo(241f, 330f, 247f, 331f, 251f, 326f)
            curveTo(254f, 322f, 255f, 316f, 255f, 308f)
            curveTo(254f, 295f, 254f, 280f, 254f, 269f)
            curveTo(254f, 254f, 263f, 242f, 262f, 230f)
            curveTo(261f, 220f, 262f, 210f, 257f, 207f)
            close()
        }
        path(fill = SolidColor(lateralDeltoid)) {
            moveTo(113f, 151f)
            curveTo(116f, 144f, 122f, 140f, 129f, 139f)
            curveTo(134f, 138f, 135f, 142f, 131f, 146f)
            curveTo(124f, 154f, 118f, 163f, 114f, 172f)
            curveTo(111f, 178f, 108f, 175f, 109f, 169f)
            curveTo(109f, 163f, 110f, 157f, 113f, 151f)
            close()
        }
        path(fill = SolidColor(lateralDeltoid)) {
            moveTo(298f, 151f)
            curveTo(295f, 144f, 289f, 140f, 282f, 139f)
            curveTo(277f, 138f, 276f, 142f, 280f, 146f)
            curveTo(287f, 154f, 293f, 163f, 297f, 172f)
            curveTo(300f, 178f, 303f, 175f, 302f, 169f)
            curveTo(302f, 163f, 301f, 157f, 298f, 151f)
            close()
        }
        path(fill = SolidColor(anteriorDeltoid)) {
            moveTo(141f, 145f)
            curveTo(147f, 141f, 151f, 142f, 149f, 148f)
            curveTo(145f, 159f, 135f, 170f, 124f, 176f)
            curveTo(119f, 179f, 116f, 176f, 119f, 171f)
            curveTo(123f, 161f, 133f, 150f, 141f, 145f)
            close()
        }
        path(fill = SolidColor(anteriorDeltoid)) {
            moveTo(270f, 145f)
            curveTo(264f, 141f, 260f, 142f, 262f, 148f)
            curveTo(266f, 159f, 276f, 170f, 287f, 176f)
            curveTo(292f, 179f, 295f, 176f, 292f, 171f)
            curveTo(288f, 161f, 278f, 150f, 270f, 145f)
            close()
        }
        path(fill = SolidColor(biceps)) {
            moveTo(117f, 188f)
            curveTo(123f, 185f, 129f, 188f, 130f, 195f)
            curveTo(131f, 209f, 121f, 229f, 110f, 239f)
            curveTo(104f, 246f, 99f, 244f, 100f, 236f)
            curveTo(101f, 217f, 105f, 194f, 117f, 188f)
            close()
        }
        path(fill = SolidColor(biceps)) {
            moveTo(294f, 188f)
            curveTo(288f, 185f, 282f, 188f, 281f, 195f)
            curveTo(280f, 209f, 290f, 229f, 301f, 239f)
            curveTo(307f, 246f, 312f, 244f, 311f, 236f)
            curveTo(310f, 217f, 306f, 194f, 294f, 188f)
            close()
        }
        path(fill = SolidColor(forearm)) {
            moveTo(95f, 266f)
            curveTo(101f, 262f, 108f, 267f, 109f, 274f)
            curveTo(109f, 286f, 98f, 304f, 87f, 317f)
            curveTo(78f, 327f, 69f, 337f, 64f, 339f)
            curveTo(58f, 342f, 56f, 338f, 59f, 332f)
            curveTo(67f, 313f, 74f, 288f, 86f, 274f)
            curveTo(90f, 270f, 92f, 268f, 95f, 266f)
            close()
        }
        path(fill = SolidColor(forearm)) {
            moveTo(316f, 266f)
            curveTo(310f, 262f, 303f, 267f, 302f, 274f)
            curveTo(302f, 286f, 313f, 304f, 324f, 317f)
            curveTo(333f, 327f, 342f, 337f, 347f, 339f)
            curveTo(353f, 342f, 355f, 338f, 352f, 332f)
            curveTo(344f, 313f, 337f, 288f, 325f, 274f)
            curveTo(321f, 270f, 319f, 268f, 316f, 266f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorClavicular)) {
            moveTo(152f, 153f)
            curveTo(159f, 145f, 168f, 141f, 177f, 142f)
            curveTo(186f, 143f, 195f, 147f, 199f, 152f)
            curveTo(202f, 156f, 203f, 163f, 198f, 164f)
            curveTo(185f, 158f, 168f, 158f, 155f, 165f)
            curveTo(150f, 168f, 146f, 166f, 148f, 161f)
            curveTo(149f, 158f, 150f, 155f, 152f, 153f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorClavicular)) {
            moveTo(259f, 153f)
            curveTo(252f, 145f, 243f, 141f, 234f, 142f)
            curveTo(225f, 143f, 216f, 147f, 212f, 152f)
            curveTo(209f, 156f, 208f, 163f, 213f, 164f)
            curveTo(226f, 158f, 243f, 158f, 256f, 165f)
            curveTo(261f, 168f, 265f, 166f, 263f, 161f)
            curveTo(262f, 158f, 261f, 155f, 259f, 153f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorSternocostal)) {
            moveTo(153f, 173f)
            curveTo(164f, 165f, 181f, 164f, 195f, 170f)
            curveTo(200f, 172f, 202f, 176f, 202f, 182f)
            curveTo(202f, 187f, 203f, 192f, 197f, 193f)
            curveTo(181f, 190f, 165f, 185f, 153f, 180f)
            curveTo(148f, 178f, 149f, 176f, 153f, 173f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorSternocostal)) {
            moveTo(258f, 173f)
            curveTo(247f, 165f, 230f, 164f, 216f, 170f)
            curveTo(211f, 172f, 209f, 176f, 209f, 182f)
            curveTo(209f, 187f, 208f, 192f, 214f, 193f)
            curveTo(230f, 190f, 246f, 185f, 258f, 180f)
            curveTo(263f, 178f, 262f, 176f, 258f, 173f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorAbdominal)) {
            moveTo(150f, 187f)
            curveTo(160f, 189f, 176f, 195f, 190f, 198f)
            curveTo(197f, 200f, 198f, 204f, 191f, 206f)
            curveTo(175f, 207f, 160f, 201f, 151f, 195f)
            curveTo(145f, 191f, 145f, 186f, 150f, 187f)
            close()
        }
        path(fill = SolidColor(pectoralisMajorAbdominal)) {
            moveTo(261f, 187f)
            curveTo(251f, 189f, 235f, 195f, 221f, 198f)
            curveTo(214f, 200f, 213f, 204f, 220f, 206f)
            curveTo(236f, 207f, 251f, 201f, 260f, 195f)
            curveTo(266f, 191f, 266f, 186f, 261f, 187f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(187f, 105f)
            curveTo(188f, 102f, 190f, 106f, 192f, 111f)
            curveTo(196f, 120f, 199f, 128f, 200f, 135f)
            curveTo(201f, 140f, 199f, 141f, 197f, 137f)
            curveTo(192f, 129f, 186f, 116f, 187f, 105f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(224f, 105f)
            curveTo(223f, 102f, 221f, 106f, 219f, 111f)
            curveTo(215f, 120f, 212f, 128f, 211f, 135f)
            curveTo(210f, 140f, 212f, 141f, 214f, 137f)
            curveTo(219f, 129f, 225f, 116f, 224f, 105f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(177f, 119f)
            curveTo(182f, 116f, 184f, 118f, 180f, 122f)
            curveTo(174f, 128f, 163f, 131f, 157f, 130f)
            curveTo(153f, 129f, 157f, 126f, 160f, 125f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(234f, 119f)
            curveTo(229f, 116f, 227f, 118f, 231f, 122f)
            curveTo(237f, 128f, 248f, 131f, 254f, 130f)
            curveTo(258f, 129f, 254f, 126f, 251f, 125f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(139f, 196f)
            curveTo(141f, 193f, 143f, 198f, 144f, 205f)
            curveTo(146f, 215f, 146f, 224f, 144f, 229f)
            curveTo(142f, 233f, 140f, 229f, 140f, 224f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(272f, 196f)
            curveTo(270f, 193f, 268f, 198f, 267f, 205f)
            curveTo(265f, 215f, 265f, 224f, 267f, 229f)
            curveTo(269f, 233f, 271f, 229f, 271f, 224f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(125f, 238f)
            curveTo(128f, 232f, 130f, 235f, 128f, 241f)
            curveTo(125f, 251f, 115f, 257f, 108f, 260f)
            curveTo(104f, 262f, 104f, 258f, 108f, 255f)
            curveTo(116f, 248f, 120f, 243f, 125f, 238f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(286f, 238f)
            curveTo(283f, 232f, 281f, 235f, 283f, 241f)
            curveTo(286f, 251f, 296f, 257f, 303f, 260f)
            curveTo(307f, 262f, 307f, 258f, 303f, 255f)
            curveTo(295f, 248f, 291f, 243f, 286f, 238f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(40.19f, 396.21f)
            lineTo(28.19f, 383.21f)
            curveTo(26.59f, 382.01f, 26.52f, 380.04f, 26.69f, 379.21f)
            lineTo(33.69f, 368.21f)
            curveTo(39.52f, 361.21f, 51.79f, 349.81f, 54.19f, 360.21f)
            curveTo(57.19f, 373.21f, 49.19f, 388.21f, 47.19f, 394.21f)
            curveTo(45.59f, 399.01f, 41.85f, 397.54f, 40.19f, 396.21f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(35.69f, 362.21f)
            curveTo(18.09f, 376.21f, 6.69f, 378.71f, 3.19f, 378.21f)
            curveTo(2.79f, 377.01f, 3.69f, 376.38f, 4.19f, 376.21f)
            lineTo(31.69f, 358.21f)
            curveTo(37.69f, 356.61f, 36.85f, 360.21f, 35.69f, 362.21f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(11.69f, 410.21f)
            lineTo(22.69f, 386.21f)
            curveTo(24.02f, 385.88f, 26.69f, 385.81f, 26.69f, 388.21f)
            curveTo(26.69f, 391.21f, 16.69f, 408.21f, 14.19f, 413.71f)
            curveTo(12.19f, 418.11f, 11.69f, 413.21f, 11.69f, 410.21f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(30.19f, 398.71f)
            curveTo(31.79f, 392.71f, 29.19f, 393.54f, 27.69f, 394.71f)
            curveTo(26.92f, 395.21f, 17.69f, 418.21f, 16.69f, 421.71f)
            curveTo(15.89f, 424.51f, 18.35f, 424.88f, 19.69f, 424.71f)
            curveTo(22.52f, 418.54f, 28.59f, 404.71f, 30.19f, 398.71f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 403.71f)
            curveTo(37.89f, 400.91f, 35.52f, 399.54f, 34.19f, 399.21f)
            lineTo(32.69f, 400.21f)
            curveTo(30.19f, 405.71f, 25.09f, 417.51f, 24.69f, 420.71f)
            curveTo(24.29f, 423.91f, 26.85f, 423.71f, 28.19f, 423.21f)
            curveTo(30.52f, 417.88f, 35.49f, 406.51f, 36.69f, 403.71f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.69f, 413.21f)
            lineTo(42.69f, 401.71f)
            curveTo(45.49f, 400.11f, 45.52f, 403.38f, 45.19f, 405.21f)
            curveTo(44.35f, 407.38f, 42.09f, 412.61f, 39.69f, 416.21f)
            curveTo(37.29f, 419.81f, 36.69f, 415.71f, 36.69f, 413.21f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(372.35f, 396.9f)
            lineTo(384.35f, 383.9f)
            curveTo(385.95f, 382.7f, 386.02f, 380.73f, 385.85f, 379.9f)
            lineTo(378.85f, 368.9f)
            curveTo(373.02f, 361.9f, 360.75f, 350.5f, 358.35f, 360.9f)
            curveTo(355.35f, 373.9f, 363.35f, 388.9f, 365.35f, 394.9f)
            curveTo(366.95f, 399.7f, 370.69f, 398.23f, 372.35f, 396.9f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(376.85f, 362.9f)
            curveTo(394.45f, 376.9f, 405.85f, 379.4f, 409.35f, 378.9f)
            curveTo(409.75f, 377.7f, 408.85f, 377.06f, 408.35f, 376.9f)
            lineTo(380.85f, 358.9f)
            curveTo(374.85f, 357.3f, 375.69f, 360.9f, 376.85f, 362.9f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(400.85f, 410.9f)
            lineTo(389.85f, 386.9f)
            curveTo(388.52f, 386.56f, 385.85f, 386.5f, 385.85f, 388.9f)
            curveTo(385.85f, 391.9f, 395.85f, 408.9f, 398.35f, 414.4f)
            curveTo(400.35f, 418.8f, 400.85f, 413.9f, 400.85f, 410.9f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(382.35f, 399.4f)
            curveTo(380.75f, 393.4f, 383.35f, 394.23f, 384.85f, 395.4f)
            curveTo(385.62f, 395.9f, 394.85f, 418.9f, 395.85f, 422.4f)
            curveTo(396.65f, 425.2f, 394.19f, 425.56f, 392.85f, 425.4f)
            curveTo(390.02f, 419.23f, 383.95f, 405.4f, 382.35f, 399.4f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(375.85f, 404.4f)
            curveTo(374.65f, 401.6f, 377.02f, 400.23f, 378.35f, 399.9f)
            lineTo(379.85f, 400.9f)
            curveTo(382.35f, 406.4f, 387.45f, 418.2f, 387.85f, 421.4f)
            curveTo(388.25f, 424.6f, 385.69f, 424.4f, 384.35f, 423.9f)
            curveTo(382.02f, 418.56f, 377.05f, 407.2f, 375.85f, 404.4f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(375.85f, 413.9f)
            lineTo(369.85f, 402.4f)
            curveTo(367.05f, 400.8f, 367.02f, 404.06f, 367.35f, 405.9f)
            curveTo(368.19f, 408.06f, 370.45f, 413.3f, 372.85f, 416.9f)
            curveTo(375.25f, 420.5f, 375.85f, 416.4f, 375.85f, 413.9f)
            close()
        }
    }.build()
}
