package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

private var _Kettlebells: ImageVector? = null

public val Kettlebells: ImageVector
    get() {
        if (_Kettlebells != null) {
            return _Kettlebells!!
        }
        _Kettlebells = ImageVector.Builder(
            name = "Kettlebells",
            defaultWidth = 180.dp,
            defaultHeight = 208.dp,
            viewportWidth = 180f,
            viewportHeight = 208f
        ).apply {
            path(
                fill = SolidColor(AppPalette.NeutralDark.N300),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(x = 53f, y = 88f)
                lineTo(x = 42f, y = 51f)
                curveTo(x1 = 36f, y1 = 28f, x2 = 50f, y2 = 12f, x3 = 74f, y3 = 10f)
                curveTo(x1 = 90f, y1 = 8f, x2 = 109f, y2 = 15f, x3 = 122f, y3 = 26f)
                curveTo(x1 = 133f, y1 = 35f, x2 = 137f, y2 = 46f, x3 = 132f, y3 = 61f)
                lineTo(x = 124f, y = 94f)
                lineTo(x = 107f, y = 88f)
                lineTo(x = 117f, y = 53f)
                curveTo(x1 = 120f, y1 = 43f, x2 = 112f, y2 = 35f, x3 = 101f, y3 = 30f)
                curveTo(x1 = 90f, y1 = 25f, x2 = 77f, y2 = 23f, x3 = 68f, y3 = 27f)
                curveTo(x1 = 59f, y1 = 30f, x2 = 56f, y2 = 37f, x3 = 59f, y3 = 48f)
                lineTo(x = 70f, y = 86f)
                close()
            }
            path(
                fill = SolidColor(AppPalette.NeutralDark.N550),
                pathFillType = PathFillType.EvenOdd
            ) {
                moveTo(x = 46f, y = 84f)
                lineTo(x = 35f, y = 47f)
                curveTo(x1 = 29f, y1 = 24f, x2 = 43f, y2 = 8f, x3 = 67f, y3 = 6f)
                curveTo(x1 = 83f, y1 = 4f, x2 = 102f, y2 = 11f, x3 = 115f, y3 = 22f)
                curveTo(x1 = 126f, y1 = 31f, x2 = 130f, y2 = 42f, x3 = 125f, y3 = 57f)
                lineTo(x = 117f, y = 90f)
                lineTo(x = 100f, y = 84f)
                lineTo(x = 110f, y = 49f)
                curveTo(x1 = 113f, y1 = 39f, x2 = 105f, y2 = 31f, x3 = 94f, y3 = 26f)
                curveTo(x1 = 83f, y1 = 21f, x2 = 70f, y2 = 19f, x3 = 61f, y3 = 23f)
                curveTo(x1 = 52f, y1 = 26f, x2 = 49f, y2 = 33f, x3 = 52f, y3 = 44f)
                lineTo(x = 63f, y = 82f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N700)) {
                moveTo(x = 36f, y = 31f)
                curveTo(x1 = 39f, y1 = 17f, x2 = 52f, y2 = 8f, x3 = 68f, y3 = 8f)
                curveTo(x1 = 84f, y1 = 7f, x2 = 101f, y2 = 13f, x3 = 113f, y3 = 23f)
                lineTo(x = 107f, y = 26f)
                curveTo(x1 = 95f, y1 = 18f, x2 = 81f, y2 = 13f, x3 = 68f, y3 = 14f)
                curveTo(x1 = 54f, y1 = 14f, x2 = 44f, y2 = 21f, x3 = 41f, y3 = 33f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(x = 61f, y = 23f)
                curveTo(x1 = 70f, y1 = 19f, x2 = 83f, y2 = 21f, x3 = 94f, y3 = 26f)
                curveTo(x1 = 105f, y1 = 31f, x2 = 113f, y2 = 39f, x3 = 110f, y3 = 49f)
                lineTo(x = 107f, y = 59f)
                curveTo(x1 = 109f, y1 = 45f, x2 = 103f, y2 = 37f, x3 = 91f, y3 = 32f)
                curveTo(x1 = 79f, y1 = 26f, x2 = 69f, y2 = 25f, x3 = 60f, y3 = 29f)
                curveTo(x1 = 55f, y1 = 31f, x2 = 52f, y2 = 35f, x3 = 52f, y3 = 40f)
                curveTo(x1 = 50f, y1 = 32f, x2 = 54f, y2 = 26f, x3 = 61f, y3 = 23f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N250)) {
                moveTo(x = 75f, y = 81f)
                curveTo(x1 = 103f, y1 = 76f, x2 = 137f, y2 = 86f, x3 = 154f, y3 = 110f)
                curveTo(x1 = 172f, y1 = 135f, x2 = 171f, y2 = 164f, x3 = 152f, y3 = 185f)
                lineTo(x = 130f, y = 200f)
                lineTo(x = 72f, y = 195f)
                curveTo(x1 = 46f, y1 = 189f, x2 = 26f, y2 = 169f, x3 = 23f, y3 = 143f)
                curveTo(x1 = 20f, y1 = 115f, x2 = 43f, y2 = 88f, x3 = 75f, y3 = 81f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(x = 65f, y = 77f)
                curveTo(x1 = 91f, y1 = 72f, x2 = 121f, y2 = 82f, x3 = 138f, y3 = 105f)
                curveTo(x1 = 155f, y1 = 127f, x2 = 156f, y2 = 154f, x3 = 139f, y3 = 176f)
                lineTo(x = 115f, y = 193f)
                lineTo(x = 58f, y = 188f)
                curveTo(x1 = 32f, y1 = 182f, x2 = 15f, y2 = 163f, x3 = 13f, y3 = 138f)
                curveTo(x1 = 10f, y1 = 109f, x2 = 32f, y2 = 84f, x3 = 65f, y3 = 77f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(x = 108f, y = 84f)
                curveTo(x1 = 121f, y1 = 89f, x2 = 132f, y2 = 97f, x3 = 138f, y3 = 105f)
                curveTo(x1 = 155f, y1 = 127f, x2 = 156f, y2 = 154f, x3 = 139f, y3 = 176f)
                lineTo(x = 115f, y = 193f)
                lineTo(x = 98f, y = 189f)
                curveTo(x1 = 124f, y1 = 174f, x2 = 137f, y2 = 154f, x3 = 136f, y3 = 132f)
                curveTo(x1 = 135f, y1 = 111f, x2 = 124f, y2 = 94f, x3 = 108f, y3 = 84f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N250)) {
                moveTo(x = 29f, y = 171f)
                curveTo(x1 = 48f, y1 = 183f, x2 = 76f, y2 = 189f, x3 = 98f, y3 = 189f)
                lineTo(x = 115f, y = 193f)
                lineTo(x = 139f, y = 176f)
                lineTo(x = 130f, y = 200f)
                lineTo(x = 72f, y = 195f)
                lineTo(x = 58f, y = 188f)
                curveTo(x1 = 45f, y1 = 185f, x2 = 36f, y2 = 179f, x3 = 29f, y3 = 171f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(x = 39f, y = 91f)
                curveTo(x1 = 48f, y1 = 84f, x2 = 56f, y2 = 79f, x3 = 65f, y3 = 77f)
                curveTo(x1 = 91f, y1 = 72f, x2 = 121f, y2 = 82f, x3 = 138f, y3 = 105f)
                curveTo(x1 = 115f, y1 = 91f, x2 = 89f, y2 = 85f, x3 = 67f, y3 = 89f)
                curveTo(x1 = 57f, y1 = 90f, x2 = 48f, y2 = 94f, x3 = 39f, y3 = 100f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(x = 39f, y = 91f)
                lineTo(x = 39f, y = 100f)
                curveTo(x1 = 24f, y1 = 115f, x2 = 20f, y2 = 134f, x3 = 26f, y3 = 153f)
                curveTo(x1 = 30f, y1 = 165f, x2 = 38f, y2 = 175f, x3 = 49f, y3 = 183f)
                curveTo(x1 = 28f, y1 = 175f, x2 = 15f, y2 = 159f, x3 = 13f, y3 = 138f)
                curveTo(x1 = 11f, y1 = 119f, x2 = 21f, y2 = 102f, x3 = 39f, y3 = 91f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N600)) {
                moveTo(x = 42f, y = 89f)
                curveTo(x1 = 50f, y1 = 83f, x2 = 58f, y2 = 79f, x3 = 66f, y3 = 78f)
                curveTo(x1 = 82f, y1 = 75f, x2 = 99f, y2 = 78f, x3 = 113f, y3 = 84f)
                lineTo(x = 107f, y = 86f)
                curveTo(x1 = 93f, y1 = 81f, x2 = 79f, y2 = 79f, x3 = 66f, y3 = 82f)
                curveTo(x1 = 57f, y1 = 83f, x2 = 49f, y2 = 87f, x3 = 42f, y3 = 92f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(x = 31f, y = 112f)
                curveTo(x1 = 35f, y1 = 103f, x2 = 43f, y2 = 98f, x3 = 51f, y3 = 96f)
                curveTo(x1 = 42f, y1 = 103f, x2 = 37f, y2 = 112f, x3 = 35f, y3 = 124f)
                curveTo(x1 = 34f, y1 = 131f, x2 = 34f, y2 = 138f, x3 = 36f, y3 = 145f)
                curveTo(x1 = 29f, y1 = 136f, x2 = 27f, y2 = 123f, x3 = 31f, y3 = 112f)
                close()
            }
        }.build()
        return _Kettlebells!!
    }
