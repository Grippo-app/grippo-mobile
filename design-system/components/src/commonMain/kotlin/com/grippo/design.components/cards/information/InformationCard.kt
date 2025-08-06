package com.grippo.design.components.cards.information

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.training.IterationCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.stubIteration

@Composable
public fun InformationCard(
    modifier: Modifier = Modifier,
    label: String,
    trailing: @Composable RowScope.() -> Unit = {},
    value: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.height(AppTokens.dp.informationCard.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        trailing.invoke(this)

        Text(
            text = label,
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Spacer(modifier = Modifier.weight(1f))

        value.invoke(this)
    }
}

@AppPreview
@Composable
private fun InformationCardPreview() {
    PreviewContainer {
        InformationCard(
            modifier = Modifier.fillMaxWidth(),
            label = "Volume",
            value = {
                Text(
                    text = "Value"
                )
            }
        )

        InformationCard(
            modifier = Modifier.fillMaxWidth(),
            label = "Volume",
            value = {
                IterationCard(
                    value = stubIteration()
                )
            }
        )
    }
}