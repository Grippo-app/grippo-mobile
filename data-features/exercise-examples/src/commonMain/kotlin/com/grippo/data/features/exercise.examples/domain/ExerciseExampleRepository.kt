package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import kotlinx.coroutines.flow.Flow

internal interface ExerciseExampleRepository {
    fun observeExerciseExamples(): Flow<List<ExerciseExample>>

    fun observeExerciseExample(id: String): Flow<ExerciseExample?>

    suspend fun getExerciseExamples(): Result<Unit>

    suspend fun getExerciseExampleById(id: String): Result<Unit>

    suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>>
}