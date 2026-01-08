package com.grippo.data.features.suggestions.prompt.exercise.example.catalog

import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleCatalog
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext
import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleShare
import com.grippo.entity.domain.equipment.toDomain
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Single

/**
 * Loads the exercise example catalog for the active user by applying
 * exclusions, strict bundle validation, and metadata normalization before the
 * prompting pipeline consumes the data.
 */
@Single
internal class ExampleCatalogLoader(
    private val exerciseExampleDao: com.grippo.services.database.dao.ExerciseExampleDao,
    private val userDao: com.grippo.services.database.dao.UserDao,
    private val userActiveDao: com.grippo.services.database.dao.UserActiveDao
) {

    suspend fun load(): ExampleCatalog? {
        val profileId = getActiveProfileId() ?: return null

        val excludedMuscleIds = userDao.getExcludedMuscles(profileId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val excludedEquipmentIds = userDao.getExcludedEquipments(profileId)
            .firstOrNull()
            ?.map { it.id }
            ?.toSet()
            ?: emptySet()

        val contexts = exerciseExampleDao
            .getAll(
                excludedEquipmentIds = excludedEquipmentIds,
                excludedMuscleIds = excludedMuscleIds,
                limits = null,
                number = null,
                sorting = ExampleSortingEnum.RecentlyUsed.key
            )
            .firstOrNull()
            ?.mapNotNull { it.toContextOrNullStrict() }
            ?: return null

        if (contexts.isEmpty()) return null

        return ExampleCatalog(
            contexts = contexts,
            byId = contexts.associateBy { it.id }
        )
    }

    private fun com.grippo.services.database.models.ExerciseExamplePack.toContextOrNullStrict(): ExampleContext? {
        val value = example.toDomain() ?: return null
        if (value.id.isBlank() || value.name.isBlank()) return null

        data class RawShare(
            val id: String,
            val name: String,
            val percentage: Int,
            val recovery: Int?
        )

        val rawShares = bundles.mapNotNull { pack ->
            val pct = pack.bundle.percentage
            val id = pack.muscle.id
            val name = pack.muscle.name
            val rh: Int = pack.muscle.recovery
            if (pct > 0 && id.isNotBlank() && name.isNotBlank()) {
                RawShare(id, name, pct, rh)
            } else null
        }.sortedByDescending { it.percentage }

        if (rawShares.isEmpty()) return null

        val primary = rawShares.first()
        val primaryRh = primary.recovery
        if (primaryRh == null || primaryRh <= 0) return null

        val merged = buildMap<String, Pair<String, Int>> {
            rawShares.forEach { s ->
                val rh = s.recovery
                if (rh != null && rh > 0) {
                    val prev = this[s.id]
                    val newPct = (prev?.second ?: 0) + s.percentage
                    put(s.id, s.name to newPct)
                }
            }
        }.entries
            .map { (id, pair) -> Triple(id, pair.first, pair.second.coerceAtLeast(0)) }
            .sortedByDescending { it.third }

        if (merged.isEmpty()) return null

        var remaining = 100
        val muscles = buildList {
            for ((id, name, pctRaw) in merged) {
                if (remaining <= 0) break
                val pct = pctRaw.coerceAtMost(remaining)
                remaining -= pct
                add(
                    MuscleShare(
                        id = id,
                        name = name,
                        percentage = pct,
                        recovery = if (id == primary.id) primaryRh else rawShares.firstOrNull { it.id == id }?.recovery
                    )
                )
            }
        }.take(MAX_MUSCLES_PER_EXERCISE)

        if (muscles.isEmpty()) return null

        val category = value.category.key.takeIf { it.isNotBlank() } ?: return null
        val forceType = value.forceType.key.takeIf { it.isNotBlank() } ?: return null
        val weightType = value.weightType.key.takeIf { it.isNotBlank() } ?: return null
        val experience = value.experience.key.takeIf { it.isNotBlank() } ?: return null

        val equipmentIds = equipments
            .mapNotNull { it.equipment.id.takeIf { id -> id.isNotBlank() } }
            .toSet()

        return ExampleContext(
            id = value.id,
            displayName = value.name,
            muscles = muscles,
            category = category,
            forceType = forceType,
            weightType = weightType,
            experience = experience,
            usageCount = value.usageCount,
            lastUsed = value.lastUsed,
            equipmentIds = equipmentIds,
            value = value
        )
    }

    private suspend fun getActiveProfileId(): String? {
        val userId = userActiveDao.get().firstOrNull() ?: return null
        return userDao.getById(userId).firstOrNull()?.profileId
    }

    private companion object {
        private const val MAX_MUSCLES_PER_EXERCISE = 4
    }
}
