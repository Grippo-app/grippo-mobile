package com.grippo.design.components.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens

@Composable
public fun SelectableCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector?,
    isSelected: Boolean,
    select: () -> Unit
) {

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            AppTokens.colors.border.focus
        } else {
            AppTokens.colors.border.defaultPrimary
        },
        label = "borderColor"
    )

    val iconTint by animateColorAsState(
        targetValue = if (isSelected) {
            AppTokens.colors.icon.accent
        } else {
            AppTokens.colors.icon.default
        },
        label = "iconTint"
    )

    val shadowColor = animateColorAsState(
        targetValue = if (isSelected) {
            AppTokens.colors.overlay.accentShadow
        } else {
            AppTokens.colors.overlay.defaultShadow
        },
        label = "shadowColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val shape = RoundedCornerShape(AppTokens.dp.shape.component)

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }.shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = shadowColor.value
            )
            .clip(shape)
            .nonRippleClick(onClick = select)
            .background(color = AppTokens.colors.background.secondary)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(
                vertical = AppTokens.dp.paddings.componentVertical,
                horizontal = AppTokens.dp.paddings.componentHorizontal
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.icon.card),
                imageVector = icon,
                tint = iconTint,
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = description,
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}