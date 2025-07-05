package com.grippo.design.components.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.kg
import com.grippo.design.resources.reps
import com.grippo.presentation.api.trainings.models.IterationState
import com.grippo.presentation.api.trainings.models.stubIteration

@Composable
public fun IterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier.width(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.weight.toString(),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.secondary
            )

            Text(
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.secondary
            )
        }

        Row(
            modifier = Modifier.width(50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.repetitions.toString(),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.secondary
            )

            Text(
                text = AppTokens.strings.res(Res.string.reps),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}

@AppPreview
@Composable
private fun IterationCardPreview() {
    PreviewContainer {
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
    }
}