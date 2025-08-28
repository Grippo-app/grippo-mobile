package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.stubIteration

@Composable
internal fun IterationCardEditable(
    modifier: Modifier = Modifier,
    label: String,
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
                .widthIn(min = 60.dp)
                .padding(horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding)
                .height(AppTokens.dp.iterationCard.editable.height)
                .wrapContentHeight(),
            text = label,
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary
        )

        Text(
            modifier = Modifier
                .scalableClick(onClick = onVolumeClick)
                .weight(0.6f)
                .background(AppTokens.colors.background.card, shape)
                .padding(horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding)
                .height(AppTokens.dp.iterationCard.editable.height)
                .wrapContentHeight(),
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
                .padding(horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding)
                .height(AppTokens.dp.iterationCard.editable.height)
                .wrapContentHeight(),
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
            label = "1",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            label = "2",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            label = "3",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
        IterationCardEditable(
            label = "4",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {}
        )
    }
}
