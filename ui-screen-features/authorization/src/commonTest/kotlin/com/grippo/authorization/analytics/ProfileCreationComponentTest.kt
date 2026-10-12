package com.grippo.authorization.analytics

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.authorization.profile.creation.ProfileCreationComponent
import com.grippo.authorization.profile.creation.completed.ProfileCompletedLoader
import com.grippo.authorization.profile.creation.completed.ProfileCompletedViewModel
import com.grippo.authorization.profile.creation.excluded.muscles.ExcludedMusclesViewModel
import com.grippo.authorization.profile.creation.experience.ExperienceViewModel
import com.grippo.authorization.profile.creation.missing.equipments.MissingEquipmentsViewModel
import com.grippo.authorization.profile.creation.user.UserDirection
import com.grippo.authorization.profile.creation.user.UserViewModel
import com.grippo.core.error.provider.ErrorProvider
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.user.CreateProfileUseCase
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.ProfileCreationRouter
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.analytics.transport.AnalyticsValue
import com.grippo.toolkit.crashreporting.CrashReporting
import com.grippo.toolkit.crashreporting.context.CrashReportContext
import com.grippo.toolkit.crashreporting.values.CrashDiagnosticCode
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class ProfileCreationComponentTest {
    @Test
    fun allFiveStepsPresentOnceRestoreAndCompleteTheActualProfileOperation() = runTest {
        val recorder = RecordingAuthAnalytics()
        val pending = CompletableDeferred<Unit>()
        val domain = AuthorizationFixture()
        val profiles = mutableListOf<CreateUserProfile>()
        val welcomeStatuses = mutableListOf<HomeWelcomeStatus>()
        val userFeature = object : UserFeature by domain {
            override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> {
                profiles += profile
                pending.await()
                return Result.success(true)
            }
        }
        val settings = object : LocalSettingsFeature by TestSettings(mutableListOf()) {
            override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> {
                welcomeStatuses += value
                return Result.success(Unit)
            }
        }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin {
            modules(CoreModule().module, AuthAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single { LogoutUseCase(domain, settings) }
                single { CreateProfileUseCase(userFeature, domain, domain, domain, domain) }
                single<LocalSettingsFeature> { settings }
                single<DialogController> { object : DialogController {
                    override fun open(config: DialogConfig) = error("Unexpected dialog")
                } }
                single<MuscleFeature> { object : MuscleFeature {
                    override fun observeMuscles(): Flow<List<MuscleGroup>> = flowOf(listOf(MuscleGroup(
                        id = "group", name = "Group", type = MuscleGroupEnum.ARMS_AND_FOREARMS,
                        muscles = listOf(Muscle("muscle", "Muscle", Duration.ZERO, 1f, 1f, MuscleEnum.BICEPS)),
                    )))
                    override suspend fun getMuscles(): Result<Unit> = error("Unexpected catalog request")
                } }
                single<EquipmentFeature> { object : EquipmentFeature {
                    override fun observeEquipments(): Flow<List<EquipmentGroup>> = flowOf(listOf(EquipmentGroup(
                        id = "group", name = "Group", type = EquipmentGroupEnum.FREE_WEIGHT,
                        equipments = listOf(Equipment("equipment", "Equipment", EquipmentEnum.DUMBBELLS)),
                    )))
                    override suspend fun getEquipments(): Result<Unit> = error("Unexpected catalog request")
                } }
            })
        }
        val lifecycle = LifecycleRegistry()
        var home = 0
        try {
            val component = ProfileCreationComponent(
                componentContext = DefaultComponentContext(lifecycle),
                toHome = { home++ },
                toLogin = { error("Unexpected login navigation") },
                close = { error("Unexpected graph close") },
            )
            assertTrue(recorder.views.isEmpty())
            lifecycle.resume()
            runCurrent()
            assertScreenCycle(lifecycle, recorder, "profile_creation_user")
            val user = assertIs<ProfileCreationComponent.Child.User>(component.childStack.value.active.instance)
            val userViewModel = assertIs<UserViewModel>(user.component.instanceKeeper.get(typeOf<UserViewModel>()))
            userViewModel.onNameChange("Private name")
            userViewModel.onNextClick()
            runCurrent()

            assertScreenCycle(lifecycle, recorder, "profile_creation_experience")
            val experience = assertIs<ProfileCreationComponent.Child.Experience>(component.childStack.value.active.instance)
            val experienceViewModel = assertIs<ExperienceViewModel>(
                experience.component.instanceKeeper.get(typeOf<ExperienceViewModel>())
            )
            experienceViewModel.onExperienceClick(ExperienceEnumState.BEGINNER)
            experienceViewModel.onNextClick()
            runCurrent()

            assertScreenCycle(lifecycle, recorder, "profile_creation_excluded_muscles")
            val muscles = assertIs<ProfileCreationComponent.Child.ExcludedMuscles>(component.childStack.value.active.instance)
            val musclesViewModel = assertIs<ExcludedMusclesViewModel>(
                muscles.component.instanceKeeper.get(typeOf<ExcludedMusclesViewModel>())
            )
            assertEquals(listOf("muscle"), musclesViewModel.state.value.selectedMuscleIds)
            musclesViewModel.onSelect("muscle")
            musclesViewModel.onNextClick()
            runCurrent()

            assertScreenCycle(lifecycle, recorder, "profile_creation_missing_equipments")
            val equipments = assertIs<ProfileCreationComponent.Child.MissingEquipments>(component.childStack.value.active.instance)
            val equipmentsViewModel = assertIs<MissingEquipmentsViewModel>(
                equipments.component.instanceKeeper.get(typeOf<MissingEquipmentsViewModel>())
            )
            assertEquals(listOf("equipment"), equipmentsViewModel.state.value.selectedEquipmentIds)
            equipmentsViewModel.onEquipmentClick("equipment")
            equipmentsViewModel.onNextClick()
            runCurrent()

            assertScreenCycle(lifecycle, recorder, "profile_creation_completed")
            val completed = assertIs<ProfileCreationComponent.Child.Completed>(component.childStack.value.active.instance)
            val completedViewModel = assertIs<ProfileCompletedViewModel>(
                completed.component.instanceKeeper.get(typeOf<ProfileCompletedViewModel>())
            )
            assertEquals(setOf<ProfileCompletedLoader>(ProfileCompletedLoader.ProfileCreation), completedViewModel.loaders.value)
            assertEquals(0, home)
            assertTrue(domain.calls.isEmpty())
            assertTrue(welcomeStatuses.isEmpty())
            assertEquals(CreateUserProfile("Private name", 70f, ExperienceEnum.BEGINNER, 175,
                listOf("muscle"), listOf("equipment")), profiles.single())
            assertEquals(listOf("profile_operation_started"), recorder.events
                .filter { it.text("profile_action") == "create" }.map { it.event.schema.name })

            val screens = listOf("profile_creation_user", "profile_creation_experience",
                "profile_creation_excluded_muscles", "profile_creation_missing_equipments", "profile_creation_completed")
            assertEquals(screens, recorder.views.filter { it.navigation.reason == NavigationReason.Initial }
                .map { it.navigation.screen.analyticsCode })
            assertEquals(screens.flatMap { listOf(it, it) }, recorder.views.map { it.navigation.screen.analyticsCode })
            assertTrue(recorder.views.all { it.navigation.referrer == null && it.flow == null })
            assertEquals(screens.dropLast(1), recorder.events.filter { it.text("interaction_action") == "next" }
                .map { it.text("interaction_screen") })
            assertEquals("beginner", recorder.events.single { it.event.schema.name == "auth_experience_selected" }
                .text("profile_experience"))
            val selections = recorder.events.filter { it.event.schema.name == "auth_selection_changed" }
            assertEquals(screens.slice(2..3), selections.map { it.text("interaction_screen") })
            assertTrue(selections.all {
                it.event.parameters().single { parameter -> parameter.key.name == "selected_count" }.value == AnalyticsValue.Integer(0)
            })

            pending.complete(Unit)
            runCurrent()
            assertEquals(1, home)
            assertTrue(completedViewModel.loaders.value.isEmpty())
            assertEquals(listOf("profile", "excluded_muscles", "excluded_equipments", "exercise_examples", "weight_history"), domain.calls)
            assertEquals(listOf(HomeWelcomeStatus.PendingCelebration), welcomeStatuses)
            val operations = recorder.events.filter { it.text("profile_action") == "create" }
            assertEquals(listOf("profile_operation_started", "profile_operation_completed"), operations.map { it.event.schema.name })
            assertEquals(operations.first().operation, operations.last().operation)
            assertTrue(recorder.events.all { it.flow == null })
            assertEquals(10, recorder.views.size)
        } finally {
            lifecycle.destroy()
            runCurrent()
            stopKoin()
            Dispatchers.resetMain()
        }
        assertTrue(recorder.views.all { it.closes == 1 })
    }

    @Test
    fun leafViewModelsPresentScreensAndHardwareBackKeepsBusinessNavigation() = runTest {
        val recorder = RecordingAuthAnalytics()
        val businessCalls = mutableListOf<String>()
        val dialogs = mutableListOf<DialogConfig>()
        val settings = TestSettings(businessCalls)
        val authorization = object : AuthorizationFeature by AuthorizationFixture() {
            override suspend fun deletePushToken(): Result<Unit> {
                businessCalls += "push_delete"
                return Result.success(Unit)
            }

            override suspend fun logout() {
                businessCalls += "logout"
            }
        }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        startKoin {
            modules(
                CoreModule().module,
                AuthAnalyticsModule().module,
                module {
                    single<Analytics> { recorder }
                    single { LogoutUseCase(authorization, settings) }
                    single<DialogController> { object : DialogController {
                        override fun open(config: DialogConfig) {
                            dialogs += config
                        }
                    } }
                    single<MuscleFeature> { object : MuscleFeature {
                        override fun observeMuscles(): Flow<List<MuscleGroup>> = flowOf(emptyList())
                        override suspend fun getMuscles(): Result<Unit> = error("Unexpected catalog request")
                    } }
                },
            )
        }
        val lifecycle = LifecycleRegistry()
        val back = BackDispatcher()
        var returnedToLogin = 0
        try {
            val component = ProfileCreationComponent(
                componentContext = DefaultComponentContext(lifecycle, backHandler = back),
                toHome = { error("Unexpected home navigation") },
                toLogin = { returnedToLogin++ },
                close = { error("Graph close cannot replace leaf back") },
            )
            assertTrue(recorder.views.isEmpty())
            lifecycle.resume()
            runCurrent()
            val user = assertIs<ProfileCreationComponent.Child.User>(component.childStack.value.active.instance)
            val userViewModel = assertIs<UserViewModel>(user.component.instanceKeeper.get(typeOf<UserViewModel>()))
            userViewModel.onNextClick()
            runCurrent()
            assertEquals(ProfileCreationRouter.User, component.childStack.value.active.configuration)
            assertTrue(recorder.events.none { it.text("interaction_action") == "next" })

            userViewModel.onNameFocused()
            userViewModel.onNameKeyboardDone()
            userViewModel.onNameChange("Private name")
            userViewModel.onNameChange("Another private name")
            assertEquals(1, recorder.names.count { it == "auth_input_changed" })
            userViewModel.onNameChange("")
            userViewModel.onNameCleared()
            userViewModel.onNameChange("Private name")
            val nameFacts = recorder.events.filter { it.event.schema.name in setOf(
                "auth_input_focused", "auth_input_cleared", "auth_keyboard_action",
            ) }
            assertEquals(listOf("auth_input_focused", "auth_keyboard_action", "auth_input_cleared"),
                nameFacts.map { it.event.schema.name })
            assertTrue(nameFacts.all {
                it.text("input_field") == "name" && it.text("interaction_screen") == "profile_creation_user"
            })
            assertEquals("done", nameFacts.single { it.event.schema.name == "auth_keyboard_action" }.text("keyboard_action"))
            userViewModel.onWeightPickerClick()
            val weightPicker = assertIs<DialogConfig.WeightPicker>(dialogs.last())
            weightPicker.onResult(0f)
            userViewModel.onHeightPickerClick()
            val heightPicker = assertIs<DialogConfig.HeightPicker>(dialogs.last())
            heightPicker.onResult(0)
            userViewModel.onNextClick()
            runCurrent()
            assertEquals(ProfileCreationRouter.User, component.childStack.value.active.configuration)
            assertTrue(recorder.events.none { it.text("interaction_action") == "next" })

            weightPicker.onResult(70f)
            userViewModel.onNextClick()
            runCurrent()
            assertEquals(ProfileCreationRouter.User, component.childStack.value.active.configuration)
            assertTrue(recorder.events.none { it.text("interaction_action") == "next" })

            heightPicker.onResult(175)
            val pickerOpens = recorder.events.filter { it.event.schema.name == "auth_picker_opened" }
            assertEquals(listOf("weight", "height"), pickerOpens.map { it.text("input_field") })
            val pickerResults = recorder.events.filter {
                it.event.schema.name == "auth_input_changed" && it.text("input_field") in setOf("weight", "height")
            }
            assertEquals(listOf("weight" to "empty", "height" to "empty", "weight" to "valid", "height" to "valid"),
                pickerResults.map { it.text("input_field") to it.text("input_status") })
            assertTrue((pickerOpens + pickerResults).all { it.text("interaction_screen") == "profile_creation_user" })
            userViewModel.onNextClick()
            runCurrent()
            assertEquals(1, recorder.events.count { it.text("interaction_action") == "next" })
            val experience = assertIs<ProfileCreationComponent.Child.Experience>(component.childStack.value.active.instance)
            val experienceViewModel = assertIs<ExperienceViewModel>(
                experience.component.instanceKeeper.get(typeOf<ExperienceViewModel>())
            )
            experienceViewModel.onExperienceClick(ExperienceEnumState.BEGINNER)
            experienceViewModel.onNextClick()
            runCurrent()
            assertEquals(ProfileCreationRouter.ExcludedMuscles, component.childStack.value.active.configuration)

            assertTrue(back.back())
            runCurrent()
            assertEquals(ProfileCreationRouter.Experience, component.childStack.value.active.configuration)
            assertTrue(back.back())
            runCurrent()
            assertEquals(ProfileCreationRouter.User, component.childStack.value.active.configuration)
            assertEquals(
                listOf("profile_creation_user", "profile_creation_experience", "profile_creation_excluded_muscles",
                    "profile_creation_experience", "profile_creation_user"),
                recorder.views.map { it.navigation.screen.analyticsCode },
            )
            assertTrue(recorder.views.all { it.navigation.referrer == null && it.flow == null })
            assertEquals(
                listOf(NavigationReason.Initial, NavigationReason.Initial, NavigationReason.Initial,
                    NavigationReason.Restore, NavigationReason.Restore),
                recorder.views.map { it.navigation.reason },
            )
            lifecycle.pause()
            assertTrue(recorder.views.all { it.closes == 1 })
            lifecycle.resume()
            runCurrent()
            assertEquals(NavigationReason.Restore, recorder.views.last().navigation.reason)
            assertTrue(recorder.names.none { it.contains("flow") })

            assertTrue(back.back())
            runCurrent()
            assertEquals(listOf("push_delete", "settings_clear", "logout"), businessCalls)
            assertEquals(1, returnedToLogin)
            assertEquals(listOf("profile_operation_started", "profile_operation_completed"), recorder.events
                .filter { it.text("profile_action") == "logout" }.map { it.event.schema.name })
            assertEquals(listOf("profile_creation_excluded_muscles", "profile_creation_experience", "profile_creation_user"),
                recorder.events.filter { it.text("interaction_action") == "back" }.map { it.text("interaction_screen") })
            assertTrue(recorder.events.all { it.flow == null })
        } finally {
            lifecycle.destroy()
            runCurrent()
            stopKoin()
            Dispatchers.resetMain()
        }
        assertTrue(recorder.views.all { it.closes == 1 })
    }

    @Test
    fun userBackPreservesLocalSettingsFailureAndDoesNotLogoutOrNavigate() = runTest {
        val original = IllegalStateException("Local settings cleanup failed")
        val recorder = RecordingAuthAnalytics()
        val calls = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val crashes = mutableListOf<Throwable>()
        val directions = mutableListOf<UserDirection>()
        val settings = TestSettings(calls, clearResult = Result.failure(original))
        var logoutJob: Job? = null
        val authorization = object : AuthorizationFeature by AuthorizationFixture() {
            override suspend fun deletePushToken(): Result<Unit> {
                calls += "push_delete"
                logoutJob = currentCoroutineContext()[Job]
                return Result.success(Unit)
            }

            override suspend fun logout() {
                calls += "logout"
            }
        }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val application = startKoin {
            modules(CoreModule().module, AuthAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single { LogoutUseCase(authorization, settings) }
                single<DialogController> { object : DialogController {
                    override fun open(config: DialogConfig) = error("Unexpected dialog")
                } }
                single<ErrorProvider> { object : ErrorProvider {
                    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
                        errors += exception
                    }
                } }
                single<CrashReporting> { object : CrashReporting {
                    override fun reportException(throwable: Throwable, context: CrashReportContext) {
                        crashes += throwable
                    }
                    override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
                } }
            })
        }
        var viewModel: UserViewModel? = null
        var navigationJob: Job? = null
        try {
            val user = UserViewModel(application.koin.get(), application.koin.get(), application.koin.get())
            viewModel = user
            navigationJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                user.navigator.collect { directions += it }
            }
            user.onBack()
            runCurrent()

            assertEquals(listOf("push_delete", "settings_clear"), calls)
            assertTrue(directions.isEmpty())
            val reportedError = errors.single()
            assertSame(original, if (reportedError.cause === original) reportedError.cause else reportedError)
            val reportedCrash = crashes.single()
            assertSame(original, if (reportedCrash.cause === original) reportedCrash.cause else reportedCrash)
            assertEquals(listOf("auth_control_clicked", "profile_operation_started", "profile_operation_failed"), recorder.names)
            assertEquals("profile_creation_user", recorder.events.first().text("interaction_screen"))
            assertEquals("back", recorder.events.first().text("interaction_action"))
            val operations = recorder.events.filter { it.text("profile_action") == "logout" }
            assertEquals(listOf("profile_operation_started", "profile_operation_failed"), operations.map { it.event.schema.name })
            assertNotNull(operations.first().operation)
            assertEquals(operations.first().operation, operations.last().operation)
            assertTrue(recorder.events.all { it.flow == null })
            assertTrue(user.loaders.value.isEmpty())
            assertTrue(assertNotNull(logoutJob).isCompleted)
        } finally {
            viewModel?.onDestroy()
            runCurrent()
            stopKoin()
            Dispatchers.resetMain()
        }
        assertTrue(assertNotNull(navigationJob).isCompleted)
    }

    @Test
    fun userBackStillLogsOutWhenRemotePushCleanupReturnsFailure() = runTest {
        val remoteFailure = IllegalStateException("Remote push cleanup unavailable")
        val recorder = RecordingAuthAnalytics()
        val calls = mutableListOf<String>()
        val errors = mutableListOf<Throwable>()
        val crashes = mutableListOf<Throwable>()
        val directions = mutableListOf<UserDirection>()
        val settings = TestSettings(calls)
        var logoutJob: Job? = null
        val authorization = object : AuthorizationFeature by AuthorizationFixture() {
            override suspend fun deletePushToken(): Result<Unit> {
                calls += "push_delete"
                logoutJob = currentCoroutineContext()[Job]
                return Result.failure(remoteFailure)
            }

            override suspend fun logout() {
                calls += "logout"
            }
        }
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val application = startKoin {
            modules(CoreModule().module, AuthAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single { LogoutUseCase(authorization, settings) }
                single<DialogController> { object : DialogController {
                    override fun open(config: DialogConfig) = error("Unexpected dialog")
                } }
                single<ErrorProvider> { object : ErrorProvider {
                    override suspend fun provide(exception: Throwable, callback: () -> Unit) {
                        errors += exception
                    }
                } }
                single<CrashReporting> { object : CrashReporting {
                    override fun reportException(throwable: Throwable, context: CrashReportContext) {
                        crashes += throwable
                    }
                    override fun logBreadcrumb(code: CrashDiagnosticCode) = Unit
                } }
            })
        }
        var viewModel: UserViewModel? = null
        var navigationJob: Job? = null
        try {
            val user = UserViewModel(application.koin.get(), application.koin.get(), application.koin.get())
            viewModel = user
            navigationJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                user.navigator.collect { directions += it }
            }
            user.onBack()
            runCurrent()

            assertEquals(listOf("push_delete", "settings_clear", "logout"), calls)
            assertEquals(listOf<UserDirection>(UserDirection.Back), directions)
            assertTrue(errors.isEmpty())
            assertTrue(crashes.isEmpty())
            assertEquals(listOf("auth_control_clicked", "profile_operation_started", "profile_operation_completed"), recorder.names)
            val operations = recorder.events.filter { it.text("profile_action") == "logout" }
            assertEquals(listOf("profile_operation_started", "profile_operation_completed"), operations.map { it.event.schema.name })
            assertNotNull(operations.first().operation)
            assertEquals(operations.first().operation, operations.last().operation)
            assertTrue(recorder.events.all { it.flow == null })
            assertTrue(user.loaders.value.isEmpty())
            assertTrue(assertNotNull(logoutJob).isCompleted)
        } finally {
            viewModel?.onDestroy()
            runCurrent()
            stopKoin()
            Dispatchers.resetMain()
        }
        assertTrue(assertNotNull(navigationJob).isCompleted)
    }

    private fun TestScope.assertScreenCycle(
        lifecycle: LifecycleRegistry,
        recorder: RecordingAuthAnalytics,
        screen: String,
    ) {
        val views = recorder.views.size
        assertEquals(screen, recorder.views.last().navigation.screen.analyticsCode)
        assertEquals(NavigationReason.Initial, recorder.views.last().navigation.reason)
        lifecycle.pause()
        assertTrue(recorder.views.all { it.closes == 1 })
        lifecycle.resume()
        runCurrent()
        assertEquals(views + 1, recorder.views.size)
        assertEquals(screen, recorder.views.last().navigation.screen.analyticsCode)
        assertEquals(NavigationReason.Restore, recorder.views.last().navigation.reason)
        assertEquals(0, recorder.views.last().closes)
    }

    private class TestSettings(
        private val calls: MutableList<String>,
        private val clearResult: Result<Unit> = Result.success(Unit),
    ) : LocalSettingsFeature {
        override suspend fun clear(): Result<Unit> {
            calls += "settings_clear"
            return clearResult
        }

        override fun observeRange(): Flow<Range?> = error("Unused settings call")
        override suspend fun setRange(range: Range?): Result<Unit> = error("Unused settings call")
        override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> = error("Unused settings call")
        override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> = error("Unused settings call")
        override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> = error("Unused settings call")
        override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> = error("Unused settings call")
    }
}
