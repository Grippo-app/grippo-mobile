package com.grippo.design.components.modifiers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.grippo.design.components.modifiers.models.Side
import com.grippo.design.core.AppTokens
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public enum class ShadowElevation(
    public val dp: Dp,
    public val zIndex: Float
) {
    Component(2.dp, zIndex = 1f),
    Card(4.dp, zIndex = 2f),
    Container(6.dp, zIndex = 3f),
    Non(0.dp, zIndex = 0f),
}

@Composable
public fun Modifier.shadowDefault(
    shape: Shape,
    elevation: ShadowElevation,
    sides: ImmutableList<Side> = Side.entries.toPersistentList(),
    color: Color = AppTokens.colors.overlay.defaultShadow,
): Modifier {
    if (elevation == ShadowElevation.Non) return this

    val spotColor = color.copy(alpha = (color.alpha * 1.5F).coerceIn(0f, 1f))

    val hasTop = Side.TOP in sides
    val hasBottom = Side.BOTTOM in sides
    val hasLeft = Side.LEFT in sides
    val hasRight = Side.RIGHT in sides

    return this
        .zIndex(elevation.zIndex)
        .clipSides(
            left = hasLeft,
            top = hasTop,
            right = hasRight,
            bottom = hasBottom
        )
        .then(
            Modifier.shadow(
                elevation = elevation.dp,
                shape = shape,
                ambientColor = color,
                spotColor = spotColor,
                clip = false
            )
        )
}

private fun Modifier.clipSides(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false
): Modifier = drawWithContent {
    clipRect(
        left = if (left) -Float.MAX_VALUE else 0f,
        top = if (top) -Float.MAX_VALUE else 0f,
        right = if (right) Float.MAX_VALUE else size.width,
        bottom = if (bottom) Float.MAX_VALUE else size.height,
        block = { this@drawWithContent.drawContent() }
    )
}