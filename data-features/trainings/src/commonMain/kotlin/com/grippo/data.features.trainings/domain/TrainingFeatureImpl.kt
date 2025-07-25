package com.grippo.data.features.trainings.domain

import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [TrainingFeature::class])
internal class TrainingFeatureImpl(
    private val repository: TrainingRepository
) : TrainingFeature {

    override fun observeTraining(id: String): Flow<Training?> {
        return repository.observeTraining(id)
    }

    override fun observeExercise(id: String): Flow<Exercise?> {
        return repository.observeExercise(id)
    }

    override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> {
        return repository.observeTrainings(start, end)
    }

    override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        return repository.getTrainings(start, end)
    }

    override suspend fun setTraining(training: Training): Result<String?> {
        return repository.setTraining(training)
    }

    override suspend fun deleteTraining(id: String): Result<Unit> {
        return repository.deleteTraining(id)
    }
}