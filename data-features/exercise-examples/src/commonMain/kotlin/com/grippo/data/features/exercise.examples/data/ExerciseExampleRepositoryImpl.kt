package com.grippo.data.features.exercise.examples.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleScope
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
import com.grippo.toolkit.localization.AppLocale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ExerciseExampleRepository::class])
internal class ExerciseExampleRepositoryImpl(
    private val api: GrippoApi,
    private val exerciseExampleDao: ExerciseExampleDao,
) : ExerciseExampleRepository {

    override fun observeExerciseExamples(
        name: String?,
        scope: ExampleScope,
        rules: UserExerciseExampleRules,
        page: ExamplePage,
        experience: ExperienceEnum
    ): Flow<List<ExerciseExample>> {
        val nameQuery = name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val shortNameQuery = nameQuery
            ?.takeIf { it.length <= 2 }

        val searchQuery = nameQuery
            ?.takeIf { it.length > 2 }
            ?.let(PrefixSearch::parseQueryOrNull)

        val flow = when (scope) {
            is ExampleScope.All -> if (searchQuery == null) {
                exerciseExampleDao.getAll(
                    name = shortNameQuery,
                    experience = experience.key,
                    muscleGroupId = scope.muscleGroupId,
                    excludedEquipmentIds = rules.excludedEquipmentIds,
                    excludedMuscleIds = rules.excludedMuscleIds,
                    sorting = scope.sorting.key,
                    limits = page.limits,
                    number = page.number
                )
            } else {
                exerciseExampleDao.searchAll(
                    searchPhrase = searchQuery.phraseLowercase,
                    searchTokens = searchQuery.tokens,
                    searchTokenCount = searchQuery.tokens.size,
                    experience = experience.key,
                    muscleGroupId = scope.muscleGroupId,
                    excludedEquipmentIds = rules.excludedEquipmentIds,
                    excludedMuscleIds = rules.excludedMuscleIds,
                    sorting = scope.sorting.key,
                    limits = page.limits,
                    number = page.number
                )
            }

            is ExampleScope.SimilarTo -> if (searchQuery == null) {
                exerciseExampleDao.getAllSimilar(
                    targetId = scope.targetId,
                    name = shortNameQuery,
                    experience = experience.key,
                    excludedEquipmentIds = rules.excludedEquipmentIds,
                    excludedMuscleIds = rules.excludedMuscleIds,
                    limits = page.limits,
                    number = page.number
                )
            } else {
                exerciseExampleDao.searchAllSimilar(
                    targetId = scope.targetId,
                    searchTokens = searchQuery.tokens,
                    searchTokenCount = searchQuery.tokens.size,
                    experience = experience.key,
                    excludedEquipmentIds = rules.excludedEquipmentIds,
                    excludedMuscleIds = rules.excludedMuscleIds,
                    limits = page.limits,
                    number = page.number
                )
            }
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

    override suspend fun getExerciseExamples(): Unit {
        val locale = AppLocale.current()
        val response = api.getExerciseExamples(locale = locale)

        val examples = mutableListOf<ExerciseExampleEntity>()
        val components = mutableListOf<ExerciseExampleComponentsEntity>()
        val bundles = mutableListOf<ExerciseExampleBundleEntity>()
        val equipments = mutableListOf<ExerciseExampleEquipmentEntity>()

        for (value in response) {
            val entity = value.entity ?: throw AppError.Network.IncompleteResponse()
            val example = value.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
            val mappedComponents = entity.components.toEntity(example.id)
            val mappedBundles = entity.exerciseExampleBundles.toEntities()
            val mappedEquipments = entity.equipmentRefs.toEntities()
            if (mappedBundles.size != entity.exerciseExampleBundles?.size ||
                mappedEquipments.size != entity.equipmentRefs?.size ||
                mappedBundles.any { it.exerciseExampleId != example.id } ||
                mappedEquipments.any { it.exerciseExampleId != example.id }
            ) {
                throw AppError.Network.IncompleteResponse()
            }
            example.toDomain() ?: throw AppError.Network.IncompleteResponse()
            mappedComponents.toDomain() ?: throw AppError.Network.IncompleteResponse()
            examples += example
            components += mappedComponents
            bundles += mappedBundles
            equipments += mappedEquipments
        }

        if (examples.map { it.id }.toSet().size != examples.size ||
            bundles.map { it.id }.toSet().size != bundles.size ||
            equipments.map { it.id }.toSet().size != equipments.size
        ) {
            throw AppError.Network.IncompleteResponse()
        }

        if (locale != AppLocale.current()) {
            throw CancellationException("The exercise catalog locale changed")
        }
        exerciseExampleDao.replaceFromSnapshot(
            examples = examples,
            components = components,
            bundles = bundles,
            equipments = equipments,
        )
    }

    override suspend fun getExerciseExampleById(id: String): Unit {
        val locale = AppLocale.current()
        val response = api.getExerciseExample(id = id, locale = locale)

        val entity = response.entity ?: throw AppError.Network.IncompleteResponse()
        val example = response.toEntityOrNull() ?: throw AppError.Network.IncompleteResponse()
        val components = entity.components.toEntity(example.id)
        val bundles = entity.exerciseExampleBundles.toEntities()
        val equipments = entity.equipmentRefs.toEntities()
        if (example.id != id || bundles.size != entity.exerciseExampleBundles?.size ||
            equipments.size != entity.equipmentRefs?.size ||
            bundles.any { it.exerciseExampleId != example.id } ||
            equipments.any { it.exerciseExampleId != example.id } ||
            bundles.map { it.id }.toSet().size != bundles.size ||
            equipments.map { it.id }.toSet().size != equipments.size
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        example.toDomain() ?: throw AppError.Network.IncompleteResponse()
        components.toDomain() ?: throw AppError.Network.IncompleteResponse()
        if (locale != AppLocale.current()) {
            throw CancellationException("The exercise catalog locale changed")
        }
        exerciseExampleDao.insertOrReplace(
            example = example,
            components = components,
            bundles = bundles,
            equipments = equipments,
        )
    }
}
