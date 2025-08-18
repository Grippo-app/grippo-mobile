package com.grippo.training.exercise

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.IterationState
import kotlinx.collections.immutable.toPersistentList
import kotlin.uuid.Uuid

internal class ExerciseViewModel(
    private val id: String?,
    private val dialogController: DialogController,
) : BaseViewModel<ExerciseState, ExerciseDirection, ExerciseLoader>(
    ExerciseState()
), ExerciseContract {

    override fun onAddIteration() {
        val dialog = DialogConfig.Iteration(
            volume = 20f,
            repeats = 5,
            onResult = { volume, repeats ->
                val iteration = IterationState(
                    id = Uuid.random().toString(),
                    volume = VolumeFormatState(volume),
                    repetitions = RepetitionsFormatState(repeats)
                )

                update {
                    val iterations = it.exercise.iterations
                        .toMutableList()
                        .apply { add(iteration) }
                        .toPersistentList()

                    val exercise = it.exercise.copy(iterations = iterations)

                    it.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onEditIteration(id: String) {
        val iteration = state.value.exercise.iterations.find { it.id == id } ?: return

        val dialog = DialogConfig.Iteration(
            volume = iteration.volume.value,
            repeats = iteration.repetitions.value,
            onResult = { volume, repeats ->
                update {
                    val iterations = it.exercise.iterations
                        .toMutableList()
                        .map { m ->
                            if (m.id == id) m.copy(
                                volume = VolumeFormatState(volume),
                                repetitions = RepetitionsFormatState(repeats)
                            ) else m
                        }.toPersistentList()

                    val exercise = it.exercise.copy(iterations = iterations)

                    it.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onRemoveIteration(id: String) {
        update {
            val iterations = it.exercise.iterations
                .filter { f -> f.id != id }
                .toPersistentList()

            val exercise = it.exercise.copy(iterations = iterations)

            it.copy(exercise = exercise)
        }
    }

    override fun onSave() {
        navigateTo(ExerciseDirection.Back)
    }

    override fun onBack() {
        navigateTo(ExerciseDirection.Back)
    }
}
