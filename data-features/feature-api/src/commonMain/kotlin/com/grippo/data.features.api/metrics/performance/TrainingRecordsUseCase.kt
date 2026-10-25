package com.grippo.data.features.api.metrics.performance

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.metrics.performance.models.TrainingRecord
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

public class TrainingRecordsUseCase(
    private val exerciseMetricsFeature: ExerciseMetricsFeature,
) {

    public suspend fun fromTraining(training: Training): List<TrainingRecord> {
        val exercises = training.exercises.filter { exercise ->
            exercise.id.isNotBlank() && exercise.exerciseExample.id.isNotBlank()
        }
        val exercisesByExample = exercises.groupBy { exercise -> exercise.exerciseExample.id }
        val achievementsByExample = coroutineScope {
            exercisesByExample.keys
                .map { exampleId ->
                    async {
                        val achievements = exerciseMetricsFeature.getAchievementsByExampleId(exampleId)
                        exampleId to achievements
                    }
                }
                .awaitAll()
                .toMap()
        }

        return exercisesByExample.mapNotNull { (exampleId, sameExample) ->
            val achievements = achievementsByExample[exampleId].orEmpty()
            recordOf(exercises = sameExample, achievements = achievements)
        }
    }

    private fun recordOf(exercises: List<Exercise>, achievements: List<Achievement>): TrainingRecord? {
        val exerciseIds = exercises.map { exercise -> exercise.id }.toSet()
        val counted = achievements
            .filter { achievement -> exerciseIdOf(achievement) in exerciseIds }
            .filter(::hasRecordedValue)
            .sortedBy(::kindOrder)
        if (counted.isEmpty()) {
            return null
        }

        return TrainingRecord(
            exerciseExample = exercises.first().exerciseExample,
            achievements = counted,
        )
    }

    private fun exerciseIdOf(achievement: Achievement): String? {
        return when (achievement) {
            is Achievement.BestTonnage -> achievement.exerciseId
            is Achievement.BestWeight -> achievement.exerciseId
            is Achievement.MaxRepetitions -> achievement.exerciseId
            is Achievement.PeakIntensity -> achievement.exerciseId
            is Achievement.LifetimeVolume -> null
        }
    }

    private fun hasRecordedValue(achievement: Achievement): Boolean {
        return when (achievement) {
            is Achievement.BestTonnage -> achievement.tonnage > 0f
            is Achievement.BestWeight -> achievement.weight > 0f
            is Achievement.MaxRepetitions -> achievement.repetitions > 0
            is Achievement.PeakIntensity -> achievement.intensity > 0.0
            is Achievement.LifetimeVolume -> false
        }
    }

    private fun kindOrder(achievement: Achievement): Int {
        return when (achievement) {
            is Achievement.BestWeight -> 0
            is Achievement.BestTonnage -> 1
            is Achievement.MaxRepetitions -> 2
            is Achievement.PeakIntensity -> 3
            is Achievement.LifetimeVolume -> 4
        }
    }
}
