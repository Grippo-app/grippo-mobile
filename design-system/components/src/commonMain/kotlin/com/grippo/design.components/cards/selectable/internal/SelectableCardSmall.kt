package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.Check

@Composable
internal fun SelectableCardSmall(
    modifier: Modifier,
    style: SelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.small.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "border"
    )

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .border(1.dp, borderColor, shape)
            .background(AppTokens.colors.background.primary, shape)
            .padding(horizontal = AppTokens.dp.selectableCard.small.horizontalPadding)
            .height(AppTokens.dp.selectableCard.small.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = isSelected) {
            Row {
                Icon(
                    imageVector = AppTokens.icons.Check,
                    modifier = Modifier.size(AppTokens.dp.selectableCard.small.icon),
                    tint = AppTokens.colors.icon.accent,
                    contentDescription = null,
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
            }
        }

        Text(
            modifier = Modifier.weight(1f),
            text = style.title,
            style = AppTokens.typography.b14Bold(),
            maxLines = 1,
            color = AppTokens.colors.text.primary,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun SelectableCardSmallPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Small(
                title = "Test Title"
            )
        )
    }
}