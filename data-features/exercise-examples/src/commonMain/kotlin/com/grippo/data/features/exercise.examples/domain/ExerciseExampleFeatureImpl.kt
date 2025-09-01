package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleFeature::class])
internal class ExerciseExampleFeatureImpl(
    private val repository: ExerciseExampleRepository
) : ExerciseExampleFeature {

    override fun observeExerciseExamples(): Flow<List<ExerciseExample>> {
        return repository.observeExerciseExamples()
    }

    override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> {
        return repository.observeExerciseExamples(ids)
    }

    override fun observeExerciseExample(id: String): Flow<ExerciseExample?> {
        return repository.observeExerciseExample(id)
    }

    override suspend fun getExerciseExamples(): Result<Unit> {
        return repository.getExerciseExamples()
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        return repository.getExerciseExampleById(id)
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