package com.grippo.design.components.cards.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.SelectableCardStyle
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.components.toggle.Toggle
import com.grippo.design.core.AppTokens

@Composable
internal fun SelectableCardSmall(
    modifier: Modifier,
    style: SelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.shape.medium)

    Row(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(shape)
            .nonRippleClick(onClick = onClick)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(horizontal = AppTokens.dp.paddings.smallHorizontal)
            .height(AppTokens.dp.size.componentHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.b14Bold(),
            maxLines = 2,
            color = AppTokens.colors.text.primary,
            overflow = TextOverflow.Ellipsis
        )

        Toggle(
            checked = isSelected,
            onCheckedChange = onClick
        )
    }
}

@Composable
internal fun SelectableCardSmallSkeleton(modifier: Modifier) {
    val radius = AppTokens.dp.shape.medium

    Box(
        modifier = modifier
            .shimmerAnimation(
                visible = true,
                radius = AppTokens.dp.shape.medium
            )
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = RoundedCornerShape(radius),
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(RoundedCornerShape(radius))
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, RoundedCornerShape(radius))
            .padding(horizontal = AppTokens.dp.paddings.smallHorizontal)
            .height(AppTokens.dp.size.componentHeight),
    )
}