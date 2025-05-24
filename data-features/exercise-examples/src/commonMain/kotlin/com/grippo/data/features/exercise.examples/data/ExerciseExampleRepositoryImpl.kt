package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleFilter
import com.grippo.data.features.api.exercise.example.models.SetExerciseExample
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.network.Api
import com.grippo.network.mapper.toExerciseExample
import com.grippo.network.mapper.toExerciseExampleEntities
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ExerciseExampleRepositoryImpl(
    private val api: Api,
    private val exerciseExampleDao: ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(): Flow<List<ExerciseExample>> {
        return exerciseExampleDao
            .getAll()
            .map { }
    }

    override fun observeExerciseExample(id: String): Flow<ExerciseExample> {
        TODO("Not yet implemented")
    }

    override suspend fun getExerciseExamples(
        page: Int,
        size: Int,
        filter: ExerciseExampleFilter
    ): Result<List<ExerciseExample>> {
        val response = api.getExerciseExamples(
            page = page,
            size = size,
            forceType = filter.forceType,
            experience = filter.experience,
            category = filter.category,
            muscleIds = filter.muscleIds,
            equipmentIds = filter.equipmentIds,
            query = filter.query,
            weightType = filter.weightType
        )

        response.onSuccess {
            it.toExerciseExampleEntities()
            it.forEach { r ->
                r.toExerciseExample()

                exerciseExampleDao.insertOrUpdate(exerciseExample)
            }
        }
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun setExerciseExample(exerciseExample: SetExerciseExample): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>> {
        TODO("Not yet implemented")
    }
}