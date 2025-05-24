package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleFilter
import com.grippo.data.features.api.exercise.example.models.SetExerciseExample
import kotlinx.coroutines.flow.Flow

internal class ExerciseExampleFeatureImpl(
    private val repository: ExerciseExampleRepository
) : ExerciseExampleFeature {

    override fun observeExerciseExamples(): Flow<List<ExerciseExample>> {
        return repository.observeExerciseExamples()
    }

    override fun observeExerciseExample(id: String): Flow<ExerciseExample> {
        return repository.observeExerciseExample(id)
    }

    override suspend fun getExerciseExamples(
        page: Int,
        size: Int,
        filter: ExerciseExampleFilter
    ): Result<List<ExerciseExample>> {
        return repository.getExerciseExamples(page, size, filter)
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        return repository.getExerciseExampleById(id)
    }

    override suspend fun setExerciseExample(exerciseExample: SetExerciseExample): Result<Unit> {
        return repository.setExerciseExample(exerciseExample)
    }

    override suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>> {
        return repository.getRecommendedExerciseExamples(
            page,
            size,
            targetMuscleId,
            exerciseCount,
            exerciseExampleIds
        )
    }
}