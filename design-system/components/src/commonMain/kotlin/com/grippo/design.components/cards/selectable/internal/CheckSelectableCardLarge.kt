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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Spinner

@Composable
internal fun CheckSelectableCardLarge(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Large,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.large.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.semantic.success else Color.Transparent,
        label = "border"
    )

    val iconTint = AppTokens.colors.icon.primary

    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .border(1.dp, borderColor, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.large.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.large.verticalPadding
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.checkSelectableCard.large.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(AppTokens.dp.checkSelectableCard.large.icon),
                tint = iconTint
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
            ) {
                Text(
                    text = style.title,
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary
                )

                Text(
                    text = style.description,
                    style = AppTokens.typography.b14Med(),
                    color = AppTokens.colors.text.secondary
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardLargePreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description with big text for all cases and more options to do somethig!",
                icon = AppTokens.icons.Spinner
            )
        )

        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Test Title",
                description = "Test Description with big text for all cases and more options to do somethig!",
                icon = AppTokens.icons.Spinner,
            )
        )
    }
}