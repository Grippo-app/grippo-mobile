package com.grippo.data.features.trainings.data

import com.grippo.core.error.provider.AppError
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.trainings.domain.TrainingRepository
import com.grippo.domain.dto.training.toBody
import com.grippo.domain.entity.training.toEntity
import com.grippo.dto.entity.training.toEntities
import com.grippo.dto.entity.training.toEntityOrNull
import com.grippo.entity.domain.training.toDomain
import com.grippo.entity.domain.training.toDraftDomain
import com.grippo.services.backend.GrippoApi
import com.grippo.services.backend.dto.training.TrainingResponse
import com.grippo.services.database.dao.DraftTrainingDao
import com.grippo.services.database.dao.TrainingDao
import com.grippo.services.database.dao.UserActiveDao
import com.grippo.services.database.dao.UserDao
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.uuid.Uuid
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
    private val userDao: UserDao,
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

    override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Unit {
        val startUtc = DateTimeUtils.toUtcIso(start)
        val endUtc = DateTimeUtils.toUtcIso(end)
        val response = api.getTrainings(
            start = startUtc,
            end = endUtc
        )

        val actualIds = response.mapNotNull(TrainingResponse::id)
        val duplicateTrainingIds = actualIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val rawExercises = response.flatMap { it.exercises.orEmpty() }
        val duplicateExerciseIds = rawExercises.mapNotNull { it.id }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val duplicateIterationIds = rawExercises.flatMap { it.iterations.orEmpty() }.mapNotNull { it.id }
            .groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        val savedCount = response.count { training ->
            val hasDuplicateIds = training.id?.let { it in duplicateTrainingIds } == true ||
                training.exercises.orEmpty().any { exercise ->
                    exercise.id?.let { it in duplicateExerciseIds } == true ||
                        exercise.iterations.orEmpty().any { iteration ->
                            iteration.id?.let { it in duplicateIterationIds } == true
                        }
                }
            !hasDuplicateIds && provideTraining(training)
        }
        if (actualIds.size != response.size) {
            throw AppError.Network.IncompleteResponse()
        }

        if (actualIds.isEmpty()) {
            trainingDao.deleteByCreatedAtRange(startUtc, endUtc)
        } else {
            trainingDao.deleteByCreatedAtRangeExceptIds(startUtc, endUtc, actualIds)
        }
        if (savedCount != response.size) {
            throw AppError.Network.IncompleteResponse()
        }
    }

    override suspend fun updateTraining(id: String, training: SetTraining): String {
        api.updateTraining(
            id = id,
            body = training.toBody(),
        )

        val updated = api.getTraining(id)
        if (updated.id != id || !provideTraining(updated)) {
            throw AppError.Network.IncompleteResponse()
        }

        return id
    }

    override suspend fun setTraining(training: SetTraining): String {
        val response = api.setTraining(
            body = training.toBody(),
        )
        val id = response.id ?: throw AppError.Network.IncompleteResponse()

        val saved = api.getTraining(id)
        if (saved.id != id || !provideTraining(saved)) {
            throw AppError.Network.IncompleteResponse()
        }

        return id
    }

    override suspend fun deleteTraining(id: String): Unit {
        api.deleteTraining(id = id)
        trainingDao.deleteById(id)
    }

    private suspend fun provideTraining(value: TrainingResponse): Boolean {
        val training = value.toEntityOrNull() ?: return false
        val rawExercises = value.exercises ?: return false
        val exercises = rawExercises.toEntities()
        val rawIterations = rawExercises.flatMap { it.iterations ?: return false }
        val iterations = rawIterations.toEntities()
        if (exercises.size != rawExercises.size || iterations.size != rawIterations.size ||
            exercises.any { it.trainingId != training.id } ||
            rawExercises.any { exercise ->
                exercise.iterations.orEmpty().any { it.exerciseId != exercise.id }
            } ||
            exercises.map { it.id }.toSet().size != exercises.size ||
            iterations.map { it.id }.toSet().size != iterations.size
        ) {
            return false
        }
        DateTimeUtils.toLocalDateTime(training.createdAt)
        exercises.forEach {
            DateTimeUtils.toLocalDateTime(it.createdAt)
        }
        trainingDao.insertOrReplace(training, exercises, iterations)
        return true
    }

    override fun getDraftTraining(): Flow<DraftTraining?> {
        return draftTrainingDao.get()
            .map { it?.toDraftDomain() }
    }

    override suspend fun setDraftTraining(draft: DraftTraining): Unit {
        val profileId = userActiveDao.get()
            .firstOrNull()
            ?.let { userDao.getById(it).firstOrNull()?.profileId }
            ?: throw AppError.Expected(
                message = "An active profile is required to save a training draft",
                description = null,
            )

        val draftId = Uuid.random().toString()
        val draftExercises = draft.exercises.map { exercise ->
            val exerciseId = Uuid.random().toString()
            exercise.toEntity(
                id = exerciseId,
                trainingId = draftId,
                iterations = exercise.iterations.map { iteration ->
                    iteration.toEntity(id = Uuid.random().toString(), exerciseId = exerciseId)
                },
            )
        }
        val pack = draft.toEntity(id = draftId, profileId = profileId, exercises = draftExercises)

        val training = pack.training
        val exercises = pack.exercises.map { it.exercise }
        val iterations = pack.exercises.flatMap { it.iterations }

        draftTrainingDao.insertOrReplace(training, exercises, iterations)
    }

    override suspend fun deleteDraftTraining(): Unit {
        draftTrainingDao.delete()
    }
}
