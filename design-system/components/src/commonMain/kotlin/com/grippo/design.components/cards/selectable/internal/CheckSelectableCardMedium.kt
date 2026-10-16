package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Check
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun CheckSelectableCardMedium(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.medium.radius)

    val colors = AppTokens.colors.selection
    val backgroundColor by animateColorAsState(
        if (isSelected) {
            colors.selectedCardBackground
        } else {
            AppTokens.colors.background.card
        },
        label = "backgroundColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) {
            colors.selectedCardBorder
        } else {
            AppTokens.colors.border.default
        },
        label = "borderColor"
    )

    Row(
        modifier = modifier
            .semantics { selected = isSelected }
            .scalableClick(onClick = onClick)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppTokens.dp.checkSelectableCard.borderWidth, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.medium.verticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.checkSelectableCard.medium.horizontalPadding)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
        ) {
            Text(
                text = style.title,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary
            )

            style.description?.let { description ->
                Text(
                    text = description,
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary
                )
            }
        }

        Check(checked = isSelected)
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardMediumPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Build muscle",
                description = "Develop strength with consistent training and progressive overload.",
            )
        )

        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Every week",
                description = null
            )
        )
    }
}
