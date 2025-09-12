package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Check

@Composable
internal fun CheckSelectableCardMedium(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.medium.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else Color.Transparent,
        label = "border"
    )

    val iconTint by animateColorAsState(
        if (isSelected) AppTokens.colors.icon.accent else AppTokens.colors.icon.primary,
        label = "iconTint"
    )

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.medium.verticalPadding
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.checkSelectableCard.medium.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppTokens.icons.Check,
            contentDescription = null,
            modifier = Modifier.size(AppTokens.dp.checkSelectableCard.medium.icon),
            tint = iconTint
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Text(
                text = style.title,
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary
            )

            style.description?.let { d ->
                Text(
                    text = d,
                    style = AppTokens.typography.b13Semi(),
                    color = AppTokens.colors.text.secondary
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardMediumPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Test Title",
                description = "Test Description with big text for all cases and more options to do somethig!",
            )
        )

        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Test Title",
                description = null
            )
        )
    }
}