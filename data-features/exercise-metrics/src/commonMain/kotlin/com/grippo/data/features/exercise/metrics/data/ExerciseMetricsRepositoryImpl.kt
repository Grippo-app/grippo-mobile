package com.grippo.data.features.exercise.metrics.data

import com.grippo.backend.GrippoApi
import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.exercise.metrics.domain.ExerciseMetricsRepository
import com.grippo.database.dao.ExerciseExampleDao
import com.grippo.dto.domain.training.toDomain
import com.grippo.entity.domain.equipment.toDomain
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Single

@Single(binds = [ExerciseMetricsRepository::class])
internal class ExerciseMetricsRepositoryImpl(
    private val api: GrippoApi,
    private val exampleDao: ExerciseExampleDao,
) : ExerciseMetricsRepository {

    override suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>> {
        val response = api.getRecentExercisesByExampleId(id)

        val example = exampleDao.getById(id).firstOrNull()?.toDomain()
            ?: return Result.failure(
                AppError.Expected(
                    "Missing exercise example for recent exercises",
                    description = null
                )
            )

        return response.map { it.toDomain(example = example.value) }
    }
}