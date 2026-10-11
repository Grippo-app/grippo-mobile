package com.grippo.training.exercise

import com.grippo.core.foundation.models.ResultKeys

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComponent
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.*
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.training.ExerciseValidatorUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.*
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.dialog.api.DialogConfig
import com.grippo.screen.api.ExercisesRouter
import com.grippo.dialog.api.DialogController
import com.grippo.state.domain.example.toDomain
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.ksp.generated.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingExerciseValidationTest {
    private var lifecycle: LifecycleRegistry? = null
    private var viewModel: TrainingExerciseViewModel? = null
    private val directions = mutableListOf<TrainingExerciseDirection>()

    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest fun teardown() {
        lifecycle?.destroy()
        viewModel?.onDestroy()
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        source: ExerciseState,
        onDialog: (DialogConfig) -> Unit = { error("No dialogs expected") },
        remote: suspend () -> Result<List<Exercise>> = { Result.success(emptyList()) },
    ): TrainingExerciseViewModel {
        val example = ExerciseExample(source.exerciseExample.toDomain(), ExerciseExampleComponents.External(true), emptyList(), emptyList())
        val examples = FakeExamples(example)
        val weights = FakeWeights()
        val validator = ExerciseValidatorUseCase(FakeTrainings(), FakeMetrics(remote), examples, weights)
        startKoin { modules(CoreModule().module) }
        val vm = TrainingExerciseViewModel(
            source, examples, object : DialogController {
                override fun open(config: DialogConfig) = onDialog(config)
            }, weights, validator,
        )
        val registry = LifecycleRegistry()
        lifecycle = registry
        viewModel = vm
        TestComponent(DefaultComponentContext(registry), vm)
        registry.resume()
        return vm
    }

    private inner class TestComponent(
        context: ComponentContext,
        override val viewModel: TrainingExerciseViewModel,
    ) : BaseComponent<TrainingExerciseDirection>(context) {
        override suspend fun eventListener(direction: TrainingExerciseDirection) { directions += direction }
        @Composable override fun Render() = Unit
    }

    @Test fun replacementClosesDetailsSessionAndOpensLibraryScreen() = runTest {
        val source = stubExercise()
        var opened: DialogConfig? = null
        val model = createViewModel(source, onDialog = { opened = it })
        advanceUntilIdle()
        model.onExampleClick()
        val details = assertIs<DialogConfig.ExerciseExample>(opened)
        val action = assertIs<DialogConfig.ExerciseExample.Mode.Action>(details.mode)
        var closed = false
        action.onClick(object : DialogController.Session {
            override fun close() { closed = true }
            override fun push(config: DialogConfig) = error("Screen expected")
            override fun replaceCurrent(config: DialogConfig) = error("Screen expected")
            override fun back() = error("Whole session must close")
        })
        assertTrue(closed)
        advanceUntilIdle()
        assertEquals(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
                resultKey = ResultKeys.create("exercise-library:${source.id}"),
            ),
            assertIs<TrainingExerciseDirection.ToLibrary>(directions.single()).route,
        )
    }

    @Test fun libraryReplacementPreservesExerciseIdentityAndRecordedSets() = runTest {
        val source = stubExercise().copy(iterations = listOf(row("recorded", 100f)).toPersistentList())
        val model = createViewModel(source)
        advanceUntilIdle()
        val selected = stubExerciseExample().let {
            it.copy(value = it.value.copy(id = "replacement", name = "Replacement"))
        }
        model.swapExerciseExample(selected)
        advanceUntilIdle()
        val updated = model.state.value.exercise
        assertEquals(source.id, updated.id)
        assertEquals(source.createdAt, updated.createdAt)
        assertEquals(source.iterations, updated.iterations)
        assertEquals(selected.value, updated.exerciseExample)
        assertEquals(selected.value.name, updated.name)
    }

    @Test fun skippedUnmappableRowDoesNotShiftHighlightToAnotherSet() = runTest {
        val source = stubExercise().copy(iterations = listOf(
            row("unmappable", 100f).copy(repetitions = RepetitionsFormatState.Empty()),
            row("first", 100f), row("second", 100f), row("typo", 1000f),
        ).toPersistentList())
        val vm = createViewModel(source)
        advanceUntilIdle()
        val result = vm.state.value
        assertEquals(setOf("typo"), result.volumeArtifactIds)
        assertTrue(result.repetitionArtifactIds.isEmpty())
        assertTrue(result.previousIterations.isEmpty())
        assertEquals(source, result.exercise)
    }

    @Test fun pendingRowsDoNotShiftHighlight() = runTest {
        val source = stubExercise().copy(iterations = listOf(
            stubPendingIteration().copy(id = "pending"),
            row("first", 100f), row("second", 100f), row("typo", 1000f),
        ).toPersistentList())
        val vm = createViewModel(source)
        advanceUntilIdle()
        assertEquals(setOf("typo"), vm.state.value.volumeArtifactIds)
    }

    @Test fun simultaneousFindingsHighlightBothFields() = runTest {
        val source = stubExercise().copy(iterations = listOf(
            row("first", 100f), row("second", 100f),
            row("typo", 1000f).copy(repetitions = RepetitionsFormatState.of(50)),
        ).toPersistentList())
        val vm = createViewModel(source)
        advanceUntilIdle()
        val result = vm.state.value
        assertEquals(setOf("typo"), result.volumeArtifactIds)
        assertEquals(setOf("typo"), result.repetitionArtifactIds)
    }

    @Test fun emptyExerciseHasNoHighlights() = runTest {
        val source = stubExercise().copy(iterations = emptyList<IterationState>().toPersistentList())
        val vm = createViewModel(source)
        advanceUntilIdle()
        val result = vm.state.value
        assertTrue(result.volumeArtifactIds.isEmpty())
        assertTrue(result.repetitionArtifactIds.isEmpty())
    }

    @Test fun missingAggregateTotalsDoNotDisableRawSetValidation() = runTest {
        val initial = stubExercise()
        val source = initial.copy(
            total = initial.total.copy(repetitions = RepetitionsFormatState.Empty(), volume = VolumeFormatState.Empty()),
            iterations = listOf(row("first", 100f), row("second", 100f), row("typo", 1000f)).toPersistentList(),
        )
        val vm = createViewModel(source)
        advanceUntilIdle()
        assertEquals(setOf("typo"), vm.state.value.volumeArtifactIds)
    }

    @Test fun unknownDateDoesNotFabricateContext() = runTest {
        val initial = stubExercise()
        val source = initial.copy(
            createdAt = DateTimeFormatState.Empty(initial.createdAt.format),
            iterations = listOf(row("first", 100f), row("second", 100f), row("typo", 1000f)).toPersistentList(),
        )
        val vm = createViewModel(source)
        advanceUntilIdle()
        assertTrue(vm.state.value.volumeArtifactIds.isEmpty())
    }

    @Test fun editingWhileHistoryLoadsCannotHighlightTheOldRow() = runTest {
        val source = stubExercise().copy(iterations = listOf(
            row("first", 100f), row("second", 100f), row("typo", 1000f),
        ).toPersistentList())
        val gate = CompletableDeferred<Result<List<Exercise>>>()
        val vm = createViewModel(source) { gate.await() }
        advanceUntilIdle()
        assertTrue(vm.state.value.volumeArtifactIds.isEmpty())
        vm.onDeleteIteration("typo")
        gate.complete(Result.success(emptyList()))
        advanceUntilIdle()
        assertTrue(vm.state.value.volumeArtifactIds.isEmpty())
        assertEquals(listOf("first", "second"), vm.state.value.exercise.iterations.map { it.id })
    }

    @Test fun correctingTheExerciseClearsPreviousHighlights() = runTest {
        val source = stubExercise().copy(iterations = listOf(
            row("first", 100f), row("second", 100f), row("typo", 1000f),
        ).toPersistentList())
        val vm = createViewModel(source)
        advanceUntilIdle()
        assertEquals(setOf("typo"), vm.state.value.volumeArtifactIds)
        vm.onDeleteIteration("typo")
        advanceUntilIdle()
        assertTrue(vm.state.value.volumeArtifactIds.isEmpty())
    }

    @Test fun previousSetsFollowOrdinalFromLatestEarlierWorkoutAndClearOnExampleChange() = runTest {
        val initial = stubExercise()
        val source = initial.copy(
            createdAt = DateTimeFormatState.Valid(
                display = "", value = LocalDateTime(2026, 10, 11, 12, 0), format = initial.createdAt.format,
            ),
            iterations = listOf(
                row("current-first", 40f), row("current-second", 60f),
                row("current-third", 60f), row("current-extra", 60f),
            ).toPersistentList(),
        )
        fun history(id: String, day: Int, weight: Float) = Exercise(
            id = id, name = source.name,
            iterations = listOf(Iteration(id = "$id-set", externalWeight = weight, assistWeight = null,
                extraWeight = null, bodyWeight = null, bodyMultiplier = null, repetitions = 10)),
            exerciseExample = source.exerciseExample.toDomain(),
            createdAt = LocalDateTime(2026, 10, day, 12, 0),
            volume = weight * 10, repetitions = 10, intensity = weight,
        )
        val vm = createViewModel(source) { Result.success(listOf(
            history("older", 8, 40f), history("previous", 10, 60f).copy(iterations = listOf(
                Iteration(id = "previous-first", externalWeight = 40f, assistWeight = null,
                    extraWeight = null, bodyWeight = null, bodyMultiplier = null, repetitions = 12),
                Iteration(id = "previous-second", externalWeight = 60f, assistWeight = null,
                    extraWeight = null, bodyWeight = null, bodyMultiplier = null, repetitions = 10),
                Iteration(id = "previous-third", externalWeight = 60f, assistWeight = null,
                    extraWeight = null, bodyWeight = null, bodyMultiplier = null, repetitions = 8),
            )),
            history("future", 12, 90f), history(source.id, 9, 100f),
            history("empty", 11, 80f).copy(iterations = emptyList(), createdAt = LocalDateTime(2026, 10, 11, 10, 0)),
            history("other", 11, 80f).copy(exerciseExample = source.exerciseExample.toDomain().copy(id = "other-example")),
        )) }
        advanceUntilIdle()
        val previous = vm.state.value.previousIterations
        assertEquals(listOf(40f, 60f, 60f), previous.map { it.externalWeight?.value })
        assertEquals(listOf(12, 10, 8), previous.map { it.repetitions.value })
        assertEquals(null, previous.getOrNull(3))
        val ids = source.iterations.map { it.id }
        vm.onStartReorderIterations(ids[0], ids[1])
        vm.onEndReorderIterations()
        advanceUntilIdle()
        assertEquals(listOf(ids[1], ids[0], ids[2], ids[3]), vm.state.value.exercise.iterations.map { it.id })
        assertEquals(previous, vm.state.value.previousIterations)
        val other = stubExerciseExample().let { it.copy(value = it.value.copy(id = "new-example")) }
        vm.swapExerciseExample(other)
        advanceUntilIdle()
        assertTrue(vm.state.value.previousIterations.isEmpty())
    }

    private fun row(id: String, weight: Float): IterationState = stubPendingIteration().copy(
        id = id,
        externalWeight = VolumeFormatState.of(weight),
        repetitions = RepetitionsFormatState.of(5),
    )

    private class FakeMetrics(private val remote: suspend () -> Result<List<Exercise>>) : ExerciseMetricsFeature {
        override suspend fun getRecentExercisesByExampleId(id: String): Result<List<Exercise>> = remote()
        override suspend fun getAchievementsByExampleId(id: String): Result<List<Achievement>> = error("unused")
    }

    private class FakeTrainings : TrainingFeature {
        override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> = flowOf(emptyList())
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

    private class FakeWeights : WeightHistoryFeature {
        override fun observeWeightHistory(): Flow<List<WeightHistory>> = flowOf(emptyList())
        override fun observeLastWeight(): Flow<WeightHistory?> = error("unused")
        override suspend fun getWeightHistory(): Result<Unit> = error("unused")
        override suspend fun updateWeight(value: Float): Result<Unit> = error("unused")
        override suspend fun deleteWeight(id: String): Result<Unit> = error("unused")
    }

    private class FakeExamples(private val example: ExerciseExample) : ExerciseExampleFeature {
        override fun observeExerciseExample(id: String): Flow<ExerciseExample?> = flowOf(example)
        override fun observeExerciseExamples(name: String?, scope: ExampleScope, sorting: ExampleSortingEnum,
            rules: UserExerciseExampleRules, page: ExamplePage, experience: ExperienceEnum): Flow<List<ExerciseExample>> = error("unused")
        override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> = error("unused")
        override suspend fun getExerciseExamples(): Result<Unit> = error("unused")
        override suspend fun getExerciseExampleById(id: String): Result<Unit> = error("unused")
    }
}
