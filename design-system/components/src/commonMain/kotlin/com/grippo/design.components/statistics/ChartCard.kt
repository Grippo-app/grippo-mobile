package com.grippo.design.components.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun ChartCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.chartCard.radius)

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.chartCard.horizontalPadding,
                vertical = AppTokens.dp.chartCard.verticalPadding
            ),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        content.invoke(this)
    }
}

@AppPreview
@Composable
private fun ChartCardPreview() {
    PreviewContainer {
        ChartCard(
            title = "My title",
            content = {
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.LightGray))
            }
        )
    }
}