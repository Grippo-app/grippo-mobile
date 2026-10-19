package com.grippo.data.features.exercise.examples.domain

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleScope
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleFeature::class])
internal class ExerciseExampleFeatureImpl(
    private val repository: ExerciseExampleRepository
) : ExerciseExampleFeature {

    override fun observeExerciseExamples(
        name: String?,
        scope: ExampleScope,
        rules: UserExerciseExampleRules,
        page: ExamplePage,
        experience: ExperienceEnum
    ): Flow<List<ExerciseExample>> {
        return repository.observeExerciseExamples(
            name = name,
            scope = scope,
            rules = rules,
            page = page,
            experience = experience
        )
    }

    override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> {
        return repository.observeExerciseExamples(ids)
    }

    override fun observeExerciseExample(id: String): Flow<ExerciseExample?> {
        return repository.observeExerciseExample(id)
    }

    override suspend fun getExerciseExamples(): Unit {
        return repository.getExerciseExamples()
    }

    override suspend fun getExerciseExampleById(id: String): Unit {
        return repository.getExerciseExampleById(id)
    }
}