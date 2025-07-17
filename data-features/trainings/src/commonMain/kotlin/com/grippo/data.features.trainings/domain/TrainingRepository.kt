package com.grippo.data.features.trainings.domain

import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal interface TrainingRepository {
    fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>>
    fun observeTraining(id: String): Flow<Training?>
    fun observeExercise(id: String): Flow<Exercise?>

    suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit>
    suspend fun setTraining(training: Training): Result<String?>
    suspend fun deleteTraining(id: String): Result<Unit>
}