package com.grippo.design.components.training

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.components.training.internal.IterationCardEditable
import com.grippo.design.components.training.internal.IterationCardSmallView
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public sealed interface IterationCardStyle {
    @Immutable
    public data object SmallView : IterationCardStyle

    @Immutable
    public data class Editable(
        val label: String,
        val onVolumeClick: () -> Unit,
        val onRepetitionClick: () -> Unit,
        val volumeDecorator: @Composable BoxScope.() -> Unit,
        val repetitionDecorator: @Composable BoxScope.() -> Unit
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
            label = style.label,
            value = value,
            onVolumeClick = style.onVolumeClick,
            onRepetitionClick = style.onRepetitionClick,
            volumeDecorator = style.volumeDecorator,
            repetitionDecorator = style.repetitionDecorator
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
            style = IterationCardStyle.Editable(
                label = "1",
                onVolumeClick = {},
                onRepetitionClick = {},
                volumeDecorator = {},
                repetitionDecorator = {}
            )
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(
                label = "2",
                onVolumeClick = {},
                onRepetitionClick = {},
                volumeDecorator = {},
                repetitionDecorator = {}
            ),
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(
                label = "3",
                onVolumeClick = {},
                onRepetitionClick = {},
                volumeDecorator = {},
                repetitionDecorator = {}
            ),
        )
        IterationCard(
            value = stubIteration(),
            style = IterationCardStyle.Editable(
                label = "4",
                onVolumeClick = {},
                onRepetitionClick = {},
                volumeDecorator = {},
                repetitionDecorator = {}
            ),
        )
    }
}