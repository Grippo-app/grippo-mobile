package com.grippo.data.features.exercise.metrics.domain

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.training.models.Exercise

internal interface ExerciseMetricsRepository {

    suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>>

    suspend fun getAchievementsByExampleId(id: String): Result<List<Achievement>>
}