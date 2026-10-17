package com.grippo.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.formatters.MultiplierFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.divider.ContentDivider
import com.grippo.design.components.training.IterationCard
import com.grippo.design.components.training.IterationCardStyle
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_history_previous_set_date
import com.grippo.design.resources.provider.exercise_history_set_result
import com.grippo.design.resources.provider.exercise_history_vs_date
import com.grippo.design.resources.provider.kg
import com.grippo.design.resources.provider.sets_value
import com.grippo.design.resources.provider.training_previous_set
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Composable
internal fun ExerciseSetsContent(
    modifier: Modifier = Modifier,
    value: ImmutableList<IterationState>,
    previous: ExerciseState?,
    isHistoryLoading: Boolean,
) {
    val contextStyle = AppTokens.typography.b12Med()
    val contextHeight = with(LocalDensity.current) { contextStyle.lineHeight.toDp() }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTokens.dp.contentPadding.subContent),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.sets_value, value.size),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
            )
            Box(
                modifier = Modifier
                    .width(AppTokens.dp.iterationCard.historyContextWidth)
                    .heightIn(min = contextHeight * 2),
                contentAlignment = Alignment.CenterEnd,
            ) {
                when {
                    isHistoryLoading -> ExerciseComparisonBone(
                        modifier = Modifier.fillMaxWidth().height(contextHeight),
                    )

                    previous != null -> {
                        val date = previous.createdAt.value?.let { date ->
                            DateTimeUtils.format(value = date, format = DateFormat.DateOnly.DateDdMmm)
                        } ?: previous.createdAt.display
                        val description = AppTokens.strings.res(
                            Res.string.exercise_history_previous_set_date,
                            previous.createdAt.display,
                        )
                        Text(
                            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                            text = AppTokens.strings.res(Res.string.exercise_history_vs_date, date),
                            style = contextStyle,
                            color = AppTokens.colors.text.secondary,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
        value.forEachIndexed { index, iteration ->
            key(iteration.id) {
                val previousSet = previous?.iterations?.getOrNull(index)
                    ?.takeIf { iteration.hasComparableLoadTo(it) }
                IterationCard(
                    value = iteration,
                    style = IterationCardStyle.Recorded(
                        number = index + 1,
                        trailing = {
                            ExerciseSetComparison(
                                value = previousSet,
                                isLoading = isHistoryLoading,
                            )
                        },
                    ),
                )
                if (index < value.lastIndex) {
                    ContentDivider()
                }
            }
        }
    }
}

@Composable
private fun ExerciseSetComparison(
    modifier: Modifier = Modifier,
    value: IterationState?,
    isLoading: Boolean,
) {
    val style = AppTokens.typography.b12Med()
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }

    Box(
        modifier = modifier
            .width(AppTokens.dp.iterationCard.historyContextWidth)
            .heightIn(min = lineHeight * 2),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (isLoading) {
            ExerciseComparisonBone(modifier = Modifier.fillMaxWidth().height(lineHeight))
        } else if (value != null) {
            val rawVolume = value.recordedVolume()
            val volume = VolumeFormatState.of(rawVolume)
            val load = when {
                rawVolume == null -> "—"
                volume.value == null -> "0${AppTokens.strings.res(Res.string.kg)}"
                else -> volume.short()
            }
            val repetitions = value.repetitions.value?.toString() ?: "—"
            val result = AppTokens.strings.res(Res.string.exercise_history_set_result, load, repetitions)
            val description = AppTokens.strings.res(Res.string.training_previous_set, load, repetitions)

            Text(
                modifier = Modifier.clearAndSetSemantics { contentDescription = description },
                text = result,
                style = style,
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ExerciseComparisonBone(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.metrics.status.radius))
            .background(AppTokens.colors.divider.default)
            .shimmer()
            .clearAndSetSemantics {},
    )
}

@AppPreview
@Composable
private fun ExerciseSetsContentExternalBodyAndPartialPreview() {
    val bodySet = stubPendingIteration().copy(
        id = "body-set",
        bodyWeight = WeightFormatState.of(80f),
        bodyMultiplier = MultiplierFormatState.of(1f),
        repetitions = RepetitionsFormatState.of(8),
    )
    PreviewContainer {
        ExerciseSetsContent(
            value = persistentListOf(
                bodySet.copy(
                    id = "external-set",
                    bodyWeight = WeightFormatState.Empty(),
                    bodyMultiplier = MultiplierFormatState.Empty(),
                    externalWeight = VolumeFormatState.of(80f),
                ),
                bodySet,
                bodySet.copy(id = "assisted-set", assistWeight = VolumeFormatState.of(30f)),
                bodySet.copy(id = "extra-set", extraWeight = VolumeFormatState.of(20f)),
                bodySet.copy(id = "zero-set", assistWeight = VolumeFormatState.of(80f)),
                bodySet.copy(
                    id = "rounded-zero-set",
                    bodyWeight = WeightFormatState.of(70.3f),
                    bodyMultiplier = MultiplierFormatState.of(0.43f),
                    assistWeight = VolumeFormatState.of(30.2f),
                ),
                bodySet.copy(id = "partial-set", bodyMultiplier = MultiplierFormatState.Empty()),
                bodySet.copy(id = "missing-reps", repetitions = RepetitionsFormatState.Empty()),
                bodySet.copy(
                    id = "optional-body-extra",
                    bodyWeight = WeightFormatState.Empty(),
                    bodyMultiplier = MultiplierFormatState.Empty(),
                    extraWeight = VolumeFormatState.of(20f),
                ),
                bodySet.copy(
                    id = "optional-body-assist",
                    bodyWeight = WeightFormatState.Empty(),
                    bodyMultiplier = MultiplierFormatState.Empty(),
                    assistWeight = VolumeFormatState.of(20f),
                ),
                bodySet.copy(
                    id = "optional-body-with-coefficient",
                    bodyWeight = WeightFormatState.Empty(),
                    extraWeight = VolumeFormatState.of(20f),
                ),
                bodySet.copy(id = "coefficient-only", bodyWeight = WeightFormatState.Empty()),
            ),
            previous = null,
            isHistoryLoading = false,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSetsContentWithPreviousPreview() {
    val iteration = stubPendingIteration().copy(
        id = "current-set",
        externalWeight = VolumeFormatState.of(90f),
        repetitions = RepetitionsFormatState.of(8),
    )
    val previous = stubExercise().copy(
        createdAt = DateTimeFormatState.of(
            value = LocalDateTime(year = 2026, monthNumber = 10, dayOfMonth = 7, hour = 18, minute = 0),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        iterations = persistentListOf(
            iteration.copy(id = "previous-set", externalWeight = VolumeFormatState.of(80f)),
        ),
    )
    PreviewContainer {
        ExerciseSetsContent(
            value = persistentListOf(iteration),
            previous = previous,
            isHistoryLoading = false,
        )
    }
}

@AppPreview
@Composable
private fun ExerciseSetsContentLoadingPreviousPreview() {
    val iteration = stubPendingIteration().copy(
        id = "loading-previous-set",
        externalWeight = VolumeFormatState.of(90f),
        repetitions = RepetitionsFormatState.of(8),
    )
    PreviewContainer {
        ExerciseSetsContent(value = persistentListOf(iteration), previous = null, isHistoryLoading = true)
    }
}
