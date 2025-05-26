package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleFilter
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.mapper.exercise.equipment.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.exercise.example.toEntities
import com.grippo.network.mapper.exercise.example.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class ExerciseExampleRepositoryImpl(
    private val api: Api,
    private val exerciseExampleDao: ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(): Flow<List<ExerciseExample>> {
        return exerciseExampleDao
            .get()
            .map { it.toDomain() }
    }

    override fun observeExerciseExample(id: String): Flow<ExerciseExample?> {
        return exerciseExampleDao
            .getById(id)
            .map { it.toDomain() }
    }

    override suspend fun getExerciseExamples(
        page: Int,
        size: Int,
        filter: ExerciseExampleFilter
    ): Result<Unit> {
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
            it.forEach { r ->
                val example = r.toEntityOrNull() ?: return@onSuccess
                val bundles = r.exerciseExampleBundles.toEntities()
                val equipments = r.equipmentRefs.toEntities()
                val tutorials = r.tutorials.toEntities()
                exerciseExampleDao.insertOrReplace(example, bundles, equipments, tutorials)
            }
        }

        return response.map { }
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        val response = api.getExerciseExample(id)

        response.onSuccess { r ->
            val example = r.toEntityOrNull() ?: return@onSuccess
            val bundles = r.exerciseExampleBundles.toEntities()
            val equipments = r.equipmentRefs.toEntities()
            val tutorials = r.tutorials.toEntities()
            exerciseExampleDao.insertOrReplace(example, bundles, equipments, tutorials)
        }

        return response.map { }
    }

    override suspend fun getRecommendedExerciseExamples(
        page: Int,
        size: Int,
        targetMuscleId: String?,
        exerciseCount: Int?,
        exerciseExampleIds: List<String>
    ): Result<List<ExerciseExample>> {
        TODO()
    }
}