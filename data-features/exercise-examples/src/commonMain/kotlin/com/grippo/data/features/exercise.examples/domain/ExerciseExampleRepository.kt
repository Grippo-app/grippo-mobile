package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import kotlinx.coroutines.flow.Flow

internal interface ExerciseExampleRepository {
    fun observeExerciseExamples(
        name: String,
        forceType: ForceTypeEnum?,
        weightType: WeightTypeEnum?,
        experience: ExperienceEnum?,
        category: CategoryEnum?
    ): Flow<List<ExerciseExample>>

    fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>>

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