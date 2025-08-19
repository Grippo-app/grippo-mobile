package com.grippo.design.components.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.design.components.training.internal.IterationCardEditable
import com.grippo.design.components.training.internal.IterationCardSmallView
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.stubIteration

@Immutable
public sealed interface IterationCardStyle {
    @Immutable
    public data object SmallView : IterationCardStyle

    @Immutable
    public data class Editable(
        val onVolumeClick: () -> Unit,
        val onRepetitionClick: () -> Unit,
    ) : IterationCardStyle
}

@Composable
public fun IterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
    style: IterationCardStyle = IterationCardStyle.SmallView
) {
    when (style) {
        is IterationCardStyle.SmallView -> IterationCardSmallView(
            modifier = modifier,
            value = value
        )

        is IterationCardStyle.Editable -> IterationCardEditable(
            modifier = modifier,
            value = value,
            onVolumeClick = style.onVolumeClick,
            onRepetitionClick = style.onRepetitionClick
        )
    }
}

@AppPreview
@Composable
private fun IterationCardSmallViewPreview() {
    PreviewContainer {
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.SmallView
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.SmallView
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.SmallView
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.SmallView
        )
    }
}

@AppPreview
@Composable
private fun IterationCardEditablePreview() {
    PreviewContainer {
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(onVolumeClick = {}, onRepetitionClick = {})
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(onVolumeClick = {}, onRepetitionClick = {}),
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(onVolumeClick = {}, onRepetitionClick = {}),
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(onVolumeClick = {}, onRepetitionClick = {}),
        )
    }
}