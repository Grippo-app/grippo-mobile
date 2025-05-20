package com.grippo.data.features.trainings.data

import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.trainings.domain.TrainingRepository
import com.grippo.database.dao.TrainingDao
import com.grippo.date.utils.DateTimeUtils
import com.grippo.network.Api
import com.grippo.network.mapper.toTrainingFullList
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal class TrainingRepositoryImpl(
    private val api: Api,
    private val trainingDao: TrainingDao,
) : TrainingRepository {

    override fun observeTraining(id: String): Flow<Training?> {
        TODO("Not yet implemented")
    }


    override fun observeTrainings(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<Training>> {
        TODO("Not yet implemented")
    }

    override suspend fun getTrainings(start: LocalDateTime, endDate: LocalDateTime): Result<Unit> {
        val response = api.getTrainings(
            startDate = DateTimeUtils.toUtcIso(start),
            endDate = DateTimeUtils.toUtcIso(endDate),
        )

        response.onSuccess {
            it.toTrainingFullList().forEach {
                trainingDao.insertOrUpdateTrainingFull(it)
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