package com.grippo.data.features.exercise.examples.data

import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import com.grippo.dto.entity.example.toEntities
import com.grippo.dto.entity.example.toEntity
import com.grippo.dto.entity.example.toEntityOrNull
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.ExerciseExampleDao
import com.grippo.services.database.entity.ExerciseExampleBundleEntity
import com.grippo.services.database.entity.ExerciseExampleComponentsEntity
import com.grippo.services.database.entity.ExerciseExampleEntity
import com.grippo.services.database.entity.ExerciseExampleEquipmentEntity
import com.grippo.services.database.search.PrefixSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleRepository::class])
internal class ExerciseExampleRepositoryImpl(
    private val api: GrippoApi,
    private val exerciseExampleDao: ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(
        queries: ExampleQueries,
        sorting: ExampleSortingEnum,
        rules: UserExerciseExampleRules,
        page: ExamplePage,
        experience: ExperienceEnum
    ): Flow<List<ExerciseExample>> {
        val nameQuery = queries.name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val shortNameQuery = nameQuery
            ?.takeIf { it.length <= 2 }

        val searchQuery = nameQuery
            ?.takeIf { it.length > 2 }
            ?.let(PrefixSearch::parseQueryOrNull)

        val flow = if (searchQuery == null) {
            exerciseExampleDao.getAll(
                name = shortNameQuery,
                experience = experience.key,
                muscleGroupId = queries.muscleGroupId,
                excludedEquipmentIds = rules.excludedEquipmentIds,
                excludedMuscleIds = rules.excludedMuscleIds,
                sorting = sorting.key,
                limits = page.limits,
                number = page.number
            )
        } else {
            exerciseExampleDao.searchAll(
                searchPhrase = searchQuery.phraseLowercase,
                searchTokens = searchQuery.tokens,
                searchTokenCount = searchQuery.tokens.size,
                experience = experience.key,
                muscleGroupId = queries.muscleGroupId,
                excludedEquipmentIds = rules.excludedEquipmentIds,
                excludedMuscleIds = rules.excludedMuscleIds,
                sorting = sorting.key,
                limits = page.limits,
                number = page.number
            )
        }

        return flow.map { it.toDomain() }
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
            val examples = mutableListOf<ExerciseExampleEntity>()
            val components = mutableListOf<ExerciseExampleComponentsEntity>()
            val bundles = mutableListOf<ExerciseExampleBundleEntity>()
            val equipments = mutableListOf<ExerciseExampleEquipmentEntity>()

            for (r in it) {
                val entity = r.entity ?: return@onSuccess
                val example = r.toEntityOrNull() ?: return@onSuccess
                examples += example
                components += entity.components.toEntity(example.id)
                bundles += entity.exerciseExampleBundles.toEntities()
                equipments += entity.equipmentRefs.toEntities()
            }

            exerciseExampleDao.replaceFromSnapshot(
                examples = examples,
                components = components,
                bundles = bundles,
                equipments = equipments,
            )
        }

        return response.map { }
    }

    override suspend fun getExerciseExampleById(id: String): Result<Unit> {
        val response = api.getExerciseExample(id)

        response.onSuccess { r ->
            val entity = r.entity ?: return@onSuccess
            val example = r.toEntityOrNull() ?: return@onSuccess
            val components = entity.components.toEntity(example.id)
            val bundles = entity.exerciseExampleBundles.toEntities()
            val equipments = entity.equipmentRefs.toEntities()
            exerciseExampleDao.insertOrReplace(example, components, bundles, equipments)
        }

        return response.map { }
    }
}
