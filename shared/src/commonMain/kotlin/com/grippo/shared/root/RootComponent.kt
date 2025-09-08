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
import com.grippo.design.components.connection.snackbar.ConnectionSnackbar
import com.grippo.design.core.AppTheme
import com.grippo.exercise.examples.ExerciseExamplesComponent
import com.grippo.home.BottomNavigationComponent
import com.grippo.profile.ProfileComponent
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.BottomNavigationRouter
import com.grippo.screen.api.ExerciseExamplesRouter
import com.grippo.screen.api.ProfileRouter
import com.grippo.screen.api.RootRouter
import com.grippo.screen.api.SettingsRouter
import com.grippo.screen.api.TrainingRouter
import com.grippo.settings.SettingsComponent
import com.grippo.shared.dialog.DialogComponent
import com.grippo.shared.root.RootComponent.Child.Authorization
import com.grippo.shared.root.RootComponent.Child.Debug
import com.grippo.shared.root.RootComponent.Child.ExerciseExamples
import com.grippo.shared.root.RootComponent.Child.Home
import com.grippo.shared.root.RootComponent.Child.Profile
import com.grippo.shared.root.RootComponent.Child.Settings
import com.grippo.shared.root.RootComponent.Child.Training
import com.grippo.state.settings.ThemeState
import com.grippo.training.TrainingComponent

public class RootComponent(
    componentContext: ComponentContext,
    private val close: () -> Unit,
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

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: RootDirection) {
        when (direction) {
            RootDirection.Login -> if (childStack.value.active.instance !is Authorization) {
                navigation.replaceAll(RootRouter.Auth(AuthRouter.AuthProcess))
            }

            RootDirection.ToHome -> navigation.replaceAll(
                RootRouter.Home
            )

            RootDirection.ToProfile -> navigation.push(
                RootRouter.Profile(ProfileRouter.WeightHistory)
            )

            RootDirection.ToDebug -> navigation.push(
                RootRouter.Debug
            )

            RootDirection.ToSettings -> navigation.push(
                RootRouter.Settings(SettingsRouter.System)
            )

            RootDirection.ToTraining -> navigation.push(
                RootRouter.Training
            )

            RootDirection.ToWeightHistory -> navigation.push(
                RootRouter.Profile(ProfileRouter.WeightHistory)
            )

            RootDirection.ToMissingEquipment -> navigation.push(
                RootRouter.Profile(ProfileRouter.Equipments)
            )

            RootDirection.ToExcludedMuscles -> navigation.push(
                RootRouter.Profile(ProfileRouter.Muscles)
            )

            RootDirection.ToExerciseExamples -> navigation.push(
                RootRouter.ExerciseExamples(ExerciseExamplesRouter.List)
            )

            RootDirection.ToSystemSettings -> navigation.push(
                RootRouter.Settings(SettingsRouter.System)
            )

            RootDirection.Back -> navigation.pop()
            RootDirection.Close -> close.invoke()
        }
    }

    private fun createChild(router: RootRouter, context: ComponentContext): Child {
        return when (router) {
            is RootRouter.Auth -> Authorization(
                AuthComponent(
                    componentContext = context,
                    initial = router.value,
                    toHome = viewModel::toHome,
                    close = viewModel::onClose
                ),
            )

            RootRouter.Home -> Home(
                BottomNavigationComponent(
                    componentContext = context,
                    initial = BottomNavigationRouter.Trainings,
                    toWeightHistory = viewModel::toWeightHistory,
                    toMissingEquipment = viewModel::toMissingEquipment,
                    toExcludedMuscles = viewModel::toExcludedMuscles,
                    toDebug = viewModel::toDebug,
                    toTraining = viewModel::toTraining,
                    toSystemSettings = viewModel::toSystemSettings,
                    toExerciseExamples = viewModel::toExerciseExamples,
                    close = viewModel::onClose
                )
            )

            is RootRouter.Profile -> Profile(
                ProfileComponent(
                    componentContext = context,
                    initial = router.value,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Debug -> Debug(
                DebugComponent(
                    componentContext = context,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Training -> Training(
                TrainingComponent(
                    componentContext = context,
                    initial = TrainingRouter.Setup,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Settings -> Settings(
                SettingsComponent(
                    componentContext = context,
                    initial = router.value,
                    close = viewModel::onBack,
                )
            )

            is RootRouter.ExerciseExamples -> ExerciseExamples(
                ExerciseExamplesComponent(
                    componentContext = context,
                    initial = router.value,
                    close = viewModel::onBack,
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
            ConnectionSnackbar(state = state.value.connection)
            dialogComponent.Render()
        }
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) :
            Child(component)

        public data class Home(override val component: BottomNavigationComponent) :
            Child(component)

        public data class Profile(override val component: ProfileComponent) :
            Child(component)

        public data class Debug(override val component: DebugComponent) :
            Child(component)

        public data class Settings(override val component: SettingsComponent) :
            Child(component)

        public data class Training(override val component: TrainingComponent) :
            Child(component)

        public data class ExerciseExamples(override val component: ExerciseExamplesComponent) :
            Child(component)
    }
}