package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.selectors.Radio
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun SelectableCardSmall(
    modifier: Modifier,
    style: SelectableCardStyle.Small,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.small.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else Color.Transparent,
        label = "border"
    )

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .height(AppTokens.dp.selectableCard.small.height)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = AppTokens.dp.selectableCard.small.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Radio(
            selected = isSelected,
            onSelectedChange = onClick
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.content))

        Text(
            text = style.title,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))
    }
}

@AppPreview
@Composable
private fun SelectableCardSmallPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Small(
                title = "Test Title",
            )
        )
    }
}