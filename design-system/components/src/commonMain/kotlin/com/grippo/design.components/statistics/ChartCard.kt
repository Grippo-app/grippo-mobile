package com.grippo.design.components.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ChartCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {

    val shape = RoundedCornerShape(AppTokens.dp.chartCard.radius)

    val gradient = Brush.verticalGradient(
        colors = listOf(
            AppTokens.colors.background.dialog,
            AppTokens.colors.background.card,
        )
    )

    Column(
        modifier = modifier
            .background(gradient, shape)
            .border(width = 2.dp, color = AppTokens.colors.background.card, shape = shape)
            .padding(
                horizontal = AppTokens.dp.chartCard.horizontalPadding,
                vertical = AppTokens.dp.chartCard.verticalPadding
            ),
    ) {
        content.invoke(this)
    }
}

@AppPreview
@Composable
private fun ChartCardPreview() {
    PreviewContainer {
        ChartCard {}
    }
}