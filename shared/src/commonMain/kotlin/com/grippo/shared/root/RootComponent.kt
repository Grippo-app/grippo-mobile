package com.grippo.shared.root

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.authorization.AuthComponent
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.debug.DebugComponent
import com.grippo.design.core.AppTheme
import com.grippo.home.BottomNavigationComponent
import com.grippo.presentation.api.RootRouter
import com.grippo.presentation.api.auth.AuthRouter
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter
import com.grippo.presentation.api.profile.ProfileRouter
import com.grippo.presentation.api.settings.SettingsRouter
import com.grippo.profile.ProfileComponent
import com.grippo.settings.SettingsComponent
import com.grippo.shared.dialog.DialogComponent
import com.grippo.shared.root.RootComponent.Child.Authorization
import com.grippo.shared.root.RootComponent.Child.Home
import com.grippo.state.settings.ThemeState

public class RootComponent(
    componentContext: ComponentContext,
    private val finish: () -> Unit,
) : BaseComponent<RootDirection>(componentContext) {

    private val dialogComponent = DialogComponent(componentContext)

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel(
            authorizationFeature = getKoin().get(),
            settingsFeature = getKoin().get(),
            connectivity = getKoin().get()
        )
    }

    private val navigation = StackNavigation<RootRouter>()
    internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
        source = navigation,
        serializer = RootRouter.serializer(),
        initialConfiguration = RootRouter.Auth(AuthRouter.Splash),
        handleBackButton = true,
        key = "RootComponent",
        childFactory = ::createChild,
    )

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: RootDirection) {
        when (direction) {
            RootDirection.Login -> if (childStack.value.active.instance !is Authorization) {
                navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
            }

            RootDirection.Back -> finish.invoke()
        }
    }

    private fun createChild(router: RootRouter, context: ComponentContext): Child {
        return when (router) {
            is RootRouter.Auth -> Authorization(
                AuthComponent(
                    componentContext = context,
                    initial = router.value,
                    toHome = { navigation.replaceAll(RootRouter.Home) },
                    back = finish
                ),
            )

            RootRouter.Home -> Home(
                BottomNavigationComponent(
                    componentContext = context,
                    initial = BottomNavigationRouter.Trainings,
                    toWeightHistory = { navigation.push(RootRouter.Profile(ProfileRouter.WeightHistory)) },
                    toMissingEquipment = { navigation.push(RootRouter.Profile(ProfileRouter.Equipments)) },
                    toExcludedMuscles = { navigation.push(RootRouter.Profile(ProfileRouter.Muscles)) },
                    toDebug = { navigation.push(RootRouter.Debug) },
                    toWorkout = { navigation.push(RootRouter.Workout) },
                    toSystemSettings = { navigation.push(RootRouter.Settings(SettingsRouter.System)) },
                    toExerciseLibrary = {},
                    back = finish
                )
            )

            is RootRouter.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = context,
                    initial = router.value,
                    back = navigation::pop
                )
            )

            is RootRouter.Debug -> Child.Debug(
                DebugComponent(
                    componentContext = context,
                    back = navigation::pop
                )
            )

            is RootRouter.Workout -> {
                // Handle Workout router if needed
                throw NotImplementedError("Workout router is not implemented yet")
            }

            is RootRouter.Settings -> Child.Settings(
                SettingsComponent(
                    componentContext = context,
                    initial = router.value,
                    back = navigation::pop,
                )
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()

        AppTheme(darkTheme = state.value.theme == ThemeState.DARK) {
            RootScreen(this, state.value, loaders.value, viewModel)
            dialogComponent.Render()
        }
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) : Child(component)
        public data class Home(override val component: BottomNavigationComponent) : Child(component)
        public data class Profile(override val component: ProfileComponent) : Child(component)
        public data class Debug(override val component: DebugComponent) : Child(component)
        public data class Settings(override val component: SettingsComponent) : Child(component)
    }
}