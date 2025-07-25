package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.scalableClick
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
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.large.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "border"
    )

    val shadowColor by animateColorAsState(
        if (isSelected) AppTokens.colors.overlay.accentShadow else AppTokens.colors.overlay.defaultShadow,
        label = "shadow"
    )

    val iconTint by animateColorAsState(
        if (isSelected) AppTokens.colors.icon.accent else AppTokens.colors.icon.primary,
        label = "iconTint"
    )

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = shadowColor
            )
            .background(AppTokens.colors.background.secondary, shape)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.selectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.large.verticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.selectableCard.large.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            modifier = Modifier.size(AppTokens.dp.selectableCard.large.icon),
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
    val shape = AppTokens.dp.selectableCard.large.radius

    Box(
        modifier = modifier
            .shimmerAnimation(visible = true, radius = shape)
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = RoundedCornerShape(shape),
                color = AppTokens.colors.overlay.defaultShadow,
            )
            .background(AppTokens.colors.background.secondary, RoundedCornerShape(shape))
            .border(1.dp, AppTokens.colors.border.defaultPrimary, RoundedCornerShape(shape))
            .padding(
                horizontal = AppTokens.dp.selectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.large.verticalPadding
            ).height(
                AppTokens.dp.selectableCard.large.icon
            ),
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