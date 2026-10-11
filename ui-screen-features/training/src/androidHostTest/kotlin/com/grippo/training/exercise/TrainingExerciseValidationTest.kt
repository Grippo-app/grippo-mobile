package com.grippo.training.exercise

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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingExerciseValidationTest {
    private var lifecycle: LifecycleRegistry? = null
    private var viewModel: TrainingExerciseViewModel? = null

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
        remote: suspend () -> Result<List<Exercise>> = { Result.success(emptyList()) },
    ): TrainingExerciseViewModel {
        val example = ExerciseExample(source.exerciseExample.toDomain(), ExerciseExampleComponents.External(true), emptyList(), emptyList())
        val examples = FakeExamples(example)
        val weights = FakeWeights()
        val validator = ExerciseValidatorUseCase(FakeTrainings(), FakeMetrics(remote), examples, weights)
        startKoin { modules(CoreModule().module) }
        val vm = TrainingExerciseViewModel(
            source, examples, object : DialogController {
                override fun open(config: DialogConfig) = error("No dialogs expected")
            }, weights, validator,
        )
        val registry = LifecycleRegistry()
        lifecycle = registry
        viewModel = vm
        TestComponent(DefaultComponentContext(registry), vm)
        registry.resume()
        return vm
    }

    private class TestComponent(
        context: ComponentContext,
        override val viewModel: TrainingExerciseViewModel,
    ) : BaseComponent<TrainingExerciseDirection>(context) {
        override suspend fun eventListener(direction: TrainingExerciseDirection) = Unit
        @Composable override fun Render() = Unit
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
