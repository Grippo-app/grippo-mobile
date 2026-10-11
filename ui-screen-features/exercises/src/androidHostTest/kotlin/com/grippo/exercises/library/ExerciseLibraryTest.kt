package com.grippo.exercises.library

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.core.foundation.models.Result as ComponentResult
import com.grippo.core.foundation.models.ResultKeys
import com.grippo.exercises.ExercisesRootDirection
import com.grippo.exercises.ExercisesRootViewModel
import com.grippo.core.foundation.BaseComponent
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.state.examples.ExerciseExampleState
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.examples.stubExerciseExample
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.UserExerciseExamplesUseCase
import com.grippo.data.features.api.exercise.example.models.*
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.*
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.*
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.TrainingRouter
import com.grippo.state.domain.example.toDomain
import kotlin.test.*
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.ksp.generated.module

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseLibraryTest {
    private val lifecycle = LifecycleRegistry()
    private val directions = mutableListOf<ExerciseLibraryDirection>()
    private var vm: ExerciseLibraryViewModel? = null
    private val example = stubExerciseExample().let {
        ExerciseExample(it.value.toDomain(), ExerciseExampleComponents.External(true), emptyList(), emptyList())
    }
    private val requests = mutableListOf<Pair<ExampleScope, ExamplePage>>()
    private var results: (ExampleScope, ExamplePage) -> List<ExerciseExample> = { _, _ -> listOf(example) }

    @BeforeTest fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        startKoin { modules(CoreModule().module) }
    }

    @AfterTest fun teardown() {
        lifecycle.destroy()
        vm?.onDestroy()
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun create(mode: ExerciseLibraryMode): ExerciseLibraryViewModel {
        val useCase = UserExerciseExamplesUseCase(ExcludedMuscles(), ExcludedEquipments(), Examples(), Users())
        val model = ExerciseLibraryViewModel(mode, useCase, Examples(), Muscles())
        vm = model
        object : BaseComponent<ExerciseLibraryDirection>(DefaultComponentContext(lifecycle)) {
            override val viewModel = model
            override suspend fun eventListener(direction: ExerciseLibraryDirection) { directions += direction }
            @Composable override fun Render() = Unit
        }
        lifecycle.resume()
        return model
    }

    @Test fun componentSendsSelectionThenRequestsBack() = runTest {
        val examples = Examples()
        val useCase = UserExerciseExamplesUseCase(ExcludedMuscles(), ExcludedEquipments(), examples, Users())
        loadKoinModules(module {
            single<ExerciseExampleFeature> { examples }
            single<UserExerciseExamplesUseCase> { useCase }
            single<MuscleFeature> { Muscles() }
        })
        val received = mutableListOf<ExerciseExampleState>()
        object : BaseComponent<ExercisesRootDirection>(DefaultComponentContext(lifecycle)) {
            override val viewModel = ExercisesRootViewModel()
            init {
                observeResult<ComponentResult<ExerciseExampleState>>(
                    ResultKeys.create("custom-receiver"),
                ) { received += it.data }
            }
            override suspend fun eventListener(direction: ExercisesRootDirection) = Unit
            @Composable override fun Render() = Unit
        }
        var wentBack = false
        val component = ExerciseLibraryComponent(
            componentContext = DefaultComponentContext(lifecycle),
            mode = ExerciseLibraryMode.Default(null),
            resultKey = ResultKeys.create("custom-receiver"),
            back = { wentBack = true },
        )
        val model = component.retainedInstance<ExerciseLibraryViewModel> { error("Existing library VM expected") }
        vm = model
        lifecycle.resume()
        advanceUntilIdle()
        val selected = model.state.value.exerciseExamples.single()
        model.onExerciseExampleSelectClick(selected.value.id)
        advanceUntilIdle()
        assertEquals(listOf(selected), received)
        assertTrue(wentBack)
        model.onBack()
        advanceUntilIdle()
        assertTrue(wentBack)
        assertEquals(listOf(selected), received)
    }

    @Test fun bothModesSurviveTrainingRouteSerialization() {
        val modes = listOf(ExercisesRouter.Mode.Default("chest"), ExercisesRouter.Mode.SimilarTo("target"))
        modes.forEach { mode ->
            val route: TrainingRouter = TrainingRouter.Exercises(ExercisesRouter.Library(mode = mode, resultKey = ResultKeys.create("custom-receiver")))
            val encoded = Json.encodeToString(TrainingRouter.serializer(), route)
            assertEquals(route, Json.decodeFromString(TrainingRouter.serializer(), encoded))
        }
    }

    @Test fun exerciseCallerKeySurvivesRouteSerialization() {
        val route: TrainingRouter = TrainingRouter.Exercise(
            com.grippo.core.state.trainings.stubExercise(),
            resultKey = ResultKeys.create("custom-exercise-receiver"),
        )
        val encoded = Json.encodeToString(TrainingRouter.serializer(), route)
        assertEquals(route, Json.decodeFromString(TrainingRouter.serializer(), encoded))
    }

    @Test fun explicitResultKeySurvivesSerializationWithDefaultMode() {
        val route = ExercisesRouter.Library(resultKey = ResultKeys.create("exercise-library"))
        val encoded = Json.encodeToString(ExercisesRouter.Library.serializer(), route)
        assertEquals(route, Json.decodeFromString(ExercisesRouter.Library.serializer(), encoded))
        assertEquals(route, Json.decodeFromString(
            ExercisesRouter.Library.serializer(),
            """{"resultKey":"exercise-library"}""",
        ))
    }

    @Test fun selectionReturnsFullExampleAndBackDoesNotSelect() = runTest {
        val model = create(ExerciseLibraryMode.Default(null))
        advanceUntilIdle()
        model.onExerciseExampleSelectClick("missing")
        advanceUntilIdle()
        assertTrue(directions.isEmpty())
        val selected = model.state.value.exerciseExamples.single()
        model.onExerciseExampleSelectClick(selected.value.id)
        advanceUntilIdle()
        assertEquals<List<ExerciseLibraryDirection>>(listOf(ExerciseLibraryDirection.BackWithResult(selected)), directions)
        model.onBack()
        advanceUntilIdle()
        assertEquals(ExerciseLibraryDirection.Back, directions.last())
    }

    @Test fun replacementLoadsTargetAndFallsBackWhenSuggestionsAreEmpty() = runTest {
        results = { scope, _ -> if (scope is ExampleScope.SimilarTo) emptyList() else listOf(example) }
        val model = create(ExerciseLibraryMode.SimilarTo(example.value.id))
        advanceUntilIdle()
        assertEquals(example.value.id, (model.state.value.mode as ExerciseLibraryMode.SimilarTo).target?.id)
        assertEquals(QueryFilter.All, model.state.value.queries.filter)
        assertFalse(model.state.value.canShowSuggestions)
        assertEquals(1, model.state.value.exerciseExamples.size)
        assertTrue(requests.first().first is ExampleScope.SimilarTo)
        assertTrue(requests.last().first is ExampleScope.All)
    }

    @Test fun preselectedGroupCanBeToggledAndSearchCanBeCleared() = runTest {
        val model = create(ExerciseLibraryMode.Default("chest"))
        advanceUntilIdle()
        assertEquals(QueryFilter.Group("chest"), model.state.value.queries.filter)
        assertEquals(ExampleScope.All(muscleGroupId = "chest"), requests.last().first)
        model.onMuscleGroupClick("chest")
        advanceUntilIdle()
        assertEquals(QueryFilter.All, model.state.value.queries.filter)
        model.onQueryChange("bench")
        advanceUntilIdle()
        model.onQueryChange("")
        advanceUntilIdle()
        assertEquals("", model.state.value.queries.name)
    }

    @Test fun suggestionsRemainAvailableAndCanBeToggledWhenResultsExist() = runTest {
        val model = create(ExerciseLibraryMode.SimilarTo(example.value.id))
        advanceUntilIdle()
        assertTrue(model.state.value.canShowSuggestions)
        assertEquals(QueryFilter.Suggestions, model.state.value.queries.filter)
        model.onSuggestionsClick()
        advanceUntilIdle()
        assertEquals(QueryFilter.All, model.state.value.queries.filter)
        model.onSuggestionsClick()
        advanceUntilIdle()
        assertEquals(QueryFilter.Suggestions, model.state.value.queries.filter)
    }

    @Test fun emptyResultsStopPagination() = runTest {
        results = { _, _ -> emptyList() }
        val model = create(ExerciseLibraryMode.Default(null))
        advanceUntilIdle()
        assertTrue(model.state.value.exerciseExamples.isEmpty())
        assertTrue(model.state.value.pagination.isEndReached)
        val count = requests.size
        model.onLoadNextPage()
        advanceUntilIdle()
        assertEquals(count, requests.size)
    }

    @Test fun paginationAppendsDistinctItemsAndQueryRestartsFromFirstPage() = runTest {
        results = { _, page -> List(page.limits) { index ->
            val itemPage = if (index == 0) ExamplePage.Chunk.number else page.number
            example.copy(value = example.value.copy(id = "$itemPage-$index"))
        } }
        val model = create(ExerciseLibraryMode.Default(null))
        advanceUntilIdle()
        val limit = model.state.value.pagination.limit
        model.onLoadNextPage()
        advanceUntilIdle()
        assertEquals(limit * 2 - 1, model.state.value.exerciseExamples.size)
        model.onQueryChange("bench")
        assertIs<PaginationState.Restartable>(model.state.value.pagination)
        advanceUntilIdle()
        assertEquals(ExamplePage.Chunk.number, requests.last().second.number)
        assertEquals(limit, model.state.value.exerciseExamples.size)
        assertEquals("bench", model.state.value.queries.name)
    }

    private inner class Examples : ExerciseExampleFeature {
        override fun observeExerciseExample(id: String): Flow<ExerciseExample?> = flowOf(example)
        override fun observeExerciseExamples(name: String?, scope: ExampleScope, sorting: ExampleSortingEnum,
            rules: UserExerciseExampleRules, page: ExamplePage, experience: ExperienceEnum): Flow<List<ExerciseExample>> {
            requests += scope to page
            return flowOf(results(scope, page))
        }
        override fun observeExerciseExamples(ids: List<String>): Flow<List<ExerciseExample>> = error("unused")
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
