package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.stubIteration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun IterationsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<IterationState>,
) {
    FlowRow(
        modifier = modifier
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.iterationsCard.radius)
            )
            .padding(AppTokens.dp.contentPadding.content),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        value.forEach { iteration ->
            key(iteration.id) {
                IterationCard(
                    value = iteration,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun IterationsCardPreview() {
    PreviewContainer {
        IterationsCard(
            value = listOf(
                stubIteration(),
                stubIteration(),
                stubIteration()
            ).toPersistentList()
        )
    }
}