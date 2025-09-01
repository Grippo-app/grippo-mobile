package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import kotlinx.coroutines.flow.Flow

public interface ExerciseExampleFeature {

    public fun observeExerciseExamples(): Flow<List<ExerciseExample>>

    public fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>>

    public fun observeExerciseExample(id: String): Flow<ExerciseExample?>

    public suspend fun getExerciseExamples(): Result<Unit>

    public suspend fun getExerciseExampleById(id: String): Result<Unit>

    public suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>>
}