package com.grippo.training.exercise

import com.grippo.core.BaseViewModel
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.IterationFocus
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
        val value = IterationState(
            id = Uuid.random().toString(),
            volume = VolumeFormatState.of(0f),
            repetitions = RepetitionsFormatState.of(0)
        )

        val dialog = DialogConfig.Iteration(
            initial = value,
            focus = IterationFocus.UNIDENTIFIED,
            onResult = { iteration ->
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

    override fun onEditVolume(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            focus = IterationFocus.VOLUME,
            onResult = { iteration ->
                update {
                    val iterations = it.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

                    val exercise = it.exercise.copy(iterations = iterations)
                    it.copy(exercise = exercise)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onEditRepetition(id: String) {
        val value = state.value.exercise.iterations.find { it.id == id } ?: return

        val dialog = DialogConfig.Iteration(
            initial = value,
            focus = IterationFocus.REPETITIONS,
            onResult = { iteration ->
                update {
                    val iterations = it.exercise.iterations
                        .toMutableList()
                        .map { m -> if (m.id == id) iteration else m }
                        .toPersistentList()

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
