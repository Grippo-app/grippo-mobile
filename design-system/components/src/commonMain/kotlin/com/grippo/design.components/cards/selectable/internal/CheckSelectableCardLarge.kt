package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Check
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ExperienceIntermediate

@Composable
internal fun CheckSelectableCardLarge(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Large,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.large.radius)

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

    Column(
        modifier = modifier
            .semantics { selected = isSelected }
            .scalableClick(onClick = onClick)
            .clip(shape)
            .background(backgroundColor, shape)
            .border(AppTokens.dp.checkSelectableCard.borderWidth, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.large.verticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.checkSelectableCard.large.horizontalPadding)
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(AppTokens.dp.checkSelectableCard.large.icon),
                tint = AppTokens.colors.icon.tertiary
            )

            Text(
                modifier = Modifier.weight(1f),
                text = style.title,
                style = AppTokens.typography.h5(),
                color = AppTokens.colors.text.primary
            )

            Check(checked = isSelected)
        }

        Text(
            text = style.description,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardLargePreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Intermediate",
                description = "You train regularly and want a balanced plan that adapts to your progress.",
                icon = AppTokens.icons.ExperienceIntermediate
            )
        )
    }
}
