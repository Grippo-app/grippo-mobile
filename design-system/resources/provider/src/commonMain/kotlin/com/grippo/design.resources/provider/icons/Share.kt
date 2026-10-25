package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Share: ImageVector
    get() {
        if (_Share != null) {
            return _Share!!
        }
        _Share = ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF33363F)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 5f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 5f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 5f)
                close()
                moveTo(9f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3f, y1 = 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 9f, y1 = 12f)
                close()
                moveTo(21f, 19f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 15f, y1 = 19f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 21f, y1 = 19f)
                close()
                moveTo(8.59f, 13.51f)
                lineTo(15.42f, 17.49f)
                moveTo(15.41f, 6.51f)
                lineTo(8.59f, 10.49f)
            }
        }.build()

        return _Share!!
    }

@Suppress("ObjectPropertyName")
private var _Share: ImageVector? = null
