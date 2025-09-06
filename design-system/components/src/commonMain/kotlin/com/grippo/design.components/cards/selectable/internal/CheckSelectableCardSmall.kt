package com.grippo.design.components.cards.selectable.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    Row(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.checkSelectableCard.small.horizontalPadding,
                vertical = AppTokens.dp.checkSelectableCard.small.verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (isSelected) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = AppTokens.icons.Check,
                contentDescription = null,
                tint = AppTokens.colors.icon.accent
            )
        }

        Spacer(Modifier.width(AppTokens.dp.contentPadding.subContent))

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
private fun CheckSelectableCardSmallPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Small(
                title = "Test Title",
            )
        )
    }
}