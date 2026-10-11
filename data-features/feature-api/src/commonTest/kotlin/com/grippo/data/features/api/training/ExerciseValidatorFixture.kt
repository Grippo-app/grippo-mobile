package com.grippo.data.features.api.training

import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.*
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.training.models.*
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDateTime

internal class ExerciseValidatorFixture(
    private val example: ExerciseExample,
    remote: suspend () -> Result<List<Exercise>> = { Result.success(emptyList()) },
) {
    val trainings = FakeTrainings()
    val metrics = FakeMetrics(remote)
    val weights = FakeWeights()
    val examples = FakeExamples()
    val loader = ExerciseValidatorUseCase(trainings, metrics, examples, weights)

    internal class FakeMetrics(private val remote: suspend () -> Result<List<Exercise>>) : ExerciseMetricsFeature {
        var calls = 0
        override suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>> { calls++; return remote() }
        override suspend fun getAchievementsByExampleId(id: String): Result<List<Achievement>> = error("unused")
    }

    internal class FakeTrainings : TrainingFeature {
        val values = MutableStateFlow<List<Training>>(emptyList())
        var failure: Exception? = null
        var requestedStart: LocalDateTime? = null
        var requestedEnd: LocalDateTime? = null
        override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> {
            requestedStart = start
            requestedEnd = end
            return failure?.let { error -> flow { throw error } } ?: values
        }
        override fun observeTraining(id: String): Flow<Training?> = error("unused")
        override fun observeExercise(id: String): Flow<Exercise?> = error("unused")
        override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> = error("unused")
        override suspend fun setTraining(training: SetTraining): Result<String?> = error("unused")
        override suspend fun updateTraining(id: String, training: SetTraining): Result<String?> = error("unused")
        override suspend fun deleteTraining(id: String): Result<Unit> = error("unused")
        override fun getDraftTraining(): Flow<DraftTraining?> = error("unused")
        override suspend fun setDraftTraining(draft: DraftTraining): Result<Unit> = error("unused")
        override suspend fun deleteDraftTraining(): Result<Unit> = error("unused")
    }

    internal class FakeWeights : WeightHistoryFeature {
        val values = MutableStateFlow<List<WeightHistory>>(emptyList())
        var failure: Exception? = null
        override fun observeWeightHistory(): Flow<List<WeightHistory>> =
            failure?.let { error -> flow { throw error } } ?: values
        override fun observeLastWeight(): Flow<WeightHistory?> = error("unused")
        override suspend fun getWeightHistory(): Result<Unit> = error("unused")
        override suspend fun updateWeight(value: Float): Result<Unit> = error("unused")
        override suspend fun deleteWeight(id: String): Result<Unit> = error("unused")
    }

    internal inner class FakeExamples : ExerciseExampleFeature {
        var value: ExerciseExample? = example
        var failure: Exception? = null
        override fun observeExerciseExample(id: String): Flow<ExerciseExample?> =
            failure?.let { error -> flow { throw error } } ?: flowOf(value)
        override fun observeExerciseExamples(name: String?, scope: ExampleScope, sorting: ExampleSortingEnum,
            rules: UserExerciseExampleRules, page: ExamplePage, experience: ExperienceEnum): Flow<List<ExerciseExample>> = error("unused")
        override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> = error("unused")
        override suspend fun getExerciseExamples(): Result<Unit> = error("unused")
        override suspend fun getExerciseExampleById(id: String): Result<Unit> = error("unused")
    }
}
