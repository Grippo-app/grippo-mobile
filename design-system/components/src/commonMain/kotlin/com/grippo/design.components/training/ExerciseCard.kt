package com.grippo.design.components.training

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubIteration
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.training.internal.ExerciseCardRow
import com.grippo.design.components.training.internal.ExerciseCardSmall
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import kotlinx.collections.immutable.persistentListOf

@Stable
public sealed interface ExerciseCardStyle {
    @Stable
    public data class Large(
        val onClick: () -> Unit
    ) : ExerciseCardStyle

    @Stable
    public data class Medium(
        val onClick: () -> Unit
    ) : ExerciseCardStyle

    @Immutable
    public data object Small : ExerciseCardStyle
}

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState,
    style: ExerciseCardStyle,
) {

    when (style) {
        is ExerciseCardStyle.Small -> ExerciseCardSmall(
            modifier = modifier,
            value = value,
        )

        is ExerciseCardStyle.Medium -> ExerciseCardRow(
            modifier = modifier,
            value = value,
            onClick = style.onClick
        )

        is ExerciseCardStyle.Large -> ExerciseCardRow(
            modifier = modifier,
            value = value,
            onClick = style.onClick
        )
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise(),
            style = ExerciseCardStyle.Small
        )
        ExerciseCard(
            value = stubExercise(),
            style = ExerciseCardStyle.Medium(
                onClick = {}
            ),
        )
        ExerciseCard(
            value = stubExercise(),
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
        val base = stubExercise()
        val pending = stubPendingIteration()
        val completed = stubIteration()
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
            ExerciseCard(value = exercise, style = ExerciseCardStyle.Small)
        }
    }
}
