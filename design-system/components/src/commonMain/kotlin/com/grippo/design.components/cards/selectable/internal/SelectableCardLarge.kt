package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun SelectableCardLarge(
    modifier: Modifier,
    style: SelectableCardStyle.Large,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.shape.large)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "mediumScale"
    )

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "mediumBorder"
    )

    val shadowColor by animateColorAsState(
        if (isSelected) AppTokens.colors.overlay.accentShadow else AppTokens.colors.overlay.defaultShadow,
        label = "mediumShadow"
    )

    val iconTint by animateColorAsState(
        if (isSelected) AppTokens.colors.icon.accent else AppTokens.colors.icon.default,
        label = "mediumIconTint"
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = shadowColor
            )
            .clip(shape)
            .nonRippleClick(onClick = onClick)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.paddings.mediumHorizontal,
                vertical = AppTokens.dp.paddings.mediumVertical
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            modifier = Modifier.size(AppTokens.dp.icon.m),
            tint = iconTint
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = style.title,
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary
            )
            Text(
                text = style.description,
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}

@Composable
internal fun SelectableCardLargeSkeleton(modifier: Modifier) {
    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

    Box(
        modifier = modifier
            .shimmerAnimation(
                visible = true,
                radius = AppTokens.dp.shape.large
            )
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow,
            )
            .clip(shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.paddings.mediumHorizontal,
                vertical = AppTokens.dp.paddings.mediumVertical
            ).height(AppTokens.dp.size.componentHeight)
    )
}

@AppPreview
@Composable
private fun SelectableCardLargePreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description with big text for all cases and more options to do somethig!",
                icon = Icons.Filled.Done
            )
        )

        SelectableCardLargeSkeleton(
            modifier = Modifier.fillMaxWidth()
        )
    }
}