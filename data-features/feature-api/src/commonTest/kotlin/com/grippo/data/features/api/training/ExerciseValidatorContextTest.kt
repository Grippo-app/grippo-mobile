package com.grippo.data.features.api.training

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.WeightTypeEnum
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.ExerciseArtifacts
import com.grippo.data.features.api.training.models.ExerciseValidationContext
import com.grippo.data.features.api.training.models.ExerciseValidationInput
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.SetIteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class ExerciseValidatorContextTest {
    private val date = LocalDateTime(2026, 10, 10, 12, 0)
    private val exampleValue = ExerciseExampleValue(
        "bench", "Bench", "", null, ExperienceEnum.INTERMEDIATE,
        ForceTypeEnum.PUSH, WeightTypeEnum.FREE, CategoryEnum.COMPOUND, 0, null,
    )
    private val example = ExerciseExample(
        exampleValue,
        ExerciseExampleComponents.External(true),
        emptyList(),
        emptyList()
    )

    @Test
    fun observedContextIsUsedByTheSameValidator() = runTest {
        val fixture = createFixture()
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        val input = ExerciseValidationInput(
            "Bench", "bench", listOf(
                SetIteration(null, null, null, null, null, 5),
            ), date
        )
        val artifacts = fixture.loader.execute(input, contexts.last())
        assertTrue(artifacts?.iterations?.single()?.artifacts?.any {
            it.field == ExerciseArtifacts.Field.ExternalWeight &&
                    it.reason == ExerciseArtifacts.Reason.MissingRequired
        } == true)
    }

    @Test
    fun validationWaitsForRemoteEvidence() = runTest {
        val gate = CompletableDeferred<Result<List<Exercise>>>()
        val fixture = createFixture { gate.await() }
        fixture.trainings.values.value = listOf(training(exercise("local")))
        val contexts = mutableListOf<ExerciseValidationContext>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, "editing").collect(contexts::add)
        }
        runCurrent()
        assertTrue(contexts.isEmpty())
        assertFalse(gate.isCompleted)
        gate.complete(Result.success(listOf(exercise("remote"))))
        runCurrent()
        assertEquals(setOf("local", "remote"), contexts.last().history.map { it.id }.toSet())
        job.cancel()
    }

    @Test
    fun networkFailureIsPropagated() = runTest {
        val error = IllegalStateException("offline")
        val fixture = createFixture { Result.failure(error) }
        val actual = assertFailsWith<IllegalStateException> {
            fixture.loader.observeContext("bench", date, null).first()
        }
        assertEquals(error.message, actual.message)
    }

    @Test
    fun thrownNetworkFailureIsPropagated() = runTest {
        val error = IllegalStateException("transport failure")
        val fixture = createFixture { throw error }
        val actual = assertFailsWith<IllegalStateException> {
            fixture.loader.observeContext("bench", date, null).first()
        }
        assertEquals(error.message, actual.message)
    }

    @Test
    fun localUpdatesDoNotRefetchRemoteHistoryAndDuplicatesAreRemoved() = runTest {
        val fixture = createFixture { Result.success(listOf(exercise("shared"), exercise("remote"))) }
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        fixture.trainings.values.value = listOf(training(exercise("shared")))
        runCurrent()
        fixture.trainings.values.value = listOf(training(exercise("shared"), exercise("local")))
        runCurrent()
        assertEquals(1, fixture.metrics.calls)
        assertEquals(
            setOf("shared", "remote", "local"),
            contexts.last().history.map { it.id }.toSet()
        )
        assertEquals(3, contexts.last().history.size)
    }

    @Test
    fun bodyWeightComesFromExerciseDateRatherThanCurrentProfile() = runTest {
        val fixture = createFixture()
        fixture.weights.values.value = listOf(
            WeightHistory("old", 75f, LocalDateTime(2026, 9, 1, 12, 0)),
            WeightHistory("then", 80f, LocalDateTime(2026, 10, 9, 12, 0)),
            WeightHistory("future", 90f, LocalDateTime(2026, 10, 11, 12, 0)),
            WeightHistory("bad", Float.NaN, LocalDateTime(2026, 10, 10, 11, 0)),
        )
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        assertEquals(80f, contexts.last().bodyWeight)
    }

    @Test
    fun staleBodyWeightIsOmittedAndHistoryIsBoundedByExerciseDate() = runTest {
        val fixture = createFixture()
        fixture.weights.values.value =
            listOf(WeightHistory("stale", 80f, LocalDateTime(2026, 1, 1, 12, 0)))
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        assertEquals(null, contexts.last().bodyWeight)
        assertEquals(date, fixture.trainings.requestedEnd)
        assertEquals(LocalDateTime(2026, 4, 13, 0, 0), fixture.trainings.requestedStart)
    }

    @Test
    fun cancellationStopsPendingRemoteRequest() = runTest {
        val gate = CompletableDeferred<Result<List<Exercise>>>()
        var cancelled = false
        val fixture = createFixture {
            try {
                gate.await()
            } finally {
                cancelled = true
            }
        }
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect()
        }
        runCurrent()
        job.cancel()
        runCurrent()
        assertTrue(cancelled)
        assertTrue(job.isCancelled)
    }

    @Test
    fun freshRemoteRecordSupersedesItsCachedVersion() = runTest {
        val updated = exercise("same").copy(
            iterations = listOf(
                Iteration(
                    "set",
                    85f,
                    null,
                    null,
                    null,
                    null,
                    5
                )
            )
        )
        val fixture = createFixture { Result.success(listOf(updated)) }
        fixture.trainings.values.value = listOf(training(exercise("same")))
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        assertEquals(listOf(updated), contexts.last().history)
    }

    @Test
    fun missingCatalogIsAnExplicitError() = runTest {
        val fixture = createFixture()
        fixture.examples.value = null
        val error = assertFailsWith<IllegalArgumentException> {
            fixture.loader.observeContext("bench", date, null).first()
        }
        assertEquals("Exercise example bench is unavailable.", error.message)
    }

    @Test
    fun successfullyLoadedEmptyHistoryIsValid() = runTest {
        val fixture = createFixture()
        val context = fixture.loader.observeContext("bench", date, null).first()
        assertTrue(context.history.isEmpty())
        assertEquals(example.components, context.components)
        assertEquals(null, context.bodyWeight)
    }

    @Test
    fun eachLocalSourceFailureIsPropagated() = runTest {
        for (source in 0..2) {
            val fixture = createFixture()
            val error = IllegalStateException("source $source unavailable")
            when (source) {
                0 -> fixture.trainings.failure = error
                1 -> fixture.examples.failure = error
                2 -> fixture.weights.failure = error
            }
            val actual = assertFailsWith<IllegalStateException> {
                fixture.loader.observeContext("bench", date, "editing").first()
            }
            assertEquals(error.message, actual.message)
        }
    }

    @Test
    fun measurementRecordedLaterOnWorkoutDayCanConfirmBodyWeight() = runTest {
        val fixture = createFixture()
        fixture.weights.values.value = listOf(
            WeightHistory("old", 80f, LocalDateTime(2026, 10, 9, 12, 0)),
            WeightHistory("today", 120f, LocalDateTime(2026, 10, 10, 12, 30)),
        )
        val contexts = mutableListOf<ExerciseValidationContext>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.loader.observeContext("bench", date, null).collect(contexts::add)
        }
        runCurrent()
        assertEquals(120f, contexts.last().bodyWeight)
    }

    private fun exercise(id: String): Exercise =
        Exercise(id, "Bench", emptyList(), exampleValue, date, 0f, 0, 0f)

    private fun training(vararg exercises: Exercise): Training =
        Training("training", exercises.toList(), Duration.ZERO, date, 0f, 0, 0f)

    private fun createFixture(
        remote: suspend () -> Result<List<Exercise>> = { Result.success(emptyList()) },
    ): ExerciseValidatorFixture = ExerciseValidatorFixture(example, remote)
}
