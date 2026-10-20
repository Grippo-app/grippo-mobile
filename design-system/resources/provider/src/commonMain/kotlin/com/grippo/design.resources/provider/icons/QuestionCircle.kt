package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.QuestionCircle: ImageVector
    get() {
        if (_QuestionCircle != null) {
            return _QuestionCircle!!
        }
        _QuestionCircle = ImageVector.Builder(
            name = "QuestionCircle",
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
                moveTo(12f, 3f)
                curveTo(16.971f, 3f, 21f, 7.029f, 21f, 12f)
                curveTo(21f, 16.971f, 16.971f, 21f, 12f, 21f)
                curveTo(7.029f, 21f, 3f, 16.971f, 3f, 12f)
                curveTo(3f, 7.029f, 7.029f, 3f, 12f, 3f)
                close()
                moveTo(9.25f, 9f)
                curveTo(9.25f, 7.481f, 10.481f, 6.75f, 12f, 6.75f)
                curveTo(13.519f, 6.75f, 14.75f, 7.731f, 14.75f, 9.25f)
                curveTo(14.75f, 10.5f, 13.7f, 11.1f, 12.8f, 11.7f)
                curveTo(12.2f, 12.1f, 12f, 12.6f, 12f, 13.5f)
            }
            path(fill = SolidColor(Color(0xFF33363F))) {
                moveTo(12f, 15.75f)
                curveTo(12.552f, 15.75f, 13f, 16.198f, 13f, 16.75f)
                curveTo(13f, 17.302f, 12.552f, 17.75f, 12f, 17.75f)
                curveTo(11.448f, 17.75f, 11f, 17.302f, 11f, 16.75f)
                curveTo(11f, 16.198f, 11.448f, 15.75f, 12f, 15.75f)
                close()
            }
        }.build()

        return _QuestionCircle!!
    }

@Suppress("ObjectPropertyName")
private var _QuestionCircle: ImageVector? = null
