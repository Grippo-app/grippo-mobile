package com.grippo.design.components.training

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.IntensityFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.metrics.volume.TrainingTotalState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.training.internal.ExerciseCardRow
import com.grippo.design.components.training.internal.ExerciseCardSmall
import com.grippo.design.components.training.internal.ExercisePlanSummary
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime

@Stable
public sealed interface ExerciseCardStyle {
    @Stable
    public data class Large(
        public val onClick: () -> Unit
    ) : ExerciseCardStyle

    @Stable
    public data class Medium(
        public val onClick: (() -> Unit)? = null,
    ) : ExerciseCardStyle

    /**
     * Exercise-details history entry: the date and the recorded sets of one session.
     * [trailing] is a neutral caller-owned slot in the header's trailing position.
     */
    @Stable
    public data class Small(
        public val trailing: (@Composable () -> Unit)? = null,
    ) : ExerciseCardStyle

    @Immutable
    public data object Planned : ExerciseCardStyle
}

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    style: ExerciseCardStyle,
) {
    when (style) {
        ExerciseCardStyle.Planned -> ExerciseCardRow(
            modifier = modifier,
            value = value,
            isCompact = true,
            onClick = null,
            summary = { ExercisePlanSummary(value = value) },
        )

        is ExerciseCardStyle.Small -> ExerciseCardSmall(
            modifier = modifier,
            value = value,
            style = style,
        )

        is ExerciseCardStyle.Medium -> ExerciseCardRow(
            modifier = modifier,
            value = value,
            isCompact = true,
            onClick = style.onClick
        )

        is ExerciseCardStyle.Large -> ExerciseCardRow(
            modifier = modifier,
            value = value,
            isCompact = false,
            onClick = style.onClick
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardReadOnlyPreview() {
    PreviewContainer {
        val exercise = exercisePreviewValue()

        ExerciseCard(
            value = exercise.copy(iterations = persistentListOf(exercisePreviewPendingIteration())),
            style = ExerciseCardStyle.Medium(),
        )
        ExerciseCard(
            value = exercise,
            style = ExerciseCardStyle.Medium(),
        )
        ExerciseCard(
            value = exercise.copy(
                exerciseExample = exercise.exerciseExample.copy(
                    name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                ),
            ),
            style = ExerciseCardStyle.Medium(),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        val exercise = exercisePreviewValue()

        ExerciseCard(
            value = exercise,
            style = ExerciseCardStyle.Small(
                trailing = {
                    Text(
                        text = "+8%",
                        style = AppTokens.typography.b12Semi(),
                        color = AppTokens.colors.text.secondary,
                    )
                },
            ),
        )
        ExerciseCard(
            value = exercise,
            style = ExerciseCardStyle.Medium(
                onClick = {}
            ),
        )
        ExerciseCard(
            value = exercise,
            style = ExerciseCardStyle.Large(
                onClick = {}
            ),
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardStatesPreview() {
    PreviewContainer {
        val base = exercisePreviewValue()
        val pending = exercisePreviewPendingIteration()
        val completed = pending.copy(
            id = "completed-set",
            externalWeight = VolumeFormatState.of(80f),
        )

        listOf(
            base.copy(iterations = persistentListOf(pending)),
            base.copy(iterations = persistentListOf(completed, pending)),
            base.copy(iterations = persistentListOf(completed)),
            base.copy(iterations = persistentListOf()),
            base.copy(
                exerciseExample = base.exerciseExample.copy(
                    name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом"
                ),
                iterations = persistentListOf(completed, pending),
            ),
        ).forEach { exercise ->
            ExerciseCard(value = exercise, style = ExerciseCardStyle.Medium(onClick = {}))
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardPlannedPreview() {
    PreviewContainer {
        val base = exercisePreviewValue()
        val known = exercisePreviewPendingIteration()
        val unknown = known.copy(repetitions = RepetitionsFormatState.Empty())

        listOf(
            base.copy(iterations = persistentListOf(known, known.copy(id = "set-2"))),
            base.copy(iterations = persistentListOf(known, known.copy(repetitions = RepetitionsFormatState.of(12)))),
            base.copy(iterations = persistentListOf(known, unknown)),
            base.copy(iterations = persistentListOf(unknown)),
            base.copy(iterations = persistentListOf()),
            base.copy(
                exerciseExample = base.exerciseExample.copy(
                    name = "Жим гантелей лёжа на наклонной скамье нейтральным хватом",
                ),
                iterations = persistentListOf(known),
            ),
        ).forEach { exercise ->
            ExerciseCard(value = exercise, style = ExerciseCardStyle.Planned)
        }
    }
}

private fun exercisePreviewValue(): ExerciseState {
    val base = stubExercise()

    return base.copy(
        id = "bench-press",
        exerciseExample = base.exerciseExample.copy(
            id = "bench-press-example",
            name = "Dumbbell bench press",
            imageUrl = null,
        ),
        iterations = persistentListOf(
            exercisePreviewPendingIteration().copy(
                id = "completed-set",
                externalWeight = VolumeFormatState.of(80f),
            ),
            exercisePreviewPendingIteration(),
        ),
        createdAt = DateTimeFormatState.of(
            value = LocalDateTime(2026, 10, 16, 10, 0),
            range = DateRangePresets.infinity(),
            format = DateFormat.DateOnly.DateMmmDdYyyy,
        ),
        total = TrainingTotalState(
            volume = VolumeFormatState.of(640f),
            repetitions = RepetitionsFormatState.of(8),
            intensity = IntensityFormatState.of(80f),
        ),
    )
}

private fun exercisePreviewPendingIteration() = stubPendingIteration().copy(
    id = "pending-set",
    repetitions = RepetitionsFormatState.of(8),
)
