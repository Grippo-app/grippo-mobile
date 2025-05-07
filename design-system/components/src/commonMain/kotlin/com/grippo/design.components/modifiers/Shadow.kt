package com.grippo.design.components.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens

@Immutable
public enum class ShadowElevation(public val dp: Dp) {
    Component(2.dp),
    Card(4.dp),
    Container(6.dp),
}

@Composable
public fun Modifier.shadowDefault(
    shape: Shape,
    elevation: ShadowElevation,
    color: Color = AppTokens.colors.overlay.defaultShadow,
): Modifier {
    val spotColor = color.copy(alpha = (color.alpha * 1.5F).coerceIn(0f, 1f))
    return this.then(
        Modifier.shadow(
            elevation = elevation.dp,
            shape = shape,
            ambientColor = color,
            spotColor = spotColor,
            clip = false
        )
    )
}