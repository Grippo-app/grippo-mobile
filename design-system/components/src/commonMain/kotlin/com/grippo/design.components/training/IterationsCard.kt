package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.trainings.models.IterationState
import com.grippo.presentation.api.trainings.models.stubIteration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
public fun IterationsCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<IterationState>,
) {
    FlowRow(
        modifier = Modifier
            .clip(RoundedCornerShape(AppTokens.dp.iterationsCard.radius))
            .background(AppTokens.colors.background.secondary)
            .border(
                width = 1.dp,
                color = AppTokens.colors.border.defaultPrimary,
                shape = RoundedCornerShape(AppTokens.dp.iterationsCard.radius)
            )
            .fillMaxWidth()
            .padding(
                vertical = AppTokens.dp.iterationsCard.verticalPadding,
                horizontal = AppTokens.dp.iterationsCard.horizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        value.forEach { iteration ->
            IterationCard(
                value = iteration,
            )
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