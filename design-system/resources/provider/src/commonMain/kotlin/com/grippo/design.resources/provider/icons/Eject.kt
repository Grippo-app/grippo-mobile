package com.grippo.design.resources.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.grippo.design.resources.provider.AppIcon

public val AppIcon.Eject: ImageVector
    get() {
        if (_Eject != null) {
            return _Eject!!
        }
        _Eject = ImageVector.Builder(
            name = "Eject",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(5f, 14f)
                lineTo(4.408f, 13.54f)
                curveTo(4.232f, 13.766f, 4.2f, 14.072f, 4.326f, 14.33f)
                curveTo(4.452f, 14.587f, 4.714f, 14.75f, 5f, 14.75f)
                verticalLineTo(14f)
                close()
                moveTo(19f, 14f)
                verticalLineTo(14.75f)
                curveTo(19.287f, 14.75f, 19.548f, 14.587f, 19.674f, 14.33f)
                curveTo(19.8f, 14.072f, 19.768f, 13.766f, 19.592f, 13.54f)
                lineTo(19f, 14f)
                close()
                moveTo(5f, 14.75f)
                horizontalLineTo(19f)
                verticalLineTo(13.25f)
                horizontalLineTo(5f)
                verticalLineTo(14.75f)
                close()
                moveTo(10.619f, 5.555f)
                lineTo(4.408f, 13.54f)
                lineTo(5.592f, 14.461f)
                lineTo(11.803f, 6.475f)
                lineTo(10.619f, 5.555f)
                close()
                moveTo(19.592f, 13.54f)
                lineTo(13.381f, 5.555f)
                lineTo(12.197f, 6.475f)
                lineTo(18.408f, 14.461f)
                lineTo(19.592f, 13.54f)
                close()
                moveTo(11.803f, 6.475f)
                curveTo(11.903f, 6.347f, 12.097f, 6.347f, 12.197f, 6.475f)
                lineTo(13.381f, 5.555f)
                curveTo(12.681f, 4.654f, 11.319f, 4.654f, 10.619f, 5.555f)
                lineTo(11.803f, 6.475f)
                close()
            }
            path(fill = SolidColor(Color(0xFF0F172A))) {
                moveTo(5f, 17.25f)
                curveTo(4.586f, 17.25f, 4.25f, 17.586f, 4.25f, 18f)
                curveTo(4.25f, 18.414f, 4.586f, 18.75f, 5f, 18.75f)
                verticalLineTo(17.25f)
                close()
                moveTo(19f, 18.75f)
                curveTo(19.414f, 18.75f, 19.75f, 18.414f, 19.75f, 18f)
                curveTo(19.75f, 17.586f, 19.414f, 17.25f, 19f, 17.25f)
                verticalLineTo(18.75f)
                close()
                moveTo(5f, 18.75f)
                horizontalLineTo(19f)
                verticalLineTo(17.25f)
                horizontalLineTo(5f)
                verticalLineTo(18.75f)
                close()
            }
        }.build()

        return _Eject!!
    }

@Suppress("ObjectPropertyName")
private var _Eject: ImageVector? = null
