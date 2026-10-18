package com.grippo.data.features.api.training

import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

public interface TrainingFeature {

    public fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>>

    public fun observeTraining(id: String): Flow<Training?>

    public fun observeExercise(id: String): Flow<Exercise?>

    public suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Unit

    public suspend fun setTraining(training: SetTraining): String

    public suspend fun updateTraining(id: String, training: SetTraining): String

    public suspend fun deleteTraining(id: String): Unit

    public fun getDraftTraining(): Flow<DraftTraining?>

    public suspend fun setDraftTraining(draft: DraftTraining): Unit

    public suspend fun deleteDraftTraining(): Unit
}
