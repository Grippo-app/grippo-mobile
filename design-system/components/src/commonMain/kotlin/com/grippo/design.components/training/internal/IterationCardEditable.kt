package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.stubIteration

@Composable
internal fun IterationCardEditable(
    modifier: Modifier = Modifier,
    value: IterationState,
    onVolumeClick: () -> Unit,
    onRepetitionClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.iterationCard.editable.radius)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.iterationCard.editable.spacing)
    ) {
        Text(
            modifier = Modifier
                .scalableClick(onClick = onVolumeClick)
                .weight(0.6f)
                .background(AppTokens.colors.background.card, shape)
                .padding(
                    horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding,
                    vertical = AppTokens.dp.iterationCard.editable.verticalPadding
                ),
            text = value.volume.short(),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary
        )

        Text(
            modifier = Modifier
                .scalableClick(onClick = onRepetitionClick)
                .weight(0.4f)
                .background(AppTokens.colors.background.card, shape)
                .padding(
                    horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding,
                    vertical = AppTokens.dp.iterationCard.editable.verticalPadding
                ),
            textAlign = TextAlign.Center,
            text = value.repetitions.short(),
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditablePreview() {
    PreviewContainer {
        IterationCardEditable(
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
    }
}
