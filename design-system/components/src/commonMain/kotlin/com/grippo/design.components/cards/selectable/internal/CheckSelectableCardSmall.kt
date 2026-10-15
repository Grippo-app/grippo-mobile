package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    val colors = AppTokens.colors.selection
    val backgroundColor by animateColorAsState(
        if (isSelected) colors.background else AppTokens.colors.background.card,
        label = "backgroundColor"
    )
    val borderColor by animateColorAsState(
        if (isSelected) colors.accent else AppTokens.colors.border.default,
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
                horizontal = AppTokens.dp.checkSelectableCard.small.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.small.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = style.title,
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardSmallPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Small(
                title = "Upper body",
            )
        )
    }
}
