package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import kotlinx.coroutines.flow.Flow

internal interface ExerciseExampleRepository {
    fun observeExerciseExamples(
        queries: ExampleQueries,
        sorting: ExampleSortingEnum
    ): Flow<List<ExerciseExample>>

    fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>>

    fun observeExerciseExample(id: String): Flow<ExerciseExample?>

    suspend fun getExerciseExamples(): Result<Unit>

    suspend fun getExerciseExampleById(id: String): Result<Unit>
}