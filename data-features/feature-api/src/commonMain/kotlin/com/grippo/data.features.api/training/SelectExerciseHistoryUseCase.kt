package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Training

public class SelectExerciseHistoryUseCase {

    public fun execute(current: Exercise, trainings: List<Training>): List<Exercise> {
        val currentTraining = trainings.firstOrNull { training ->
            training.exercises.any { it.id == current.id }
        }
        val cutoff = currentTraining?.createdAt ?: current.createdAt

        return trainings
            .filter { training ->
                training.createdAt < cutoff && training.exercises.none { it.id == current.id }
            }
            .mapNotNull { training ->
                // Repeated exercises in one session remain separate records; compare its last recorded entry.
                training.exercises
                    .withIndex()
                    .filter { (_, exercise) ->
                        exercise.exerciseExample.id == current.exerciseExample.id &&
                            exercise.createdAt < current.createdAt &&
                            exercise.hasRecordedSets()
                    }
                    .maxWithOrNull(
                        compareBy<IndexedValue<Exercise>> { it.value.createdAt }
                            .thenBy { it.index }
                    )
                    ?.value
            }
            .distinctBy { it.id }
            .sortedBy { it.createdAt }
            .takeLast(MAX_HISTORY_RECORDS)
    }

    private fun Exercise.hasRecordedSets(): Boolean {
        return iterations.any { iteration ->
            val hasLoadInput = iteration.externalWeight != null ||
                iteration.bodyWeight != null ||
                iteration.extraWeight != null ||
                iteration.assistWeight != null

            iteration.repetitions > 0 && hasLoadInput
        }
    }

    private companion object {
        private const val MAX_HISTORY_RECORDS = 5
    }
}
