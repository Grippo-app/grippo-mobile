package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.training.internal.EditableIterationCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.set_value

@Stable
public sealed class IterationCardStyle {
    @Immutable
    public data object Compact : IterationCardStyle()

    @Stable
    public data class Recorded(
        public val number: Int,
        public val trailing: (@Composable () -> Unit)? = null,
    ) : IterationCardStyle()

    @Stable
    public data class Editable(
        public val number: Int,
        public val unusualWeight: Boolean,
        public val unusualRepetitions: Boolean,
        public val onVolumeClick: () -> Unit,
        public val onRepetitionClick: () -> Unit,
        public val context: (@Composable () -> Unit)? = null,
    ) : IterationCardStyle()
}

@Composable
public fun IterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
    style: IterationCardStyle,
) {
    when (style) {
        IterationCardStyle.Compact -> CompactIterationCard(
            modifier = modifier,
            value = value,
        )

        is IterationCardStyle.Recorded -> RecordedIterationCard(
            modifier = modifier,
            value = value,
            style = style,
        )

        is IterationCardStyle.Editable -> EditableIterationCard(
            modifier = modifier,
            value = value,
            style = style,
        )
    }
}

@Composable
private fun CompactIterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value.volume().shortAnnotated(),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.secondary,
            )

            Spacer(modifier = Modifier.width(AppTokens.dp.contentPadding.text))

            Text(
                text = value.repetitions.shortAnnotated(),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.secondary,
            )
        }
        value.volumeCalculation()?.let { calculation ->
            Text(
                text = calculation,
                style = AppTokens.typography.b12Semi(),
                color = AppTokens.colors.text.secondary,
            )
        }
    }
}

@Composable
private fun RecordedIterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
    style: IterationCardStyle.Recorded,
) {
    val result = value.recordedResult()
    val calculation = value.volumeCalculation()
    val description = listOf(
        AppTokens.strings.res(Res.string.set_value, style.number),
        result,
        calculation,
    ).filterNotNull().joinToString(separator = ", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(vertical = AppTokens.dp.contentPadding.subContent),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(AppTokens.dp.iterationCard.numberSize)
                    .background(
                        color = AppTokens.colors.background.card,
                        shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = style.number.toString(),
                    style = AppTokens.typography.b12Semi(),
                    color = AppTokens.colors.text.secondary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                Text(
                    text = result,
                    style = AppTokens.typography.h5(),
                    color = AppTokens.colors.text.primary,
                )
                if (calculation != null) {
                    Text(
                        text = calculation,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                }
            }
        }
        style.trailing?.invoke()
    }
}

@AppPreview
@Composable
private fun IterationCardCompactPreview() {
    val pending = stubPendingIteration().copy(
        id = "iteration-card-compact-preview",
        repetitions = RepetitionsFormatState.of(8),
    )

    PreviewContainer {
        IterationCard(
            value = pending.copy(externalWeight = VolumeFormatState.of(80f)),
            style = IterationCardStyle.Compact,
        )
        IterationCard(
            value = pending.copy(
                bodyWeight = WeightFormatState.of(80f),
                bodyMultiplier = MultiplierFormatState.of(1f),
                assistWeight = VolumeFormatState.of(30f),
            ),
            style = IterationCardStyle.Compact,
        )
        IterationCard(value = pending, style = IterationCardStyle.Compact)
    }
}

@AppPreview
@Composable
private fun IterationCardRecordedPreview() {
    val pending = stubPendingIteration().copy(
        id = "iteration-card-recorded-preview",
        repetitions = RepetitionsFormatState.of(8),
    )
    val body = pending.copy(
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
    )

    PreviewContainer {
        IterationCard(
            value = pending.copy(externalWeight = VolumeFormatState.of(80f)),
            style = IterationCardStyle.Recorded(number = 1),
        )
        IterationCard(value = body, style = IterationCardStyle.Recorded(number = 2))
        IterationCard(
            value = body.copy(assistWeight = VolumeFormatState.of(30f)),
            style = IterationCardStyle.Recorded(number = 3),
        )
        IterationCard(
            value = body.copy(extraWeight = VolumeFormatState.of(20f)),
            style = IterationCardStyle.Recorded(number = 4),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardRecordedPartialPreview() {
    val pending = stubPendingIteration().copy(
        id = "iteration-card-recorded-partial-preview",
        repetitions = RepetitionsFormatState.of(8),
    )

    PreviewContainer {
        IterationCard(value = pending, style = IterationCardStyle.Recorded(number = 1))
        IterationCard(
            value = pending.copy(bodyWeight = WeightFormatState.of(80f)),
            style = IterationCardStyle.Recorded(number = 2),
        )
        IterationCard(
            value = pending.copy(
                externalWeight = VolumeFormatState.of(80f),
                repetitions = RepetitionsFormatState.Empty(),
            ),
            style = IterationCardStyle.Recorded(number = 3),
        )
        IterationCard(
            value = pending.copy(extraWeight = VolumeFormatState.of(20f)),
            style = IterationCardStyle.Recorded(number = 4),
        )
        IterationCard(
            value = pending.copy(assistWeight = VolumeFormatState.of(20f)),
            style = IterationCardStyle.Recorded(number = 5),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardRecordedZeroPreview() {
    val body = stubPendingIteration().copy(
        id = "iteration-card-recorded-zero-preview",
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        assistWeight = VolumeFormatState.of(80f),
        repetitions = RepetitionsFormatState.of(8),
    )

    PreviewContainer {
        IterationCard(value = body, style = IterationCardStyle.Recorded(number = 1))
        IterationCard(
            value = body.copy(
                bodyWeight = WeightFormatState.of(70.3f),
                bodyMultiplier = MultiplierFormatState.of(0.43f),
                assistWeight = VolumeFormatState.of(30.2f),
            ),
            style = IterationCardStyle.Recorded(number = 2),
        )
    }
}

@AppPreview
@Composable
private fun IterationCardRecordedTrailingPreview() {
    val value = stubPendingIteration().copy(
        id = "iteration-card-recorded-trailing-preview",
        externalWeight = VolumeFormatState.of(80f),
        repetitions = RepetitionsFormatState.of(8),
    )

    PreviewContainer {
        IterationCard(
            value = value,
            style = IterationCardStyle.Recorded(
                number = 1,
                trailing = {
                    Text(
                        modifier = Modifier
                            .width(AppTokens.dp.iterationCard.historyContextWidth)
                            .clickable(onClick = {}),
                        text = "Context action",
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                    )
                },
            ),
        )
    }
}
