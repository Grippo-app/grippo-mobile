package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.grippo.design.components.cards.selectable.CheckSelectableCardStyle
import com.grippo.design.components.cards.selectable.CheckSelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Check

@Composable
internal fun CheckSelectableCardSmall(
    modifier: Modifier,
    style: CheckSelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.checkSelectableCard.small.radius)

    val transition = updateTransition(targetState = isSelected, label = "check-card-transition")

    val iconSizeDp = AppTokens.dp.checkSelectableCard.small.icon
    val gapDp = AppTokens.dp.contentPadding.subContent

    // Animate reserved leading width: when selected -> icon + gap; when not -> only gap
    val leadingWidth = transition.animateDp(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "leading-width"
    ) { selected ->
        if (selected) iconSizeDp + gapDp else gapDp
    }

    // Icon appearance (kept in sync with width)
    val iconAlpha = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 180, easing = FastOutSlowInEasing) },
        label = "icon-alpha"
    ) { selected -> if (selected) 1f else 0f }

    val iconScale = transition.animateFloat(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "icon-scale"
    ) { selected -> if (selected) 1f else 0.85f }

    val textColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "text-color"
    ) { selected -> if (selected) AppTokens.colors.static.white else AppTokens.colors.text.primary }

    val iconColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "icon-color"
    ) { selected -> if (selected) AppTokens.colors.static.white else Color.Transparent }

    val backgroundColor = transition.animateColor(
        transitionSpec = { tween(durationMillis = 220, easing = FastOutSlowInEasing) },
        label = "text-color"
    ) { selected -> if (selected) AppTokens.colors.background.accent else AppTokens.colors.background.card }

    // Keep the icon in composition while animating to avoid popping on exit
    val renderIcon = remember(transition) {
        derivedStateOf { transition.currentState || transition.targetState }
    }

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(backgroundColor.value, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.small.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.small.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading area: Icon + gap (selected) OR only gap (unselected)
        Box(
            modifier = Modifier.width(leadingWidth.value),
            contentAlignment = Alignment.CenterStart
        ) {
            if (renderIcon.value) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier
                            .size(iconSizeDp)
                            .graphicsLayer {
                                alpha = iconAlpha.value
                                scaleX = iconScale.value
                                scaleY = iconScale.value
                            },
                        imageVector = AppTokens.icons.Check,
                        contentDescription = null,
                        tint = iconColor.value
                    )
                    Spacer(Modifier.width(gapDp))
                }
            } else {
                // When icon is fully gone, keep only the gap
                Spacer(Modifier.width(gapDp))
            }
        }

        Text(
            text = style.title,
            style = AppTokens.typography.b15Bold(),
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