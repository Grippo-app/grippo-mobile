package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun CheckSelectableCardSmall(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.small.radius)

    val transition = updateTransition(targetState = isSelected, label = "check-card-transition")

    val textColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "text-color"
    ) { selected -> if (selected) AppTokens.colors.static.white else AppTokens.colors.text.primary }

    val backgroundColor1 = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "background-color-1"
    ) { selected -> if (selected) AppTokens.colors.selectableCardColors.small.selectedBackground1 else AppTokens.colors.background.card }

    val backgroundColor2 = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "background-color-2"
    ) { selected -> if (selected) AppTokens.colors.selectableCardColors.small.selectedBackground2 else AppTokens.colors.background.card }

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    0f to backgroundColor1.value,
                    1f to backgroundColor2.value,
                ), shape
            )
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.small.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.small.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

        Text(
            text = style.title,
            style = AppTokens.typography.h6(),
            color = textColor.value
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardSmallPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Small(
                title = "Test Title",
            )
        )
    }
}
