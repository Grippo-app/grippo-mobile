package com.grippo.training

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.exercises.ExercisesRootViewModel
import com.grippo.training.exercise.TrainingExerciseViewModel
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import androidx.compose.runtime.Composable
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.Result as ComponentResult
import com.grippo.core.foundation.models.ResultKeys
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.stage.StageState
import com.grippo.training.recording.TrainingRecordingViewModel
import com.grippo.data.features.api.metrics.volume.TrainingTotalUseCase
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.toolkit.local.notification.NotificationManager
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.PluralStringResource
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.core.state.trainings.stubExercise
import com.grippo.data.features.api.achievements.Achievement
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.*
import com.grippo.data.features.api.exercise.metrics.ExerciseMetricsFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.*
import com.grippo.data.features.api.training.ExerciseValidatorUseCase
import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.api.training.models.*
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.*
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import com.grippo.state.domain.example.toDomain
import com.grippo.training.exercise.TrainingExerciseComponent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingLibraryNavigationTest {
    private val source = stubExercise()
    private val selected = stubExerciseExample().let {
        it.copy(value = it.value.copy(id = "replacement", name = "Replacement"))
    }
    private val lifecycles = mutableListOf<LifecycleRegistry>()
    private var closed = false
    private val backDispatchers = mutableListOf<BackDispatcher>()

    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        val example = ExerciseExample(source.exerciseExample.toDomain(), ExerciseExampleComponents.External(true), emptyList(), emptyList())
        val examples = FakeExamples(example)
        val weights = FakeWeights()
        val validator = ExerciseValidatorUseCase(FakeTrainings(), FakeMetrics { Result.success(emptyList()) }, examples, weights)
        val muscles = Muscles()
        val useCase = UserExerciseExamplesUseCase(ExcludedMuscles(), ExcludedEquipments(), examples, Users())
        startKoin { modules(CoreModule().module, module {
            single<TrainingFeature> { FakeTrainings() }
            single<TrainingTotalUseCase> { TrainingTotalUseCase() }
            single<StringProvider> { object : StringProvider {
                override suspend fun get(id: StringResource, vararg args: Any) = "test"
                override suspend fun plural(id: PluralStringResource, quantity: Int, vararg args: Any) = "test"
            } }
            single<NotificationManager> { object : NotificationManager {
                override fun show(notification: AppNotification, delay: Duration) = notification.id
                override fun cancel(id: NotificationKey) = Unit
                override suspend fun isPending(id: NotificationKey) = false
            } }
            single<ExerciseExampleFeature> { examples }
            single<WeightHistoryFeature> { weights }
            single<ExerciseValidatorUseCase> { validator }
            single<MuscleFeature> { muscles }
            single<UserExerciseExamplesUseCase> { useCase }
            single<DialogController> { object : DialogController {
                override fun open(config: DialogConfig) = error("No dialogs expected")
            } }
        }) }
    }

    @AfterTest fun teardown() {
        lifecycles.forEach { it.destroy() }
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun create(
        keeper: StateKeeperDispatcher = StateKeeperDispatcher(),
        initial: TrainingRouter = TrainingRouter.Exercise(source, TrainingRecordingViewModel.EXERCISE_RESULT_KEY),
    ): TrainingComponent {
        val lifecycle = LifecycleRegistry()
        lifecycles += lifecycle
        val backDispatcher = BackDispatcher()
        backDispatchers += backDispatcher
        return TrainingComponent(
            DefaultComponentContext(lifecycle, stateKeeper = keeper, backHandler = backDispatcher),
            initial,
            close = { closed = true },
        ).also { lifecycle.resume() }
    }

    private fun selectFromLibrary(component: TrainingComponent): () -> Unit {
        val library = assertIs<TrainingComponent.Child.Exercises>(component.childStack.value.active.instance).component
        val route = assertIs<TrainingRouter.Exercises>(component.childStack.value.active.configuration)
        val model = library.retainedInstance<ExercisesRootViewModel> { error("Existing exercises VM expected") }
        val lifecycle = LifecycleRegistry()
        val sender = object : BaseComponent<TrainingDirection>(DefaultComponentContext(lifecycle)) {
            override val viewModel = TrainingViewModel()
            override suspend fun eventListener(direction: TrainingDirection) {
                sendResult(route.value.resultKey, selected)
                model.onBack()
            }
            @Composable override fun Render() = Unit
            fun select() = viewModel.toExercise(source, TrainingRecordingViewModel.EXERCISE_RESULT_KEY)
        }
        library.lifecycle.doOnDestroy { lifecycle.destroy() }
        lifecycle.resume()
        sender.select()
        return { sender.select() }
    }

    private fun exercise(component: TrainingComponent): TrainingExerciseComponent =
        assertIs<TrainingComponent.Child.Exercise>(component.childStack.value.active.instance).component

    @Test fun addingFromRecordingClosesLibraryBeforeOpeningExercise() = runTest {
        val component = create(initial = TrainingRouter.Recording(StageState.Draft))
        advanceUntilIdle()
        val recording = assertIs<TrainingComponent.Child.Recording>(component.childStack.value.active.instance).component
        val model = recording.retainedInstance<TrainingRecordingViewModel> { error("Existing recording VM expected") }
        model.onAddExercise()
        advanceUntilIdle()
        assertIs<TrainingComponent.Child.Exercises>(component.childStack.value.active.instance)
        val route = assertIs<TrainingRouter.Exercises>(component.childStack.value.active.configuration)
        assertEquals("exercise-library-add", route.value.resultKey.key)
        selectFromLibrary(component)
        advanceUntilIdle()
        val exerciseModel = exercise(component).retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        assertEquals(selected.value, exerciseModel.state.value.exercise.exerciseExample)
        assertTrue(exerciseModel.state.value.exercise.iterations.isEmpty())
        assertEquals(1, component.childStack.value.backStack.size)
        assertSame(recording, component.childStack.value.backStack.single().instance.component)
        assertTrue(model.state.value.exercises.isEmpty())
        assertFalse(closed)
    }

    @Test fun addingWithImmediateDispatchReturnsToNewEditor() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val component = create(initial = TrainingRouter.Recording(StageState.Draft))
        advanceUntilIdle()
        val recording = assertIs<TrainingComponent.Child.Recording>(component.childStack.value.active.instance).component
        val model = recording.retainedInstance<TrainingRecordingViewModel> { error("Existing recording VM expected") }
        model.onAddExercise()
        advanceUntilIdle()
        assertIs<TrainingComponent.Child.Exercises>(component.childStack.value.active.instance)
        val route = assertIs<TrainingRouter.Exercises>(component.childStack.value.active.configuration)
        assertEquals("exercise-library-add", route.value.resultKey.key)
        selectFromLibrary(component)
        advanceUntilIdle()
        val exerciseModel = exercise(component).retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        assertEquals(selected.value, exerciseModel.state.value.exercise.exerciseExample)
        assertTrue(exerciseModel.state.value.exercise.iterations.isEmpty())
        assertEquals(1, component.childStack.value.backStack.size)
        assertSame(recording, component.childStack.value.backStack.single().instance.component)
        assertTrue(model.state.value.exercises.isEmpty())
        assertFalse(closed)
    }

    @Test fun selectionReachesExplicitReceiverAndLibraryRequestsBack() = runTest {
        val component = create()
        advanceUntilIdle()
        val original = exercise(component)
        val exerciseModel = original.retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        val received = mutableListOf<ExerciseExampleState>()
        val lifecycle = LifecycleRegistry()
        lifecycles += lifecycle
        object : BaseComponent<TrainingDirection>(DefaultComponentContext(lifecycle)) {
            override val viewModel = TrainingViewModel()
            init {
                observeResult<ComponentResult<ExerciseExampleState>>(
                    ResultKeys.create("custom-receiver"),
                ) {
                    received += it.data
                }
            }
            override suspend fun eventListener(direction: TrainingDirection) = Unit
            @Composable override fun Render() = Unit
        }
        lifecycle.resume()
        trainingModel.toLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.Default(),
                resultKey = ResultKeys.create("custom-receiver"),
            )
        )
        advanceUntilIdle()
        selectFromLibrary(component)
        advanceUntilIdle()
        assertEquals(listOf(selected), received)
        assertSame(original, exercise(component))
        assertTrue(component.childStack.value.backStack.isEmpty())
        assertEquals(source, exerciseModel.state.value.exercise)
        assertFalse(closed)
    }

    @Test fun replacingDoesNotOpenAnotherExerciseOrLoseRecordingUpdates() = runTest {
        val component = create(initial = TrainingRouter.Recording(StageState.Draft))
        advanceUntilIdle()
        val recording = assertIs<TrainingComponent.Child.Recording>(component.childStack.value.active.instance).component
        val recordingModel = recording.retainedInstance<TrainingRecordingViewModel> { error("Existing recording VM expected") }
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        trainingModel.toExercise(source, TrainingRecordingViewModel.EXERCISE_RESULT_KEY)
        advanceUntilIdle()
        val original = exercise(component)
        trainingModel.toLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
                resultKey = ResultKeys.create("exercise-library:${source.id}"),
            )
        )
        advanceUntilIdle()
        selectFromLibrary(component)
        advanceUntilIdle()
        assertSame(original, exercise(component))
        assertEquals(1, component.childStack.value.backStack.size)
        val updated = recordingModel.state.value.exercises.single()
        assertEquals(source.id, updated.id)
        assertEquals(source.iterations, updated.iterations)
        assertEquals(selected.value, updated.exerciseExample)
        assertFalse(closed)
    }

    @Test fun replacementReturnsToSameExerciseAndPreservesItsState() = runTest {
        val component = create()
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        advanceUntilIdle()
        val original = exercise(component)
        val exerciseModel = original.retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        trainingModel.toLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
                resultKey = ResultKeys.create("exercise-library:${source.id}"),
            )
        )
        advanceUntilIdle()
        assertEquals(1, component.childStack.value.backStack.size)
        assertIs<TrainingComponent.Child.Exercises>(component.childStack.value.active.instance)
        val selectAgain = selectFromLibrary(component)
        advanceUntilIdle()
        assertSame(original, exercise(component))
        assertEquals(selected.value, exerciseModel.state.value.exercise.exerciseExample)
        assertEquals(source.id, exerciseModel.state.value.exercise.id)
        assertEquals(source.iterations, exerciseModel.state.value.exercise.iterations)
        assertTrue(component.childStack.value.backStack.isEmpty())
        assertFalse(closed)
        selectAgain()
        advanceUntilIdle()
        assertSame(original, exercise(component))
        assertTrue(component.childStack.value.backStack.isEmpty())
    }

    @Test fun exerciseReturnsUpdatesSaveAndDeleteToCallerKey() = runTest {
        val component = create(initial = TrainingRouter.Exercise(source, ResultKeys.create("custom-exercise-receiver")))
        val received = mutableListOf<TrainingRouter.Exercise.Action>()
        val defaultReceived = mutableListOf<TrainingRouter.Exercise.Action>()
        val lifecycle = LifecycleRegistry()
        lifecycles += lifecycle
        object : BaseComponent<TrainingDirection>(DefaultComponentContext(lifecycle)) {
            override val viewModel = TrainingViewModel()
            init {
                observeResult<ComponentResult<TrainingRouter.Exercise.Action>>(
                    ResultKeys.create("custom-exercise-receiver"),
                ) { received += it.data }
                observeResult<ComponentResult<TrainingRouter.Exercise.Action>>(
                    TrainingRecordingViewModel.EXERCISE_RESULT_KEY,
                ) { defaultReceived += it.data }
            }
            override suspend fun eventListener(direction: TrainingDirection) = Unit
            @Composable override fun Render() = Unit
        }
        lifecycle.resume()
        advanceUntilIdle()
        received.clear()
        val model = exercise(component).retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        model.swapExerciseExample(selected)
        advanceUntilIdle()
        assertEquals(selected.value, assertIs<TrainingRouter.Exercise.Action.Sync>(received.last()).exercise.exerciseExample)
        val updateCount = received.size
        model.onSave()
        advanceUntilIdle()
        assertEquals(updateCount + 1, received.size)
        assertEquals(model.state.value.exercise, assertIs<TrainingRouter.Exercise.Action.Sync>(received.last()).exercise)
        model.state.value.exercise.iterations.forEach { model.onDeleteIteration(it.id) }
        advanceUntilIdle()
        assertEquals(source.id, assertIs<TrainingRouter.Exercise.Action.Remove>(received.last()).id)
        assertTrue(defaultReceived.isEmpty())
    }

    @Test fun cancellingLibraryReturnsToRetainedExerciseWithoutChangingIt() = runTest {
        val component = create()
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        advanceUntilIdle()
        val original = exercise(component)
        val exerciseModel = original.retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        trainingModel.toLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
                resultKey = ResultKeys.create("exercise-library:${source.id}"),
            )
        )
        advanceUntilIdle()
        val library = assertIs<TrainingComponent.Child.Exercises>(component.childStack.value.active.instance).component
        val libraryModel = library.retainedInstance<ExercisesRootViewModel> { error("Existing exercises VM expected") }
        libraryModel.onBack()
        advanceUntilIdle()
        assertSame(original, exercise(component))
        assertEquals(source, exerciseModel.state.value.exercise)
        assertTrue(component.childStack.value.backStack.isEmpty())
        assertFalse(closed)
    }

    @Test fun hardwareBackCancelsLibraryAndReturnsToExercise() = runTest {
        val component = create()
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        advanceUntilIdle()
        val original = exercise(component)
        val exerciseModel = original.retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        trainingModel.toLibrary(
            ExercisesRouter.Library(
                mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
                resultKey = ResultKeys.create("exercise-library:${source.id}"),
            )
        )
        advanceUntilIdle()
        backDispatchers.last().back()
        advanceUntilIdle()
        assertSame(original, exercise(component))
        assertEquals(source, exerciseModel.state.value.exercise)
        assertFalse(closed)
    }

    @Test fun restoredLibraryDeliversReplacementToRestoredExercise() = runTest {
        val keeper = StateKeeperDispatcher()
        val component = create(keeper)
        val trainingModel = component.retainedInstance<TrainingViewModel> { error("Existing training VM expected") }
        advanceUntilIdle()
        val route = ExercisesRouter.Library(
            mode = ExercisesRouter.Mode.SimilarTo(source.exerciseExample.id),
            resultKey = ResultKeys.create("exercise-library:${source.id}"),
        )
        trainingModel.toLibrary(route)
        advanceUntilIdle()
        val saved = keeper.save()
        lifecycles.last().destroy()
        lifecycles.removeAt(lifecycles.lastIndex)
        val restored = create(StateKeeperDispatcher(saved))
        advanceUntilIdle()
        assertEquals(TrainingRouter.Exercises(route), restored.childStack.value.active.configuration)
        assertEquals(listOf(TrainingRouter.Exercise(source, TrainingRecordingViewModel.EXERCISE_RESULT_KEY)), restored.childStack.value.backStack.map { it.configuration })
        assertIs<TrainingComponent.Child.Exercises>(restored.childStack.value.active.instance)
        selectFromLibrary(restored)
        advanceUntilIdle()
        val exerciseModel = exercise(restored).retainedInstance<TrainingExerciseViewModel> { error("Existing exercise VM expected") }
        assertEquals(selected.value, exerciseModel.state.value.exercise.exerciseExample)
        assertTrue(restored.childStack.value.backStack.isEmpty())
        assertFalse(closed)
    }
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
        override fun getDraftTraining(): Flow<DraftTraining?> = flowOf(null)
        override suspend fun setDraftTraining(draft: DraftTraining): Result<Unit> = Result.success(Unit)
        override suspend fun deleteDraftTraining(): Result<Unit> = error("unused")
    }

    private class FakeWeights : WeightHistoryFeature {
        override fun observeWeightHistory(): Flow<List<WeightHistory>> = flowOf(emptyList())
        override fun observeLastWeight(): Flow<WeightHistory?> = error("unused")
        override suspend fun getWeightHistory(): Result<Unit> = error("unused")
        override suspend fun updateWeight(value: Float): Result<Unit> = error("unused")
        override suspend fun deleteWeight(id: String): Result<Unit> = error("unused")
    }

    private inner class FakeExamples(private val example: ExerciseExample) : ExerciseExampleFeature {
        override fun observeExerciseExample(id: String): Flow<ExerciseExample?> = flowOf(
            if (id == selected.value.id) example.copy(value = selected.value.toDomain()) else example
        )
        override fun observeExerciseExamples(name: String?, scope: ExampleScope, sorting: ExampleSortingEnum,
            rules: UserExerciseExampleRules, page: ExamplePage, experience: ExperienceEnum): Flow<List<ExerciseExample>> = flowOf(listOf(example))
        override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> = flowOf(listOf(example))
        override suspend fun getExerciseExamples(): Result<Unit> = error("unused")
        override suspend fun getExerciseExampleById(id: String): Result<Unit> = error("unused")
    }
    private class Muscles : MuscleFeature {
        override fun observeMuscles(): Flow<List<MuscleGroup>> = flowOf(
            listOf(MuscleGroup("chest", "Chest", emptyList(), MuscleGroupEnum.CHEST_MUSCLES))
        )
        override suspend fun getMuscles(): Result<Unit> = error("unused")
    }
    private class ExcludedMuscles : ExcludedMusclesFeature {
        override fun observeExcludedMuscles(): Flow<List<Muscle>> = flowOf(emptyList())
        override suspend fun getExcludedMuscles(): Result<Unit> = error("unused")
        override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> = error("unused")
    }
    private class ExcludedEquipments : ExcludedEquipmentsFeature {
        override fun observeExcludedEquipments(): Flow<List<Equipment>> = flowOf(emptyList())
        override suspend fun getExcludedEquipments(): Result<Unit> = error("unused")
        override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> = error("unused")
    }
    private class Users : UserFeature {
        override fun observeUser(): Flow<User?> = flowOf(User("user", "User", "user@test.invalid",
            ExperienceEnum.ADVANCED, 80f, 180, LocalDateTime(2026, 1, 1, 0, 0), RoleEnum.DEFAULT,
            UserStats(0, Duration.ZERO, 0f, 0)))
        override suspend fun getUser(): Result<Boolean> = error("unused")
        override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> = error("unused")
        override suspend fun deleteProfile(): Result<Unit> = error("unused")
        override suspend fun setExperience(experience: ExperienceEnum): Result<Boolean> = error("unused")
        override suspend fun setHeight(height: Int): Result<Boolean> = error("unused")
    }
}
