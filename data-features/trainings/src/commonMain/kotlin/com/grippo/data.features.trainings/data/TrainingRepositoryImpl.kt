package com.grippo.data.features.trainings.data

import com.grippo.backend.GrippoApi
import com.grippo.backend.dto.training.TrainingResponse
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetDraftTraining
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.trainings.domain.TrainingRepository
import com.grippo.database.dao.DraftTrainingDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.dao.UserActiveDao
import com.grippo.domain.dto.training.toBody
import com.grippo.domain.entity.training.toEntity
import com.grippo.dto.entity.training.toEntities
import com.grippo.dto.entity.training.toEntityOrNull
import com.grippo.entity.domain.training.toDomain
import com.grippo.entity.domain.training.toSetDomain
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [TrainingRepository::class])
internal class TrainingRepositoryImpl(
    private val api: GrippoApi,
    private val trainingDao: TrainingDao,
    private val draftTrainingDao: DraftTrainingDao,
    private val userActiveDao: UserActiveDao,
) : TrainingRepository {

    override fun observeTraining(id: String): Flow<Training?> {
        return trainingDao.getById(id)
            .map { it?.toDomain() }
    }

    override fun observeExercise(id: String): Flow<Exercise?> {
        return trainingDao.getExerciseById(id)
            .map { it?.toDomain() }
    }

    override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> {
        return trainingDao.get(
            from = DateTimeUtils.toUtcIso(start),
            to = DateTimeUtils.toUtcIso(end)
        ).map { it.toDomain() }
    }

    override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> {
        val response = api.getTrainings(
            start = DateTimeUtils.toUtcIso(start),
            end = DateTimeUtils.toUtcIso(end)
        )

        response.onSuccess { r ->
            r.forEach { training -> provideTraining(training) }
        }

        return response.map {}
    }

    override suspend fun updateTraining(id: String, training: SetTraining): Result<String?> {
        val response = api.updateTraining(
            id = id,
            body = training.toBody()
        )

        response.onSuccess { r ->
            val training = api.getTraining(id).getOrNull() ?: return@onSuccess
            provideTraining(training)
        }

        return Result.success(id)
    }

    override suspend fun setTraining(training: SetTraining): Result<String?> {
        val response = api.setTraining(
            body = training.toBody()
        )

        response.onSuccess { r ->
            val id = r.id ?: return@onSuccess
            val training = api.getTraining(id).getOrNull() ?: return@onSuccess
            provideTraining(training)
        }

        return response.map { it.id }
    }

    override suspend fun deleteTraining(id: String): Result<Unit> {
        val response = api.deleteTraining(
            id = id,
        )

        response.onSuccess { r ->
            trainingDao.deleteById(id)
        }

        return response
    }

    private suspend fun provideTraining(value: TrainingResponse) {
        val training = value.toEntityOrNull() ?: return
        val exercises = value.exercises.toEntities()
        val iterations = value.exercises.flatMap { f -> f.iterations }.toEntities()
        trainingDao.insertOrReplace(training, exercises, iterations)
    }

    override fun getDraftTraining(): Flow<SetDraftTraining?> {
        return draftTrainingDao.get()
            .map { it?.toSetDomain() }
    }

    override suspend fun setDraftTraining(training: SetDraftTraining): Result<Unit> {
        val activeId = userActiveDao.get().firstOrNull() ?: return Result.success(Unit)

        val pack = training.toEntity(activeId)

        val training = pack.training
        val exercises = pack.exercises.map { it.exercise }
        val iterations = pack.exercises.flatMap { it.iterations }

        draftTrainingDao.insertOrReplace(training, exercises, iterations)

        return Result.success(Unit)
    }

    override suspend fun deleteDraftTraining(): Result<Unit> {
        draftTrainingDao.delete()
        return Result.success(Unit)
    }
}