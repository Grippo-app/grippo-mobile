package com.grippo.design.resources.provider.muscles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public fun bodyBack(
    colors: MuscleColorPreset
): ImageVector {

    val outlineColor: Color = colors.outline
    val backgroundBack: Color = colors.backgroundBack
    val rhomboids: Color = colors.rhomboids
    val latissimus: Color = colors.latissimus
    val trapezius: Color = colors.trapezius
    val triceps: Color = colors.triceps
    val forearm: Color = colors.forearm
    val teresMajor: Color = colors.teresMajor
    val posteriorDeltoid: Color = colors.posteriorDeltoid
    val other: Color = colors.other

    return ImageVector.Builder(
        name = "Bodyback",
        defaultWidth = 411.dp,
        defaultHeight = 430.dp,
        viewportWidth = 411f,
        viewportHeight = 430f
    ).apply {
        path(
            fill = SolidColor(backgroundBack),
            stroke = SolidColor(outlineColor),
            strokeLineWidth = 1f
        ) {
            moveTo(182.59f, 86.77f)
            verticalLineTo(93.77f)
            moveTo(182.59f, 93.77f)
            verticalLineTo(104.77f)
            curveTo(178.75f, 110.93f, 163.38f, 124.47f, 132.59f, 129.27f)
            curveTo(101.79f, 134.07f, 101.08f, 172.27f, 104.58f, 190.77f)
            curveTo(94.18f, 213.17f, 91.92f, 241.77f, 92.08f, 253.27f)
            curveTo(84.58f, 260.1f, 68.48f, 279.57f, 64.08f, 302.77f)
            curveTo(59.68f, 325.97f, 43.92f, 348.43f, 36.58f, 356.77f)
            curveTo(32.18f, 356.77f, 29.42f, 358.1f, 28.58f, 358.77f)
            curveTo(20.25f, 364.6f, 3.28f, 376.37f, 2.08f, 376.77f)
            curveTo(0.88f, 377.17f, 0.92f, 378.27f, 1.08f, 378.77f)
            curveTo(2.68f, 385.17f, 9.75f, 383.43f, 13.08f, 381.77f)
            curveTo(16.58f, 379.1f, 23.58f, 374.17f, 23.58f, 375.77f)
            curveTo(23.58f, 377.77f, 12.58f, 405.77f, 9.08f, 414.27f)
            curveTo(6.28f, 421.07f, 11.25f, 420.43f, 14.08f, 419.27f)
            lineTo(25.08f, 394.77f)
            curveTo(22.75f, 399.93f, 17.18f, 412.47f, 13.58f, 421.27f)
            curveTo(9.98f, 430.07f, 15.75f, 429.6f, 19.08f, 428.27f)
            lineTo(31.08f, 400.27f)
            lineTo(21.08f, 424.27f)
            curveTo(20.68f, 431.87f, 25.58f, 429.1f, 28.08f, 426.77f)
            curveTo(30.58f, 420.27f, 35.98f, 406.47f, 37.58f, 403.27f)
            curveTo(39.18f, 400.07f, 39.58f, 401.93f, 39.58f, 403.27f)
            lineTo(34.08f, 417.77f)
            curveTo(32.88f, 424.17f, 35.92f, 422.77f, 37.58f, 421.27f)
            curveTo(44.78f, 412.87f, 51.58f, 390.43f, 54.08f, 380.27f)
            lineTo(62.08f, 358.27f)
            curveTo(112.49f, 299.07f, 121.75f, 270.27f, 120.08f, 263.27f)
            curveTo(126.89f, 257.67f, 133.59f, 234.93f, 136.09f, 224.27f)
            lineTo(154.59f, 266.27f)
            lineTo(154.09f, 305.27f)
            lineTo(152.59f, 309.77f)
            curveTo(148.5f, 325f, 146f, 337.5f, 145.09f, 342.77f)
            lineTo(141f, 365.27f)
            lineTo(196f, 395.5f)
            horizontalLineTo(214f)
            lineTo(270.08f, 365.27f)
            curveTo(268.89f, 334.87f, 261.25f, 310.93f, 257.58f, 302.77f)
            lineTo(257.08f, 264.77f)
            curveTo(263.89f, 259.17f, 272.58f, 233.43f, 276.08f, 221.27f)
            curveTo(277.29f, 236.87f, 287.25f, 256.77f, 292.08f, 264.77f)
            curveTo(289.68f, 277.17f, 304.42f, 300.93f, 312.08f, 311.27f)
            lineTo(352.08f, 361.77f)
            lineTo(352.58f, 365.27f)
            curveTo(354.18f, 380.07f, 367.58f, 408.77f, 374.08f, 421.27f)
            curveTo(377.68f, 424.47f, 377.58f, 420.6f, 377.08f, 418.27f)
            curveTo(375.92f, 414.43f, 373.39f, 406.17f, 372.58f, 403.77f)
            curveTo(371.79f, 401.37f, 373.25f, 402.43f, 374.08f, 403.27f)
            lineTo(383.08f, 426.77f)
            curveTo(389.08f, 431.97f, 390.25f, 426.93f, 390.08f, 423.77f)
            lineTo(379.08f, 397.27f)
            lineTo(391.08f, 425.27f)
            curveTo(398.68f, 432.07f, 397.58f, 422.77f, 396.08f, 417.27f)
            lineTo(384.08f, 390.77f)
            lineTo(397.08f, 418.27f)
            curveTo(402.68f, 423.07f, 403.08f, 417.93f, 402.58f, 414.77f)
            lineTo(387.58f, 374.77f)
            curveTo(390.75f, 376.93f, 398.58f, 381.47f, 404.58f, 382.27f)
            curveTo(410.58f, 383.07f, 410.08f, 378.93f, 409.08f, 376.77f)
            lineTo(390.08f, 364.27f)
            curveTo(382.89f, 355.87f, 376.42f, 354.77f, 374.08f, 355.27f)
            curveTo(362.89f, 346.87f, 354.08f, 325.1f, 351.08f, 315.27f)
            curveTo(343.89f, 283.67f, 326.42f, 259.1f, 318.58f, 250.77f)
            curveTo(320.98f, 235.97f, 311.92f, 204.6f, 307.08f, 190.77f)
            curveTo(314.29f, 144.37f, 290.42f, 129.77f, 277.58f, 128.27f)
            curveTo(256.39f, 129.07f, 238.09f, 112.93f, 231.59f, 104.77f)
            verticalLineTo(88.27f)
            verticalLineTo(93.77f)
            horizontalLineTo(233.59f)
            lineTo(238.09f, 71.77f)
            horizontalLineTo(241.09f)
            curveTo(242.42f, 66.93f, 245.29f, 56.67f, 246.09f, 54.27f)
            curveTo(246.88f, 51.87f, 242.42f, 49.6f, 240.09f, 48.77f)
            curveTo(241.75f, 31.43f, 236.99f, -2.33f, 204.59f, 1.27f)
            curveTo(172.18f, 4.87f, 170.09f, 36.1f, 173.09f, 51.27f)
            curveTo(167.49f, 51.27f, 166.75f, 53.27f, 167.09f, 54.27f)
            lineTo(173.09f, 72.77f)
            horizontalLineTo(174.59f)
            lineTo(180.09f, 92.77f)
            lineTo(182.59f, 93.77f)
            close()
        }
        path(fill = SolidColor(rhomboids)) {
            moveTo(195f, 280f)
            curveTo(190f, 282f, 182f, 294f, 178f, 300f)
            curveTo(175f, 305f, 186f, 316f, 192f, 322f)
            curveTo(195f, 325f, 198f, 323f, 198f, 317f)
            curveTo(199f, 305f, 199f, 292f, 198f, 284f)
            curveTo(198f, 281f, 197f, 279f, 195f, 280f)
            close()
        }
        path(fill = SolidColor(rhomboids)) {
            moveTo(216f, 280f)
            curveTo(221f, 282f, 229f, 294f, 233f, 300f)
            curveTo(236f, 305f, 225f, 316f, 219f, 322f)
            curveTo(216f, 325f, 213f, 323f, 213f, 317f)
            curveTo(212f, 305f, 212f, 292f, 213f, 284f)
            curveTo(213f, 281f, 214f, 279f, 216f, 280f)
            close()
        }
        path(fill = SolidColor(latissimus)) {
            moveTo(139f, 197f)
            curveTo(149f, 195f, 160f, 197f, 166f, 202f)
            curveTo(175f, 210f, 184f, 218f, 191f, 222f)
            curveTo(198f, 228f, 197f, 241f, 193f, 254f)
            curveTo(189f, 265f, 182f, 275f, 177f, 277f)
            curveTo(173f, 279f, 170f, 278f, 168f, 273f)
            curveTo(164f, 261f, 157f, 250f, 150f, 239f)
            curveTo(140f, 224f, 136f, 210f, 137f, 202f)
            curveTo(137f, 199f, 138f, 197f, 139f, 197f)
            close()
        }
        path(fill = SolidColor(latissimus)) {
            moveTo(272f, 197f)
            curveTo(262f, 195f, 251f, 197f, 245f, 202f)
            curveTo(236f, 210f, 227f, 218f, 220f, 222f)
            curveTo(213f, 228f, 214f, 241f, 218f, 254f)
            curveTo(222f, 265f, 229f, 275f, 234f, 277f)
            curveTo(238f, 279f, 241f, 278f, 243f, 273f)
            curveTo(247f, 261f, 254f, 250f, 261f, 239f)
            curveTo(271f, 224f, 275f, 210f, 274f, 202f)
            curveTo(274f, 199f, 273f, 197f, 272f, 197f)
            close()
        }
        path(fill = SolidColor(trapezius)) {
            moveTo(197f, 88f)
            curveTo(196f, 103f, 185f, 118f, 164f, 128f)
            curveTo(159f, 130f, 154f, 132f, 152f, 135f)
            curveTo(149f, 137f, 153f, 140f, 158f, 142f)
            curveTo(167f, 148f, 171f, 166f, 180f, 181f)
            curveTo(186f, 191f, 191f, 197f, 196f, 201f)
            curveTo(200f, 204f, 202f, 201f, 202f, 195f)
            curveTo(203f, 179f, 203f, 163f, 202f, 147f)
            curveTo(201f, 127f, 203f, 104f, 203f, 91f)
            curveTo(203f, 85f, 199f, 82f, 197f, 88f)
            close()
        }
        path(fill = SolidColor(trapezius)) {
            moveTo(214f, 88f)
            curveTo(215f, 103f, 226f, 118f, 247f, 128f)
            curveTo(252f, 130f, 257f, 132f, 259f, 135f)
            curveTo(262f, 137f, 258f, 140f, 253f, 142f)
            curveTo(244f, 148f, 240f, 166f, 231f, 181f)
            curveTo(225f, 191f, 220f, 197f, 215f, 201f)
            curveTo(211f, 204f, 209f, 201f, 209f, 195f)
            curveTo(208f, 179f, 208f, 163f, 209f, 147f)
            curveTo(210f, 127f, 208f, 104f, 208f, 91f)
            curveTo(208f, 85f, 212f, 82f, 214f, 88f)
            close()
        }
        path(fill = SolidColor(posteriorDeltoid)) {
            moveTo(117f, 177f)
            curveTo(112f, 172f, 111f, 158f, 118f, 151f)
            curveTo(124f, 144f, 134f, 144f, 140f, 147f)
            curveTo(145f, 149f, 137f, 159f, 129f, 167f)
            curveTo(123f, 173f, 120f, 179f, 117f, 177f)
            close()
        }
        path(fill = SolidColor(posteriorDeltoid)) {
            moveTo(294f, 177f)
            curveTo(299f, 172f, 300f, 158f, 293f, 151f)
            curveTo(287f, 144f, 277f, 144f, 271f, 147f)
            curveTo(266f, 149f, 274f, 159f, 282f, 167f)
            curveTo(288f, 173f, 291f, 179f, 294f, 177f)
            close()
        }
        path(fill = SolidColor(teresMajor)) {
            moveTo(151f, 159f)
            curveTo(147f, 158f, 139f, 169f, 137f, 176f)
            curveTo(135f, 179f, 140f, 181f, 146f, 182f)
            curveTo(153f, 184f, 159f, 185f, 160f, 181f)
            curveTo(162f, 177f, 157f, 165f, 154f, 161f)
            curveTo(153f, 159f, 152f, 158f, 151f, 159f)
            close()
        }
        path(fill = SolidColor(teresMajor)) {
            moveTo(260f, 159f)
            curveTo(264f, 158f, 272f, 169f, 274f, 176f)
            curveTo(276f, 179f, 271f, 181f, 265f, 182f)
            curveTo(258f, 184f, 252f, 185f, 251f, 181f)
            curveTo(249f, 177f, 254f, 165f, 257f, 161f)
            curveTo(258f, 159f, 259f, 158f, 260f, 159f)
            close()
        }
        path(fill = SolidColor(triceps)) {
            moveTo(120f, 188f)
            curveTo(126f, 185f, 128f, 190f, 129f, 199f)
            curveTo(132f, 213f, 127f, 228f, 119f, 239f)
            curveTo(114f, 246f, 107f, 250f, 105f, 244f)
            curveTo(101f, 237f, 105f, 217f, 108f, 207f)
            curveTo(111f, 197f, 115f, 190f, 120f, 188f)
            close()
        }
        path(fill = SolidColor(triceps)) {
            moveTo(291f, 188f)
            curveTo(285f, 185f, 283f, 190f, 282f, 199f)
            curveTo(279f, 213f, 284f, 228f, 292f, 239f)
            curveTo(297f, 246f, 304f, 250f, 306f, 244f)
            curveTo(310f, 237f, 306f, 217f, 303f, 207f)
            curveTo(300f, 197f, 296f, 190f, 291f, 188f)
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
        path(fill = SolidColor(other)) {
            moveTo(159f, 287f)
            curveTo(160f, 284f, 163f, 287f, 166f, 295f)
            curveTo(170f, 304f, 174f, 309f, 179f, 313f)
            curveTo(183f, 316f, 182f, 320f, 177f, 322f)
            curveTo(166f, 325f, 158f, 330f, 153f, 336f)
            curveTo(150f, 340f, 148f, 339f, 149f, 334f)
            curveTo(150f, 320f, 155f, 307f, 156f, 298f)
            curveTo(156f, 292f, 157f, 289f, 159f, 287f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(252f, 287f)
            curveTo(251f, 284f, 248f, 287f, 245f, 295f)
            curveTo(241f, 304f, 237f, 309f, 232f, 313f)
            curveTo(228f, 316f, 229f, 320f, 234f, 322f)
            curveTo(245f, 325f, 253f, 330f, 258f, 336f)
            curveTo(261f, 340f, 263f, 339f, 262f, 334f)
            curveTo(261f, 320f, 256f, 307f, 255f, 298f)
            curveTo(255f, 292f, 254f, 289f, 252f, 287f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(105f, 258f)
            curveTo(102f, 259f, 100f, 262f, 100f, 266f)
            curveTo(100f, 269f, 103f, 268f, 106f, 265f)
            curveTo(109f, 262f, 111f, 259f, 109f, 258f)
            curveTo(108f, 257f, 106f, 257f, 105f, 258f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(306f, 258f)
            curveTo(309f, 259f, 311f, 262f, 311f, 266f)
            curveTo(311f, 269f, 308f, 268f, 305f, 265f)
            curveTo(302f, 262f, 300f, 259f, 302f, 258f)
            curveTo(303f, 257f, 305f, 257f, 306f, 258f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(41.58f, 357.27f)
            lineTo(26.58f, 380.77f)
            curveTo(27.08f, 381.93f, 29.68f, 385.77f, 36.08f, 391.77f)
            curveTo(42.48f, 397.77f, 44.75f, 396.6f, 45.08f, 395.27f)
            curveTo(55.48f, 384.47f, 54.75f, 365.43f, 53.08f, 357.27f)
            horizontalLineTo(41.58f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(44.58f, 401.77f)
            curveTo(44.98f, 399.77f, 43.75f, 399.93f, 43.08f, 400.27f)
            curveTo(42.58f, 401.27f, 37.58f, 410.77f, 36.58f, 414.27f)
            curveTo(35.78f, 417.07f, 37.92f, 416.43f, 39.08f, 415.77f)
            lineTo(41.08f, 412.77f)
            curveTo(42.08f, 409.93f, 44.18f, 403.77f, 44.58f, 401.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(36.58f, 400.77f)
            curveTo(37.78f, 396.77f, 35.08f, 397.43f, 33.58f, 398.27f)
            curveTo(32.78f, 398.62f, 27.25f, 413.08f, 24.58f, 420.27f)
            curveTo(24.58f, 424.27f, 26.92f, 422.93f, 28.08f, 421.77f)
            curveTo(30.42f, 416.43f, 35.38f, 404.77f, 36.58f, 400.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(27.08f, 394.27f)
            lineTo(16.58f, 420.27f)
            curveTo(16.58f, 424.67f, 18.92f, 424.43f, 20.08f, 423.77f)
            curveTo(23.25f, 416.43f, 29.88f, 400.67f, 31.08f, 396.27f)
            curveTo(32.28f, 391.87f, 28.92f, 393.1f, 27.08f, 394.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(14.58f, 413.77f)
            curveTo(12.58f, 416.97f, 12.08f, 413.43f, 12.08f, 411.27f)
            curveTo(14.25f, 404.43f, 19.28f, 389.97f, 22.08f, 386.77f)
            curveTo(24.88f, 383.57f, 25.58f, 387.1f, 25.58f, 389.27f)
            curveTo(22.75f, 396.1f, 16.58f, 410.57f, 14.58f, 413.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(31.08f, 360.27f)
            lineTo(11.08f, 371.77f)
            curveTo(10.08f, 372.6f, 7.18f, 374.77f, 3.58f, 376.77f)
            curveTo(-0.02f, 378.77f, 4.08f, 379.93f, 6.58f, 380.27f)
            horizontalLineTo(11.58f)
            lineTo(17.58f, 377.27f)
            curveTo(22.08f, 373.93f, 31.78f, 366.37f, 34.58f, 362.77f)
            curveTo(37.38f, 359.17f, 33.42f, 359.6f, 31.08f, 360.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(365.58f, 396.27f)
            curveTo(357.18f, 383.87f, 357.08f, 365.77f, 358.08f, 358.27f)
            curveTo(360.75f, 356.93f, 366.89f, 354.97f, 370.08f, 357.77f)
            curveTo(373.29f, 360.57f, 381.08f, 373.27f, 384.58f, 379.27f)
            curveTo(385.42f, 380.1f, 386.29f, 382.27f, 383.08f, 384.27f)
            curveTo(379.89f, 386.27f, 377.75f, 388.77f, 377.08f, 389.77f)
            curveTo(373.48f, 396.17f, 367.92f, 396.77f, 365.58f, 396.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(369.58f, 401.27f)
            curveTo(367.18f, 397.67f, 366.58f, 400.1f, 366.58f, 401.77f)
            curveTo(367.75f, 404.93f, 370.48f, 412.07f, 372.08f, 415.27f)
            curveTo(373.68f, 418.47f, 374.75f, 415.93f, 375.08f, 414.27f)
            curveTo(374.25f, 411.43f, 371.98f, 404.87f, 369.58f, 401.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(378.58f, 398.77f)
            curveTo(375.79f, 393.17f, 374.75f, 398.1f, 374.58f, 401.27f)
            lineTo(383.08f, 421.27f)
            curveTo(387.08f, 424.87f, 387.08f, 421.1f, 386.58f, 418.77f)
            curveTo(385.08f, 414.43f, 381.39f, 404.37f, 378.58f, 398.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(384.58f, 395.27f)
            curveTo(380.98f, 389.27f, 380.08f, 394.43f, 380.08f, 397.77f)
            lineTo(391.58f, 423.27f)
            curveTo(395.18f, 427.67f, 395.08f, 421.43f, 394.58f, 417.77f)
            curveTo(392.75f, 412.77f, 388.18f, 401.27f, 384.58f, 395.27f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(390.58f, 387.77f)
            curveTo(388.18f, 382.57f, 386.25f, 386.93f, 385.58f, 389.77f)
            lineTo(396.08f, 413.77f)
            curveTo(399.68f, 417.77f, 399.92f, 412.1f, 399.58f, 408.77f)
            curveTo(397.58f, 403.93f, 392.98f, 392.97f, 390.58f, 387.77f)
            close()
        }
        path(fill = SolidColor(other)) {
            moveTo(403.58f, 380.27f)
            curveTo(395.98f, 379.87f, 383.08f, 369.43f, 377.58f, 364.27f)
            curveTo(376.92f, 362.93f, 375.98f, 360.17f, 377.58f, 359.77f)
            curveTo(379.18f, 359.37f, 381.25f, 360.6f, 382.08f, 361.27f)
            lineTo(395.08f, 369.27f)
            lineTo(407.08f, 376.27f)
            curveTo(409.08f, 377.77f, 411.18f, 380.67f, 403.58f, 380.27f)
            close()
        }
    }.build()
}