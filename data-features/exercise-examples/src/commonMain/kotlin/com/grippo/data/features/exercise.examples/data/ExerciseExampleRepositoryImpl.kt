package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.domain.exercise.equipment.toDomain
import com.grippo.network.Api
import com.grippo.network.mapper.exercise.example.toEntities
import com.grippo.network.mapper.exercise.example.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleRepository::class])
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

    override suspend fun getExerciseExamples(): Result<Unit> {
        val response = api.getExerciseExamples()

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