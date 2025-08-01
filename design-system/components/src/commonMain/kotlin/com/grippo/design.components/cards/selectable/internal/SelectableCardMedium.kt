package com.grippo.design.components.cards.selectable.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.cards.selectable.SelectableCardVariants
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun SelectableCardMedium(
    modifier: Modifier,
    style: SelectableCardStyle.Medium,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.selectableCard.medium.radius)

    val borderColor by animateColorAsState(
        if (isSelected) AppTokens.colors.border.focus else AppTokens.colors.border.defaultPrimary,
        label = "border"
    )

    Column(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.primary, shape)
            .border(1.dp, borderColor, shape)
            .animateContentSize()
            .padding(
                horizontal = AppTokens.dp.selectableCard.medium.horizontalPadding,
                vertical = AppTokens.dp.selectableCard.medium.verticalPadding
            ),
    ) {
        Text(
            text = style.title,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

        Text(
            text = style.description,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary
        )

        style.subContent?.let { subContent ->

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            subContent.invoke(this)
        }
    }
}

@AppPreview
@Composable
private fun SelectableCardMediumPreview() {
    PreviewContainer {
        SelectableCardVariants(
            SelectableCardStyle.Medium(
                title = "Test Title",
                description = "Test description",
                subContent = null
            )
        )
        SelectableCardVariants(
            SelectableCardStyle.Medium(
                title = "Test Title",
                description = "Test description",
                subContent = {
                    Box(modifier = Modifier.size(40.dp).background(Color.Green))
                }
            )
        )
    }
}