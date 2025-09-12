package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import kotlinx.coroutines.flow.Flow

public interface ExerciseExampleFeature {

    public fun observeExerciseExamples(
        queries: ExampleQueries,
        sorting: ExampleSortingEnum,
        page: ExamplePage
    ): Flow<List<ExerciseExample>>

    public fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>>

    public fun observeExerciseExample(id: String): Flow<ExerciseExample?>

    public suspend fun getExerciseExamples(): Result<Unit>

    public suspend fun getExerciseExampleById(id: String): Result<Unit>
}