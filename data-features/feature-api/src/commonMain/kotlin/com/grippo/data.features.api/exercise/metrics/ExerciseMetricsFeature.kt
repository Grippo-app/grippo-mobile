package com.grippo.data.features.api.exercise.metrics

import com.grippo.data.features.api.training.models.Exercise

public interface ExerciseMetricsFeature {

    public suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>>
}