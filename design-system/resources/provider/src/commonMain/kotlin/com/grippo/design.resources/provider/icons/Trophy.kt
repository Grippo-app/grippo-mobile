package com.grippo.design.resources.provider.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Trophy: ImageVector
    get() {
        if (_Trophy != null) {
            return _Trophy!!
        }
        _Trophy = ImageVector.Builder(
            name = "Trophy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // Чаша, ножка, подставка
            path(
                fill = SolidColor(Color(0xFF0F172A))
            ) {
                // Чаша трофея
                moveTo(8f, 6f)
                horizontalLineTo(16f)
                curveTo(16f, 9f, 15f, 11.5f, 12f, 12.5f)
                curveTo(9f, 11.5f, 8f, 9f, 8f, 6f)
                close()

                // Ножка (стебель)
                moveTo(11f, 12.5f)
                lineTo(11f, 15f)
                lineTo(13f, 15f)
                lineTo(13f, 12.5f)
                close()

                // Верхняя платформа
                moveTo(9f, 15f)
                lineTo(15f, 15f)
                lineTo(15f, 16.5f)
                lineTo(9f, 16.5f)
                close()

                // Нижняя база
                moveTo(7f, 18f)
                lineTo(17f, 18f)
                lineTo(17f, 19.5f)
                lineTo(7f, 19.5f)
                close()
            }

            // Левая ручка (заполненная)
            path(
                fill = SolidColor(Color(0xFF0F172A))
            ) {
                // внешняя линия ручки
                moveTo(8f, 7f)
                curveTo(6.3f, 7f, 5f, 8.3f, 5f, 9.7f)
                curveTo(5f, 11.3f, 6.4f, 12.7f, 8f, 12.7f)
                lineTo(8.8f, 11.2f)
                curveTo(7.9f, 11.2f, 7.1f, 10.5f, 7.1f, 9.7f)
                curveTo(7.1f, 8.9f, 7.8f, 8.2f, 8.7f, 8.2f)
                lineTo(8.7f, 7.8f)
                curveTo(8.5f, 7.4f, 8.3f, 7f, 8f, 7f)
                close()
            }

            // Правая ручка (заполненная)
            path(
                fill = SolidColor(Color(0xFF0F172A))
            ) {
                moveTo(16f, 7f)
                curveTo(17.7f, 7f, 19f, 8.3f, 19f, 9.7f)
                curveTo(19f, 11.3f, 17.6f, 12.7f, 16f, 12.7f)
                lineTo(15.2f, 11.2f)
                curveTo(16.1f, 11.2f, 16.9f, 10.5f, 16.9f, 9.7f)
                curveTo(16.9f, 8.9f, 16.2f, 8.2f, 15.3f, 8.2f)
                lineTo(15.3f, 7.8f)
                curveTo(15.5f, 7.4f, 15.7f, 7f, 16f, 7f)
                close()
            }
        }.build()

        return _Trophy!!
    }

@Suppress("ObjectPropertyName")
private var _Trophy: ImageVector? = null

