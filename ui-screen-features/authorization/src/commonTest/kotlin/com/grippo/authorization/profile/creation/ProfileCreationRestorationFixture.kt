package com.grippo.authorization.profile.creation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.analytics.authorization.AuthAnalyticsModule
import com.grippo.authorization.analytics.AuthorizationFixture
import com.grippo.authorization.analytics.RecordingAuthAnalytics
import com.grippo.authorization.profile.creation.user.UserViewModel
import com.grippo.core.foundation.CoreModule
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum
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
import com.grippo.toolkit.analytics.Analytics
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal suspend fun TestScope.withProfileRestorationFixture(
    block: suspend TestScope.(ProfileCreationRestorationFixture) -> Unit,
) {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = ProfileCreationRestorationFixture()
    try {
        fixture.start()
        runCurrent()
        block(fixture)
    } finally {
        fixture.destroy()
        runCurrent()
        stopKoin()
        Dispatchers.resetMain()
    }
}

internal class ProfileCreationRestorationFixture {
    val recorder = RecordingAuthAnalytics()
    val domain = AuthorizationFixture()
    val dialogs = mutableListOf<DialogConfig>()
    val profiles = mutableListOf<CreateUserProfile>()
    val welcomeStatuses = mutableListOf<HomeWelcomeStatus>()
    val pendingProfile = CompletableDeferred<Unit>()
    val muscles = MutableStateFlow(listOf(muscles("first", "m1", "m2"), muscles("second", "m3")))
    val equipments = MutableStateFlow(listOf(equipments("first", "e1", "e2"), equipments("second", "e3")))
    var homeNavigations = 0
        private set
    lateinit var component: ProfileCreationComponent
        private set
    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var stateKeeper: StateKeeperDispatcher
    private lateinit var instanceKeeper: InstanceKeeperDispatcher

    fun start() {
        val settings = object : LocalSettingsFeature {
            override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> {
                welcomeStatuses += value
                return Result.success(Unit)
            }
            override suspend fun clear(): Result<Unit> = error("Unexpected logout")
            override fun observeRange(): Flow<Range?> = error("Unused settings call")
            override suspend fun setRange(range: Range?): Result<Unit> = error("Unused settings call")
            override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> = error("Unused settings call")
            override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> = error("Unused settings call")
            override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> = error("Unused settings call")
        }
        val userFeature = object : UserFeature by domain {
            override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> {
                profiles += profile
                pendingProfile.await()
                return Result.success(true)
            }
        }
        startKoin {
            modules(CoreModule().module, AuthAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single { LogoutUseCase(domain, settings) }
                single { CreateProfileUseCase(userFeature, domain, domain, domain, domain) }
                single<LocalSettingsFeature> { settings }
                single<DialogController> { object : DialogController {
                    override fun open(config: DialogConfig) { dialogs += config }
                } }
                single<MuscleFeature> { object : MuscleFeature {
                    override fun observeMuscles(): Flow<List<MuscleGroup>> = muscles
                    override suspend fun getMuscles(): Result<Unit> = error("Unexpected catalog request")
                } }
                single<EquipmentFeature> { object : EquipmentFeature {
                    override fun observeEquipments(): Flow<List<EquipmentGroup>> = equipments
                    override suspend fun getEquipments(): Result<Unit> = error("Unexpected catalog request")
                } }
            })
        }
        create()
    }

    fun restore(beforeCreate: () -> Unit = {}) {
        val activeChild = component.childStack.value.active.instance
        val views = recorder.views.size
        val events = recorder.names
        // Encoding materializes all lazy containers; decoding rules out retained objects.
        val archive = Json.encodeToString(stateKeeper.save())
        val saved = Json.decodeFromString<SerializableContainer>(archive)
        assertSame(activeChild, component.childStack.value.active.instance)
        assertEquals(views, recorder.views.size)
        assertEquals(events, recorder.names)
        destroy()
        beforeCreate()
        create(saved)
    }

    fun destroy() {
        if (::lifecycle.isInitialized) lifecycle.destroy()
        if (::instanceKeeper.isInitialized) instanceKeeper.destroy()
    }

    inline fun <reified T : InstanceKeeper.Instance> model(
        context: ComponentContext = component.childStack.value.active.instance.component,
    ): T = assertIs<T>(context.instanceKeeper.get(typeOf<T>()))

    fun measurements(weight: Float, height: Int) {
        val user = model<UserViewModel>()
        user.onWeightPickerClick()
        assertIs<DialogConfig.WeightPicker>(dialogs.last()).onResult(weight)
        user.onHeightPickerClick()
        assertIs<DialogConfig.HeightPicker>(dialogs.last()).onResult(height)
    }

    private fun create(saved: SerializableContainer? = null) {
        lifecycle = LifecycleRegistry()
        stateKeeper = StateKeeperDispatcher(saved)
        instanceKeeper = InstanceKeeperDispatcher()
        component = ProfileCreationComponent(
            componentContext = DefaultComponentContext(lifecycle, stateKeeper, instanceKeeper),
            toHome = { homeNavigations++ },
            toLogin = { error("Unexpected login navigation") },
            close = { error("Unexpected graph close") },
        )
        lifecycle.resume()
    }

    companion object {
        fun muscles(group: String, vararg ids: String): MuscleGroup = MuscleGroup(
            id = group,
            name = group,
            type = MuscleGroupEnum.ARMS_AND_FOREARMS,
            muscles = ids.map { Muscle(it, it, Duration.ZERO, 1f, 1f, MuscleEnum.BICEPS) },
        )

        fun equipments(group: String, vararg ids: String): EquipmentGroup = EquipmentGroup(
            id = group,
            name = group,
            type = EquipmentGroupEnum.FREE_WEIGHT,
            equipments = ids.map { Equipment(it, it, EquipmentEnum.DUMBBELLS) },
        )
    }
}
