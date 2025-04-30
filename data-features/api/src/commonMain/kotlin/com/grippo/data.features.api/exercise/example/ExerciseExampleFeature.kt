package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleFilter
import com.grippo.data.features.api.exercise.example.models.SetExerciseExample
import kotlinx.coroutines.flow.Flow

public interface ExerciseExampleFeature {
    public fun observeExerciseExamples(): Flow<List<ExerciseExample>>

    public fun observeExerciseExample(id: String): Flow<ExerciseExample>

    public suspend fun getExerciseExamples(
        page: Int,
        size: Int,
        filter: ExerciseExampleFilter
    ): Result<List<ExerciseExample>>

    public suspend fun getExerciseExampleById(id: String): Result<Unit>

    public suspend fun setExerciseExample(
        exerciseExample: SetExerciseExample
    ): Result<Unit>

    public suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>>
}