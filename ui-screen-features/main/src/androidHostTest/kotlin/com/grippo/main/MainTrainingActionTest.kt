package com.grippo.main

import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.stage.StageState
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.GoalSetupSuggestionUseCase
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.DraftTraining
import com.grippo.data.features.api.training.models.Exercise
import com.grippo.data.features.api.training.models.SetTraining
import com.grippo.data.features.api.training.models.Training
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class MainTrainingActionTest {
    private val training = StubTrainingFeature()
    private val settings = StubSettingsFeature()
    private var dialog: DialogConfig? = null
    private var viewModel: MainViewModel? = null

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        startKoin { modules(CoreModule().module) }
    }

    @AfterTest
    fun teardown() {
        viewModel?.onDestroy()
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel = MainViewModel(
        trainingFeature = training,
        goalSetupSuggestionUseCase = GoalSetupSuggestionUseCase(StubGoalFeature(), settings),
        dialogController = object : DialogController {
            override fun open(config: DialogConfig) { dialog = config }
        },
    ).also { viewModel = it }

    @Test
    fun newWorkoutStartsWhenGoalSuggestionIsSnoozed() = runTest {
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        assertNull(dialog)
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())
        assertTrue(vm.loaders.value.isEmpty())
    }

    @Test
    fun draftCanBeContinuedWithoutSuggestingAGoal() = runTest {
        settings.lastShown = null
        training.draft = flowOf(DraftTraining(null, Duration.ZERO, emptyList()))
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        assertIs<DialogConfig.DraftTraining>(dialog).onContinue()
        assertEquals(MainDirection.Training(StageState.Draft), vm.navigator.first())
        assertEquals(0, settings.markCount)
    }

    @Test
    fun startingNewAfterDiscardingDraftUsesTheNewWorkoutFlow() = runTest {
        training.draft = flowOf(DraftTraining(null, Duration.ZERO, emptyList()))
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        val draftDialog = assertIs<DialogConfig.DraftTraining>(dialog)
        training.draft = flowOf(null) // The existing draft dialog deletes it before invoking this callback.
        draftDialog.onStartNew()
        advanceUntilIdle()
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())
    }

    @Test
    fun goalSuggestionCanStartWorkoutLaterOrOpenGoalConfiguration() = runTest {
        settings.lastShown = null
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        val suggestion = assertIs<DialogConfig.GoalSetupSuggestion>(dialog)
        assertEquals(1, settings.markCount)
        suggestion.onConfigure()
        assertEquals(MainDirection.Goal, vm.navigator.first())
        suggestion.onLater()
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())
    }

    @Test
    fun draftSavedAfterReturningToTabsIsReadOnTheNextTap() = runTest {
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())

        training.draft = flowOf(DraftTraining("existing-training", Duration.ZERO, emptyList()))
        vm.onStartTraining()
        advanceUntilIdle()
        assertIs<DialogConfig.DraftTraining>(dialog).onContinue()
        assertEquals(MainDirection.Training(StageState.Draft), vm.navigator.first())
        assertEquals(2, training.reads)
    }

    @Test
    fun discardingDraftStillSuggestsGoalBeforeStartingNewWorkout() = runTest {
        settings.lastShown = null
        training.draft = flowOf(DraftTraining(null, Duration.ZERO, emptyList()))
        val vm = createViewModel()
        vm.onStartTraining()
        advanceUntilIdle()
        val draftDialog = assertIs<DialogConfig.DraftTraining>(dialog)
        assertEquals(0, settings.markCount)

        training.draft = flowOf(null)
        draftDialog.onStartNew()
        advanceUntilIdle()
        val suggestion = assertIs<DialogConfig.GoalSetupSuggestion>(dialog)
        assertEquals(1, settings.markCount)
        suggestion.onLater()
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())
    }

    @Test
    fun loadingIsActiveWhileDraftCheckIsPending() = runTest {
        val draftFlow = MutableSharedFlow<DraftTraining?>()
        training.draft = draftFlow
        val vm = createViewModel()
        vm.onStartTraining()
        runCurrent()
        assertEquals(1, training.reads)
        assertTrue(MainLoader.StartTraining in vm.loaders.value)
        draftFlow.emit(null)
        advanceUntilIdle()
        assertEquals(MainDirection.Training(StageState.Add), vm.navigator.first())
        assertTrue(vm.loaders.value.isEmpty())
    }

    private class StubTrainingFeature : TrainingFeature {
        var draft: Flow<DraftTraining?> = flowOf(null)
        var reads = 0
        override fun getDraftTraining(): Flow<DraftTraining?> { reads++; return draft }
        override fun observeTrainings(start: LocalDateTime, end: LocalDateTime): Flow<List<Training>> = error("Unexpected call")
        override fun observeTraining(id: String): Flow<Training?> = error("Unexpected call")
        override fun observeExercise(id: String): Flow<Exercise?> = error("Unexpected call")
        override suspend fun getTrainings(start: LocalDateTime, end: LocalDateTime): Result<Unit> = error("Unexpected call")
        override suspend fun setTraining(training: SetTraining): Result<String?> = error("Unexpected call")
        override suspend fun updateTraining(id: String, training: SetTraining): Result<String?> = error("Unexpected call")
        override suspend fun deleteTraining(id: String): Result<Unit> = error("Unexpected call")
        override suspend fun setDraftTraining(draft: DraftTraining): Result<Unit> = error("Unexpected call")
        override suspend fun deleteDraftTraining(): Result<Unit> = error("Unexpected call")
    }

    private class StubGoalFeature : GoalFeature {
        override fun observeGoal(): Flow<Goal?> = flowOf(null)
        override suspend fun getGoal(): Result<Boolean> = error("Unexpected call")
        override suspend fun setGoal(goal: SetGoal): Result<Boolean> = error("Unexpected call")
    }

    private class StubSettingsFeature : LocalSettingsFeature {
        var lastShown: LocalDateTime? = DateTimeUtils.now()
        var markCount = 0
        override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> = flowOf(lastShown)
        override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> {
            lastShown = value
            markCount++
            return Result.success(Unit)
        }
        override fun observeRange(): Flow<Range?> = error("Unexpected call")
        override suspend fun setRange(range: Range?): Result<Unit> = error("Unexpected call")
        override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> = error("Unexpected call")
        override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> = error("Unexpected call")
        override suspend fun clear(): Result<Unit> = error("Unexpected call")
    }
}
