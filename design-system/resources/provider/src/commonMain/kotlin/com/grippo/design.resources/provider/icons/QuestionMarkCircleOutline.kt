package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.QuestionMarkCircleOutline: ImageVector
    get() {
        if (_QuestionMarkCircleOutline != null) {
            return _QuestionMarkCircleOutline!!
        }
        _QuestionMarkCircleOutline = ImageVector.Builder(
            name = "QuestionMarkCircleOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9.09f, 9f)
                arcTo(3f, 3f, 0f, true, true, 12f, 12f)
                verticalLineTo(15f)
            }
            path(
                stroke = SolidColor(Color(0xFF0F172A)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 17.01f)
                lineTo(12.01f, 16.999f)
            }
        }.build()

        return _QuestionMarkCircleOutline!!
    }

@Suppress("ObjectPropertyName")
private var _QuestionMarkCircleOutline: ImageVector? = null
