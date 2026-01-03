package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.models.ExerciseSpotlight
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.firstOrNull

public class ExerciseSpotlightUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {
    private companion object {
        private const val EPS = 1e-3f
    }

    public suspend fun fromTrainings(trainings: List<Training>): ExerciseSpotlight? {
        val volumes = trainings.flatMap { training ->
            training.exercises.mapNotNull { exercise ->
                val volume = exercise.volume
                if (volume <= EPS) null else ExerciseVolume(
                    exampleId = exercise.exerciseExample.id,
                    volume = volume
                )
            }
        }

        if (volumes.isEmpty()) return null

        val grouped = volumes.groupBy { it.exampleId }
        val topEntry = grouped.maxByOrNull { entry -> entry.value.sumOf { it.volume.toDouble() } }

        if (topEntry == null) return null

        val totalVolume = topEntry.value.sumOf { it.volume.toDouble() }.toFloat()
        if (totalVolume <= EPS) return null

        val exampleId = topEntry.key
        val example = loadExample(exampleId) ?: return null

        return ExerciseSpotlight(
            example = example,
            totalVolume = totalVolume,
            sessionCount = topEntry.value.size,
        )
    }

    private suspend fun loadExample(id: String): ExerciseExample? {
        return exerciseExampleFeature.observeExerciseExample(id).firstOrNull()
    }

    private data class ExerciseVolume(
        val exampleId: String,
        val volume: Float,
    )
}