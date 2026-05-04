package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val iconTint by animateColorAsState(
        if (isSelected) AppTokens.colors.semantic.success else AppTokens.colors.icon.tertiary,
        label = "iconTint"
    )

    val decorationWidth by animateDpAsState(
        targetValue = if (isSelected) AppTokens.dp.accent.smallWidth else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "decorationWidth"
    )

    Row(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .scalableClick(onClick = onClick)
            .clip(shape)
            .background(AppTokens.colors.background.card, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(decorationWidth)
                .background(AppTokens.colors.semantic.success.copy(alpha = 0.7f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTokens.dp.checkSelectableCard.large.horizontalPadding,
                    vertical = AppTokens.dp.checkSelectableCard.large.verticalPadding
                ),
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
