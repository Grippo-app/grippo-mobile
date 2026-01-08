package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.dto.entity.example.toEntities
import com.grippo.dto.entity.example.toEntityOrNull
import com.grippo.entity.domain.equipment.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleRepository::class])
internal class ExerciseExampleRepositoryImpl(
    private val api: com.grippo.services.backend.GrippoApi,
    private val exerciseExampleDao: com.grippo.services.database.dao.ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(
        queries: ExampleQueries,
        sorting: ExampleSortingEnum,
        rules: UserExerciseExampleRules,
        page: ExamplePage,
        experience: ExperienceEnum
    ): Flow<List<ExerciseExample>> {
        return exerciseExampleDao.getAll(
            name = queries.name,
            forceType = queries.forceType?.key,
            weightType = queries.weightType?.key,
            category = queries.category?.key,
            experience = experience.key,
            muscleGroupId = queries.muscleGroupId,
            excludedEquipmentIds = rules.excludedEquipmentIds,
            excludedMuscleIds = rules.excludedMuscleIds,
            sorting = sorting.key,
            limits = page.limits,
            number = page.number
        ).map { it.toDomain() }
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
                val entity = r.entity ?: return@onSuccess
                val example = r.toEntityOrNull() ?: return@onSuccess
                val bundles = entity.exerciseExampleBundles.toEntities()
                val equipments = entity.equipmentRefs.toEntities()
                exerciseExampleDao.insertOrReplace(example, bundles, equipments)
            }
        }

        return response.map { }
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        val response = api.getExerciseExample(id)

        response.onSuccess { r ->
            val entity = r.entity ?: return@onSuccess
            val example = r.toEntityOrNull() ?: return@onSuccess
            val bundles = entity.exerciseExampleBundles.toEntities()
            val equipments = entity.equipmentRefs.toEntities()
            exerciseExampleDao.insertOrReplace(example, bundles, equipments)
        }

        return response.map { }
    }
}