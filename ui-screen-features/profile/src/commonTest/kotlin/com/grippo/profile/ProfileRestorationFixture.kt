package com.grippo.profile

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.instancekeeper.InstanceKeeperDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.SerializableContainer
import com.arkivanov.essenty.statekeeper.StateKeeperDispatcher
import com.grippo.core.foundation.CoreModule
import com.grippo.core.foundation.analytics.ScreenAnalytics
import com.grippo.analytics.profile.ProfileAnalyticsModule
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.authorization.DeleteProfileUseCase
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.api.equipment.models.Equipment
import com.grippo.data.features.api.equipment.models.EquipmentEnum
import com.grippo.data.features.api.equipment.models.EquipmentGroup
import com.grippo.data.features.api.equipment.models.EquipmentGroupEnum
import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.Goal
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum
import com.grippo.data.features.api.goal.models.PersonalizationKeyEnum
import com.grippo.data.features.api.goal.models.SetGoal
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.Muscle
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.CreateUserProfile
import com.grippo.data.features.api.user.models.RoleEnum
import com.grippo.data.features.api.user.models.User
import com.grippo.data.features.api.user.models.UserStats
import com.grippo.data.features.api.weight.history.UpdateWeightUseCase
import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.api.weight.history.models.WeightHistory
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.screen.api.ProfileRouter
import com.grippo.toolkit.local.notification.AppNotification
import com.grippo.toolkit.local.notification.NotificationKey
import com.grippo.toolkit.local.notification.NotificationManager
import com.grippo.toolkit.link.opener.LinkOpener
import com.grippo.toolkit.link.opener.LinkOpenResult
import com.grippo.toolkit.analytics.Analytics
import com.grippo.toolkit.analytics.context.AnalyticsContext
import com.grippo.toolkit.analytics.context.FlowContext
import com.grippo.toolkit.analytics.context.NavigationContext
import com.grippo.toolkit.analytics.context.OperationId
import com.grippo.toolkit.analytics.identity.AnalyticsIdentity
import com.grippo.toolkit.analytics.lifecycle.AnalyticsCollection
import com.grippo.toolkit.analytics.lifecycle.ScreenPresentation
import com.grippo.toolkit.analytics.schema.AnalyticsEvent
import kotlin.reflect.typeOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.time.Duration
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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.PluralStringResource
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

internal suspend fun TestScope.withProfileRestorationFixture(
    initial: ProfileRouter = ProfileRouter.Menu,
    block: suspend TestScope.(ProfileRestorationFixture) -> Unit,
) {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val fixture = ProfileRestorationFixture(initial)
    try {
        fixture.start()
        runCurrent()
        block(fixture)
    } finally {
        try {
            fixture.destroy()
            runCurrent()
        } finally {
            stopKoin()
            Dispatchers.resetMain()
        }
    }
}

internal class ProfileRestorationFixture(private val initial: ProfileRouter) {
    val users = MutableStateFlow<User?>(user())
    val goals = MutableStateFlow<Goal?>(goal())
    val muscles = MutableStateFlow(listOf(muscles("first", "m1", "m2"), muscles("second", "m3")))
    val equipments = MutableStateFlow(listOf(equipments("first", "e1", "e2"), equipments("second", "e3")))
    val excludedMuscles = MutableStateFlow<List<Muscle>>(emptyList())
    val excludedEquipments = MutableStateFlow<List<Equipment>>(emptyList())
    val history = MutableStateFlow<List<WeightHistory>>(emptyList())
    val dialogs = mutableListOf<DialogConfig>()
    val savedWeights = mutableListOf<Float>()
    val savedHeights = mutableListOf<Int>()
    val savedExperiences = mutableListOf<ExperienceEnum>()
    val savedGoals = mutableListOf<SetGoal>()
    val savedExcludedMuscles = mutableListOf<List<String>>()
    val savedExcludedEquipments = mutableListOf<List<String>>()
    val notifications = mutableListOf<AppNotification>()
    val views = mutableListOf<View>()
    val presentationAttempts = mutableListOf<NavigationContext>()
    var presentationFailure: Throwable? = null
    var closeFailure: Throwable? = null
    var screenBindings = 0
        private set
    var closeCalls = 0
        private set
    lateinit var component: ProfileComponent
        private set
    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var stateKeeper: StateKeeperDispatcher
    private lateinit var instanceKeeper: InstanceKeeperDispatcher

    private val recorder = object : Analytics {
        override fun presentScreen(navigation: NavigationContext, flow: FlowContext?): ScreenPresentation {
            presentationAttempts += navigation
            presentationFailure?.let { throw it }
            val view = View(navigation, flow)
            views += view
            return object : ScreenPresentation {
                override fun close() {
                    view.closes++
                    closeFailure?.let { throw it }
                }
            }
        }

        override fun track(event: AnalyticsEvent, flow: FlowContext?, operation: OperationId?): Unit =
            error("Screen tracking must not emit custom events")

        override fun updateContext(context: AnalyticsContext): Unit = error("Unused context update")
        override fun updateIdentity(identity: AnalyticsIdentity): Unit = error("Unused identity update")
        override fun updateCollection(collection: AnalyticsCollection): Unit = error("Unused collection update")
    }

    fun start() {
        val userFeature = object : UserFeature {
            override fun observeUser(): Flow<User?> = users
            override suspend fun getUser(): Result<Boolean> = Result.success(users.value != null)
            override suspend fun setHeight(height: Int): Result<Boolean> {
                savedHeights += height
                return Result.success(true)
            }
            override suspend fun setExperience(experience: ExperienceEnum): Result<Boolean> {
                savedExperiences += experience
                return Result.success(true)
            }
            override suspend fun createProfile(profile: CreateUserProfile): Result<Boolean> = error("Unused profile creation")
            override suspend fun deleteProfile(): Result<Unit> = error("Unused account deletion")
        }
        val weightHistoryFeature = object : WeightHistoryFeature {
            override fun observeWeightHistory(): Flow<List<WeightHistory>> = history
            override fun observeLastWeight(): Flow<WeightHistory?> = error("Unused last weight")
            override suspend fun getWeightHistory(): Result<Unit> = Result.success(Unit)
            override suspend fun updateWeight(value: Float): Result<Unit> {
                savedWeights += value
                return Result.success(Unit)
            }
            override suspend fun deleteWeight(id: String): Result<Unit> = error("Unused weight deletion")
        }
        val settings = object : LocalSettingsFeature {
            override fun observeRange(): Flow<Range?> = error("Unused settings")
            override suspend fun setRange(range: Range?): Result<Unit> = error("Unused settings")
            override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> = error("Unused settings")
            override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> = error("Unused settings")
            override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> = error("Unused settings")
            override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> = error("Unused settings")
            override suspend fun clear(): Result<Unit> = error("Unused settings")
        }
        val authorization = object : AuthorizationFeature {
            override suspend fun login(email: String, password: String): Result<Unit> = error("Unused authorization")
            override suspend fun google(token: String): Result<Unit> = error("Unused authorization")
            override suspend fun apple(code: String): Result<Unit> = error("Unused authorization")
            override suspend fun register(email: String, password: String): Result<Unit> = error("Unused authorization")
            override suspend fun logout(): Unit = error("Unused authorization")
            override fun getToken(): Flow<String?> = error("Unused authorization")
            override suspend fun updatePushToken(token: String): Result<Unit> = error("Unused authorization")
            override suspend fun getPushToken(): String? = error("Unused authorization")
            override suspend fun deletePushToken(): Result<Unit> = error("Unused authorization")
        }
        val application = startKoin {
            modules(CoreModule().module, ProfileAnalyticsModule().module, module {
                single<Analytics> { recorder }
                single<UserFeature> { userFeature }
                single<WeightHistoryFeature> { weightHistoryFeature }
                single { UpdateWeightUseCase(userFeature, weightHistoryFeature) }
                single { LogoutUseCase(authorization, settings) }
                single { DeleteProfileUseCase(settings, userFeature) }
                single<LinkOpener> { object : LinkOpener {
                    override fun open(url: String): LinkOpenResult = error("Unused social link")
                } }
                single<DialogController> { object : DialogController {
                    override fun open(config: DialogConfig) { dialogs += config }
                } }
                single<StringProvider> { object : StringProvider {
                    override suspend fun get(id: StringResource, vararg args: Any): String = "notification"
                    override suspend fun plural(id: PluralStringResource, quantity: Int, vararg args: Any): String = error("Unused plural")
                } }
                single<NotificationManager> { object : NotificationManager {
                    override fun show(notification: AppNotification, delay: Duration): NotificationKey {
                        notifications += notification
                        return notification.id
                    }
                    override fun cancel(id: NotificationKey): Unit = error("Unused notification cancellation")
                    override suspend fun isPending(id: NotificationKey): Boolean = error("Unused pending notification")
                } }
                single<GoalFeature> { object : GoalFeature {
                    override fun observeGoal(): Flow<Goal?> = goals
                    override suspend fun getGoal(): Result<Boolean> = Result.success(goals.value != null)
                    override suspend fun setGoal(goal: SetGoal): Result<Boolean> {
                        savedGoals += goal
                        return Result.success(true)
                    }
                } }
                single<MuscleFeature> { object : MuscleFeature {
                    override fun observeMuscles(): Flow<List<MuscleGroup>> = muscles
                    override suspend fun getMuscles(): Result<Unit> = error("Unused muscle catalog request")
                } }
                single<EquipmentFeature> { object : EquipmentFeature {
                    override fun observeEquipments(): Flow<List<EquipmentGroup>> = equipments
                    override suspend fun getEquipments(): Result<Unit> = error("Unused equipment catalog request")
                } }
                single<ExcludedMusclesFeature> { object : ExcludedMusclesFeature {
                    override fun observeExcludedMuscles(): Flow<List<Muscle>> = excludedMuscles
                    override suspend fun getExcludedMuscles(): Result<Unit> = Result.success(Unit)
                    override suspend fun setExcludedMuscles(ids: List<String>): Result<Unit> {
                        savedExcludedMuscles += ids
                        return Result.success(Unit)
                    }
                } }
                single<ExcludedEquipmentsFeature> { object : ExcludedEquipmentsFeature {
                    override fun observeExcludedEquipments(): Flow<List<Equipment>> = excludedEquipments
                    override suspend fun getExcludedEquipments(): Result<Unit> = Result.success(Unit)
                    override suspend fun setExcludedEquipments(ids: List<String>): Result<Unit> {
                        savedExcludedEquipments += ids
                        return Result.success(Unit)
                    }
                } }
            })
        }
        val screenAnalytics = application.koin.get<ScreenAnalytics>()
        loadKoinModules(module {
            factory<ScreenAnalytics> {
                screenBindings++
                screenAnalytics
            }
        })
        create()
    }

    fun open(menu: ProfileMenu) = model<ProfileViewModel>(component).onProfileMenuClick(menu)

    fun restore(savedArchive: String = archive(), beforeCreate: () -> Unit = {}) {
        val current = component.childStack.value.active.instance.component
        val saved = Json.decodeFromString<SerializableContainer>(savedArchive)
        assertSame(current, component.childStack.value.active.instance.component)
        destroy()
        beforeCreate()
        create(saved)
    }

    fun archive(): String = Json.encodeToString(stateKeeper.save())

    fun pause() = lifecycle.pause()

    fun resume() = lifecycle.resume()

    fun recreateRetained() {
        val saved = Json.decodeFromString<SerializableContainer>(archive())
        lifecycle.destroy()
        create(saved, retained = instanceKeeper)
    }

    fun destroy() {
        if (::lifecycle.isInitialized) lifecycle.destroy()
        if (::instanceKeeper.isInitialized) instanceKeeper.destroy()
    }

    inline fun <reified T : InstanceKeeper.Instance> model(
        context: ComponentContext = component.childStack.value.active.instance.component,
    ): T = assertIs<T>(context.instanceKeeper.get(typeOf<T>()))

    fun assertNoBusinessWrites() {
        assertEquals(emptyList(), savedWeights)
        assertEquals(emptyList(), savedHeights)
        assertEquals(emptyList(), savedExperiences)
        assertEquals(emptyList(), savedGoals)
        assertEquals(emptyList(), savedExcludedMuscles)
        assertEquals(emptyList(), savedExcludedEquipments)
        assertEquals(emptyList(), notifications)
    }

    private fun create(saved: SerializableContainer? = null, retained: InstanceKeeperDispatcher? = null) {
        lifecycle = LifecycleRegistry()
        stateKeeper = StateKeeperDispatcher(saved)
        instanceKeeper = retained ?: InstanceKeeperDispatcher()
        component = ProfileComponent(
            initial = initial,
            componentContext = DefaultComponentContext(lifecycle, stateKeeper, instanceKeeper),
            close = { closeCalls++ },
            toDebug = { error("Unused debug navigation") },
        )
        lifecycle.resume()
    }

    companion object {
        private val CREATED_AT = LocalDateTime(2026, 1, 1, 12, 0)

        fun user(
            weight: Float = 70f,
            height: Int = 170,
            experience: ExperienceEnum = ExperienceEnum.BEGINNER,
        ): User = User(
            id = "user-catalog-only",
            name = "User",
            email = "user@example.com",
            experience = experience,
            weight = weight,
            height = height,
            createdAt = CREATED_AT,
            role = RoleEnum.DEFAULT,
            stats = UserStats(0, Duration.ZERO, 0f, 0),
        )

        fun goal(primary: GoalPrimaryGoalEnum = GoalPrimaryGoalEnum.BUILD_MUSCLE): Goal = Goal(
            primaryGoal = primary,
            secondaryGoal = GoalSecondaryGoalEnum.CONSISTENCY,
            target = LocalDateTime(2027, 3, 1, 12, 0),
            personalizations = listOf(PersonalizationKeyEnum.LOW_FREQUENCY),
            confidence = 1.0,
            createdAt = CREATED_AT,
            updatedAt = CREATED_AT,
            lastConfirmedAt = null,
        )

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

    class View(val navigation: NavigationContext, val flow: FlowContext?) {
        var closes = 0
    }
}
