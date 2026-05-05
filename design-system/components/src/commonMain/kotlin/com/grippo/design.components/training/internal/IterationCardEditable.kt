package com.grippo.design.components.training.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.isPending
import com.grippo.core.state.trainings.stubIteration
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun IterationCardEditable(
    modifier: Modifier = Modifier,
    label: String,
    value: IterationState,
    onVolumeClick: () -> Unit,
    onRepetitionClick: () -> Unit,
    volumeDecorator: @Composable BoxScope.() -> Unit,
    repetitionDecorator: @Composable BoxScope.() -> Unit,
) {
    val isPending = value.isPending()

    val shape = RoundedCornerShape(AppTokens.dp.iterationCard.editable.radius)

    val cellLabelColor = if (isPending) {
        AppTokens.colors.text.secondary
    } else {
        AppTokens.colors.text.primary
    }

    val cellTextColor = if (isPending) {
        AppTokens.colors.text.secondary
    } else {
        AppTokens.colors.text.primary
    }

    val cellBackground = if (isPending) {
        Color.Transparent
    } else {
        AppTokens.colors.background.card
    }

    val cellBorderModifier: Modifier = if (isPending) {
        Modifier.border(
            width = 1.dp,
            color = AppTokens.colors.border.default,
            shape = shape,
        )
    } else {
        Modifier
    }

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
            style = AppTokens.typography.b14Bold(),
            color = cellLabelColor
        )

        Box(
            modifier = Modifier
                .scalableClick(onClick = onVolumeClick)
                .weight(0.6f)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cellBackground, shape)
                    .then(cellBorderModifier)
                    .padding(horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding)
                    .height(AppTokens.dp.iterationCard.editable.height)
                    .wrapContentHeight(),
                text = value.volume().short(),
                textAlign = TextAlign.Center,
                style = AppTokens.typography.b14Bold(),
                color = cellTextColor
            )

            volumeDecorator.invoke(this)
        }

        Box(
            modifier = Modifier
                .scalableClick(onClick = onRepetitionClick)
                .weight(0.4f)
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cellBackground, shape)
                    .then(cellBorderModifier)
                    .padding(horizontal = AppTokens.dp.iterationCard.editable.horizontalPadding)
                    .height(AppTokens.dp.iterationCard.editable.height)
                    .wrapContentHeight(),
                textAlign = TextAlign.Center,
                text = value.repetitions.short(),
                style = AppTokens.typography.b14Bold(),
                color = cellTextColor
            )
            repetitionDecorator.invoke(this)
        }
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
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {}
        )
        IterationCardEditable(
            label = "2",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {}
        )
        IterationCardEditable(
            label = "3",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {}
        )
        IterationCardEditable(
            label = "4",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {}
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditablePendingPreview() {
    PreviewContainer {
        IterationCardEditable(
            label = "1",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {},
        )
        IterationCardEditable(
            label = "2",
            value = stubIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {},
        )
        IterationCardEditable(
            label = "3",
            value = stubPendingIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {},
        )
        IterationCardEditable(
            label = "4",
            value = stubPendingIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {},
        )
        IterationCardEditable(
            label = "5",
            value = stubPendingIteration(),
            onVolumeClick = {},
            onRepetitionClick = {},
            volumeDecorator = {},
            repetitionDecorator = {},
        )
    }
}
