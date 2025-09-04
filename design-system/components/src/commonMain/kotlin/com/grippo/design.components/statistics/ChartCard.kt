package com.grippo.design.components.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.components.tooltip.Tooltip
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.QuestionMarkCircleOutline
import com.grippo.state.formatters.UiText
import kotlinx.coroutines.launch

@Composable
public fun ChartCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.chartCard.radius)

    val positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider()
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .background(AppTokens.colors.background.card, shape)
            .padding(
                horizontal = AppTokens.dp.chartCard.horizontalPadding,
                vertical = AppTokens.dp.chartCard.verticalPadding
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary,
            )

            TooltipBox(
                positionProvider = positionProvider,
                state = tooltipState,
                tooltip = {
                    Tooltip(
                        modifier = Modifier.padding(AppTokens.dp.contentPadding.content),
                        title = UiText.Str("Hello world"),
                        description = UiText.Str("Hello my dear friend"),
                    )
                },
                content = {
                    Icon(
                        modifier = Modifier
                            .scalableClick(onClick = { scope.launch { tooltipState.show() } })
                            .size(AppTokens.dp.chartCard.icon),
                        imageVector = AppTokens.icons.QuestionMarkCircleOutline,
                        tint = AppTokens.colors.icon.secondary,
                        contentDescription = null
                    )
                }
            )
        }

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