package com.grippo.data.features.exercise.metrics.domain

import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.training.models.Exercise
import org.koin.core.annotation.Single

@Single(binds = [ExerciseMetricsFeature::class])
internal class ExerciseMetricsFeatureImpl(
    private val repository: ExerciseMetricsRepository
) : ExerciseMetricsFeature {
    override suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>> {
        return repository.getRecentExercisesByExampleId(id)
    }
}