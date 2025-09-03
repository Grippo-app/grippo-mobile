package com.grippo.data.features.api.exercise.example

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import kotlinx.coroutines.flow.Flow

public interface ExerciseExampleFeature {

    public fun observeExerciseExamples(
        name: String,
        forceType: ForceTypeEnum?,
        weightType: WeightTypeEnum?,
        experience: ExperienceEnum?,
        category: CategoryEnum?
    ): Flow<List<ExerciseExample>>

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