package com.grippo.authorization.analytics

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.authorization.profile.creation.ProfileCreationComponent
import com.grippo.authorization.profile.creation.experience.ExperienceViewModel
import com.grippo.authorization.profile.creation.user.UserViewModel
import com.grippo.core.foundation.CoreModule
import com.grippo.core.state.profile.ExperienceEnumState
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.ProfileCreationRouter
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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

            userViewModel.onNameChange("Private name")
            userViewModel.onNameChange("Another private name")
            assertEquals(1, recorder.names.count { it == "auth_input_changed" })
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
            assertTrue(recorder.events.all { it.flow == null })
        } finally {
            lifecycle.destroy()
            stopKoin()
            Dispatchers.resetMain()
        }
    }

    private class TestSettings(private val calls: MutableList<String>) : LocalSettingsFeature {
        override suspend fun clear(): Result<Unit> {
            calls += "settings_clear"
            return Result.success(Unit)
        }

        override fun observeRange(): Flow<Range?> = error("Unused settings call")
        override suspend fun setRange(range: Range?): Result<Unit> = error("Unused settings call")
        override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> = error("Unused settings call")
        override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> = error("Unused settings call")
        override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> = error("Unused settings call")
        override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> = error("Unused settings call")
    }
}
