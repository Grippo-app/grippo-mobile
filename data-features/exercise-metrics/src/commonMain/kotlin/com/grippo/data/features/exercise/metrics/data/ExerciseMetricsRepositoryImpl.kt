package com.grippo.data.features.exercise.metrics.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.exercise.metrics.domain.ExerciseMetricsRepository
import com.grippo.dto.domain.achievement.toDomain
import com.grippo.dto.domain.training.toDomain
import com.grippo.entity.domain.equipment.toDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.database.dao.ExerciseExampleDao
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Single

@Single(binds = [ExerciseMetricsRepository::class])
internal class ExerciseMetricsRepositoryImpl(
    private val api: GrippoApi,
    private val exampleDao: ExerciseExampleDao,
) : ExerciseMetricsRepository {

    override suspend fun getRecentExercisesByExampleId(id: String): List<Exercise> {
        val response = api.getRecentExercisesByExampleId(id)

        val iterations = response.flatMap {
            it.iterations ?: throw AppError.Network.IncompleteResponse()
        }
        if (response.map { it.id }.toSet().size != response.size ||
            iterations.map { it.id }.toSet().size != iterations.size ||
            response.any { exercise ->
                exercise.exerciseExampleId != id ||
                    exercise.iterations.orEmpty().any { it.exerciseId != exercise.id }
            }
        ) {
            throw AppError.Network.IncompleteResponse()
        }

        val example = exampleDao.getById(id).firstOrNull()?.toDomain()
            ?: throw AppError.Expected(
                message = "Missing exercise example for recent exercises",
                description = null,
            )

        val exercises = response.toDomain(example = example.value)
        if (exercises.size != response.size || response.zip(exercises).any { (raw, mapped) ->
                raw.iterations?.size != mapped.iterations.size
            }
        ) {
            throw AppError.Network.IncompleteResponse()
        }
        return exercises
    }

    override suspend fun getAchievementsByExampleId(id: String): List<Achievement> {
        val response = api.getAchievementsByExampleId(id)

        val achievements = response.toDomain()
        val recordedCount = listOfNotNull(
            response.bestTonnage,
            response.bestWeight,
            response.lifetimeVolume,
            response.maxRepetitions,
            response.peakIntensity,
        ).size
        if (achievements.size != recordedCount || achievements.any { it.exerciseExampleId != id }) {
            throw AppError.Network.IncompleteResponse()
        }
        return achievements
    }
}
