package com.grippo.data.features.api.exercise.metrics

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.training.models.Exercise

public interface ExerciseMetricsFeature {

    public suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>>

    public suspend fun getAchievementsByExampleId(id: String): Result<List<Achievement>>
}