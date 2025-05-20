package com.grippo.data.features.trainings.domain

import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal interface TrainingRepository {
    fun observeTrainings(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Training>>
    fun observeTraining(id: String): Flow<Training?>

    suspend fun getTrainings(start: LocalDateTime, endDate: LocalDateTime): Result<Unit>
    suspend fun setTraining(training: Training): Result<String?>
    suspend fun deleteTraining(id: String): Result<Unit>
}