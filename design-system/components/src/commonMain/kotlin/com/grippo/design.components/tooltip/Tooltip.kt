package com.grippo.design.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.formatters.UiText

@Composable
public fun Tooltip(
    modifier: Modifier = Modifier,
    title: UiText,
    description: UiText,
) {
    Column(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.inverted,
                shape = RoundedCornerShape(AppTokens.dp.tooltip.radius)
            ).padding(
                horizontal = AppTokens.dp.tooltip.horizontalPadding,
                vertical = AppTokens.dp.tooltip.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Text(
            text = title.text(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.inverted,
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = AppTokens.colors.divider.default,
        )

        Text(
            text = description.text(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.inverted,
        )
    }
}

@AppPreview
@Composable
private fun TooltipPreview() {
    PreviewContainer {
        Tooltip(
            title = UiText.Str("Hello world"),
            description = UiText.Str("Hello my dear friend"),
        )
    }
}