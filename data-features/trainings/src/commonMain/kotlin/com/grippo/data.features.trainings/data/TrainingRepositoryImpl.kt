package com.grippo.data.features.trainings.data

import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.trainings.domain.TrainingRepository
import com.grippo.database.dao.TrainingDao
import com.grippo.database.mapper.training.toDomain
import com.grippo.date.utils.DateTimeUtils
import com.grippo.network.Api
import com.grippo.network.mapper.training.toEntities
import com.grippo.network.mapper.training.toEntityOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [TrainingRepository::class])
internal class TrainingRepositoryImpl(
    private val api: Api,
    private val trainingDao: TrainingDao,
) : TrainingRepository {

    override fun observeTraining(id: String): Flow<Training?> {
        return trainingDao.getById(id)
            .map { it?.toDomain() }
    }

    override fun observeTrainings(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<Training>> {
        return trainingDao.get(
            from = DateTimeUtils.toUtcIso(start),
            to = DateTimeUtils.toUtcIso(end)
        ).map { it.toDomain() }
    }

    override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        val response = api.getTrainings(
            start = DateTimeUtils.toUtcIso(start),
            end = DateTimeUtils.toUtcIso(end),
        )

        response.onSuccess {
            it.forEach { r ->
                val training = r.toEntityOrNull() ?: return@onSuccess
                val exercises = r.exercises.toEntities()
                val iterations = r.exercises.flatMap { it.iterations }.toEntities()
                trainingDao.insertOrReplace(training, exercises, iterations)
            }
        }

        return response.map {}
    }

    override suspend fun setTraining(training: Training): Result<String?> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteTraining(id: String): Result<Unit> {
        TODO("Not yet implemented")
    }
}