package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow

public interface TrainingFeature {
    public fun observeLastTraining(): Flow<Training?>
    public fun observeTrainings(startDate: String, endDate: String): Flow<List<Training>>
    public fun observeTraining(id: String): Flow<Training?>

    public suspend fun syncTrainings(startDate: String, endDate: String): Result<Unit>
    public suspend fun setTraining(training: Training): Result<String?>
    public suspend fun deleteTraining(id: String): Result<Unit>
}