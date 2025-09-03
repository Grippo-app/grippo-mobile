package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.database.domain.exercise.equipment.toDomain
import com.grippo.network.Api
import com.grippo.network.database.exercise.example.toEntities
import com.grippo.network.database.exercise.example.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleRepository::class])
internal class ExerciseExampleRepositoryImpl(
    private val api: Api,
    private val exerciseExampleDao: ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(
        name: String,
        forceType: ForceTypeEnum?,
        weightType: WeightTypeEnum?,
        experience: ExperienceEnum?,
        category: CategoryEnum?
    ): Flow<List<ExerciseExample>> {
        return if (name.isBlank()) {
            exerciseExampleDao.getAllFiltered(
                forceType = forceType?.key,
                weightType = weightType?.key,
                category = category?.key,
                experience = experience?.key
            )
        } else {
            val query = name.trim()
                .split("\\s+".toRegex())
                .joinToString(" ") { "$it*" }

            exerciseExampleDao.searchFiltered(
                name = query,
                forceType = forceType?.key,
                weightType = weightType?.key,
                category = category?.key,
                experience = experience?.key
            )
        }.map { it.toDomain() }
    }

    override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> {
        return exerciseExampleDao
            .get(ids)
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