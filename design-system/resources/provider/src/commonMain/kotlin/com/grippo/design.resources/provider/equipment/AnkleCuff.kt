package com.grippo.design.resources.provider.equipment

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.colors.AppPalette

public val AnkleCuff: ImageVector
    get() {
        if (_AnkleCuff != null) {
            return _AnkleCuff!!
        }
        _AnkleCuff = ImageVector.Builder(
            name = "AnkleCuff",
            defaultWidth = 164.dp,
            defaultHeight = 130.dp,
            viewportWidth = 164f,
            viewportHeight = 130f,
        ).apply {
            path(fill = SolidColor(AppPalette.NeutralDark.N650), pathFillType = PathFillType.EvenOdd) {
                moveTo(x = 119f, y = 44f)
                lineTo(x = 140f, y = 44f)
                curveTo(x1 = 151f, y1 = 44f, x2 = 158f, y2 = 51f, x3 = 158f, y3 = 61f)
                curveTo(x1 = 158f, y1 = 72f, x2 = 150f, y2 = 81f, x3 = 140f, y3 = 85f)
                lineTo(x = 121f, y = 91f)
                close()
                moveTo(x = 127f, y = 52f)
                lineTo(x = 140f, y = 51f)
                curveTo(x1 = 147f, y1 = 51f, x2 = 151f, y2 = 55f, x3 = 151f, y3 = 62f)
                curveTo(x1 = 151f, y1 = 69f, x2 = 146f, y2 = 75f, x3 = 138f, y3 = 78f)
                lineTo(x = 127f, y = 82f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralLight.N200), pathFillType = PathFillType.EvenOdd) {
                moveTo(x = 118f, y = 42f)
                lineTo(x = 138f, y = 42f)
                curveTo(x1 = 149f, y1 = 42f, x2 = 156f, y2 = 49f, x3 = 156f, y3 = 59f)
                curveTo(x1 = 156f, y1 = 70f, x2 = 148f, y2 = 79f, x3 = 138f, y3 = 83f)
                lineTo(x = 119f, y = 89f)
                close()
                moveTo(x = 125f, y = 49f)
                lineTo(x = 138f, y = 49f)
                curveTo(x1 = 145f, y1 = 49f, x2 = 149f, y2 = 53f, x3 = 149f, y3 = 60f)
                curveTo(x1 = 149f, y1 = 67f, x2 = 144f, y2 = 73f, x3 = 136f, y3 = 76f)
                lineTo(x = 125f, y = 80f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N400)) {
                moveTo(x = 12f, y = 44f)
                curveTo(x1 = 14f, y1 = 60f, x2 = 39f, y2 = 72f, x3 = 67f, y3 = 72f)
                curveTo(x1 = 93f, y1 = 72f, x2 = 114f, y2 = 62f, x3 = 121f, y3 = 45f)
                lineTo(x = 117f, y = 91f)
                curveTo(x1 = 112f, y1 = 109f, x2 = 88f, y2 = 120f, x3 = 60f, y3 = 117f)
                curveTo(x1 = 34f, y1 = 115f, x2 = 13f, y2 = 103f, x3 = 10f, y3 = 87f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(x = 94f, y = 65f)
                curveTo(x1 = 108f, y1 = 60f, x2 = 116f, y2 = 53f, x3 = 121f, y3 = 45f)
                lineTo(x = 117f, y = 91f)
                curveTo(x1 = 113f, y1 = 105f, x2 = 99f, y2 = 114f, x3 = 82f, y3 = 117f)
                lineTo(x = 88f, y = 76f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(x = 13f, y = 83f)
                curveTo(x1 = 18f, y1 = 100f, x2 = 39f, y2 = 110f, x3 = 62f, y3 = 112f)
                curveTo(x1 = 83f, y1 = 114f, x2 = 102f, y2 = 107f, x3 = 113f, y3 = 97f)
                lineTo(x = 112f, y = 102f)
                curveTo(x1 = 100f, y1 = 114f, x2 = 81f, y2 = 119f, x3 = 60f, y3 = 117f)
                curveTo(x1 = 34f, y1 = 115f, x2 = 13f, y2 = 103f, x3 = 10f, y3 = 87f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N550), pathFillType = PathFillType.EvenOdd) {
                moveTo(x = 12f, y = 44f)
                curveTo(x1 = 12f, y1 = 26f, x2 = 38f, y2 = 14f, x3 = 69f, y3 = 14f)
                curveTo(x1 = 100f, y1 = 14f, x2 = 122f, y2 = 28f, x3 = 122f, y3 = 46f)
                curveTo(x1 = 122f, y1 = 64f, x2 = 98f, y2 = 77f, x3 = 67f, y3 = 77f)
                curveTo(x1 = 36f, y1 = 77f, x2 = 12f, y2 = 62f, x3 = 12f, y3 = 44f)
                close()
                moveTo(x = 28f, y = 43f)
                curveTo(x1 = 28f, y1 = 33f, x2 = 46f, y2 = 26f, x3 = 68f, y3 = 26f)
                curveTo(x1 = 90f, y1 = 26f, x2 = 107f, y2 = 34f, x3 = 107f, y3 = 44f)
                curveTo(x1 = 107f, y1 = 54f, x2 = 90f, y2 = 62f, x3 = 68f, y3 = 62f)
                curveTo(x1 = 46f, y1 = 62f, x2 = 28f, y2 = 53f, x3 = 28f, y3 = 43f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(x = 28f, y = 43f)
                curveTo(x1 = 28f, y1 = 33f, x2 = 46f, y2 = 26f, x3 = 68f, y3 = 26f)
                curveTo(x1 = 90f, y1 = 26f, x2 = 107f, y2 = 34f, x3 = 107f, y3 = 44f)
                lineTo(x = 105f, y = 48f)
                curveTo(x1 = 102f, y1 = 40f, x2 = 87f, y2 = 34f, x3 = 68f, y3 = 34f)
                curveTo(x1 = 49f, y1 = 34f, x2 = 34f, y2 = 39f, x3 = 30f, y3 = 47f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(x = 12f, y = 44f)
                curveTo(x1 = 12f, y1 = 62f, x2 = 36f, y2 = 77f, x3 = 67f, y3 = 77f)
                curveTo(x1 = 98f, y1 = 77f, x2 = 122f, y2 = 64f, x3 = 122f, y3 = 46f)
                lineTo(x = 107f, y = 44f)
                curveTo(x1 = 107f, y1 = 54f, x2 = 90f, y2 = 62f, x3 = 68f, y3 = 62f)
                curveTo(x1 = 46f, y1 = 62f, x2 = 28f, y2 = 53f, x3 = 28f, y3 = 43f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N650)) {
                moveTo(x = 15f, y = 44f)
                curveTo(x1 = 17f, y1 = 59f, x2 = 39f, y2 = 71f, x3 = 67f, y3 = 71f)
                curveTo(x1 = 91f, y1 = 71f, x2 = 110f, y2 = 63f, x3 = 118f, y3 = 52f)
                curveTo(x1 = 113f, y1 = 66f, x2 = 93f, y2 = 75f, x3 = 67f, y3 = 75f)
                curveTo(x1 = 38f, y1 = 75f, x2 = 16f, y2 = 62f, x3 = 15f, y3 = 44f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N200)) {
                moveTo(x = 107f, y = 54f)
                lineTo(x = 123f, y = 50f)
                lineTo(x = 129f, y = 81f)
                lineTo(x = 112f, y = 90f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N500)) {
                moveTo(x = 112f, y = 55f)
                lineTo(x = 121f, y = 53f)
                lineTo(x = 126f, y = 78f)
                lineTo(x = 116f, y = 83f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(x = 17f, y = 62f)
                curveTo(x1 = 35f, y1 = 76f, x2 = 65f, y2 = 81f, x3 = 91f, y3 = 74f)
                lineTo(x = 107f, y = 87f)
                curveTo(x1 = 111f, y1 = 91f, x2 = 109f, y2 = 99f, x3 = 103f, y3 = 102f)
                curveTo(x1 = 78f, y1 = 115f, x2 = 38f, y2 = 105f, x3 = 18f, y3 = 89f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N450)) {
                moveTo(x = 17f, y = 59f)
                curveTo(x1 = 35f, y1 = 73f, x2 = 64f, y2 = 78f, x3 = 91f, y3 = 71f)
                lineTo(x = 107f, y = 84f)
                curveTo(x1 = 111f, y1 = 88f, x2 = 108f, y2 = 96f, x3 = 102f, y3 = 99f)
                curveTo(x1 = 78f, y1 = 112f, x2 = 38f, y2 = 102f, x3 = 18f, y3 = 86f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N550)) {
                moveTo(x = 18f, y = 60f)
                curveTo(x1 = 36f, y1 = 74f, x2 = 65f, y2 = 79f, x3 = 91f, y3 = 72f)
                lineTo(x = 106f, y = 85f)
                lineTo(x = 105f, y = 88f)
                lineTo(x = 90f, y = 76f)
                curveTo(x1 = 64f, y1 = 82f, x2 = 36f, y2 = 77f, x3 = 18f, y3 = 64f)
                close()
            }
            path(fill = SolidColor(AppPalette.NeutralDark.N300)) {
                moveTo(x = 29f, y = 73f)
                curveTo(x1 = 46f, y1 = 82f, x2 = 67f, y2 = 85f, x3 = 85f, y3 = 82f)
                lineTo(x = 94f, y = 90f)
                curveTo(x1 = 96f, y1 = 92f, x2 = 94f, y2 = 95f, x3 = 90f, y3 = 96f)
                curveTo(x1 = 72f, y1 = 101f, x2 = 47f, y2 = 94f, x3 = 30f, y3 = 86f)
                close()
            }
        }.build()
        return _AnkleCuff!!
    }

@Suppress("ObjectPropertyName")
private var _AnkleCuff: ImageVector? = null
