package com.grippo.shared.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.debug.DebugComponent
import com.grippo.design.core.AppTheme
import com.grippo.exercises.ExercisesRootComponent
import com.grippo.home.HomeRootComponent
import com.grippo.main.MainComponent
import com.grippo.profile.ProfileComponent
import com.grippo.screen.api.AuthRouter
import com.grippo.screen.api.ExercisesRouter
import com.grippo.screen.api.HomeRouter
import com.grippo.screen.api.ProfileRouter
import com.grippo.screen.api.RootRouter
import com.grippo.screen.api.RootRouter.Auth
import com.grippo.screen.api.TrainingRouter
import com.grippo.screen.api.TrainingsRouter
import com.grippo.shared.dialog.DialogComponent
import com.grippo.shared.root.RootComponent.Child.Authorization
import com.grippo.shared.root.RootComponent.Child.Debug
import com.grippo.shared.root.RootComponent.Child.Profile
import com.grippo.shared.root.RootComponent.Child.Training
import com.grippo.shared.root.RootComponent.Child.Trainings
import com.grippo.toolkit.date.utils.DateFormatting
import com.grippo.toolkit.localization.AppLocale
import com.grippo.toolkit.theme.AppTheme
import com.grippo.training.TrainingComponent
import com.grippo.trainings.TrainingsRootComponent

public class RootComponent(
    componentContext: ComponentContext,
    private val close: () -> Unit,
    deeplink: String? = null,
) : BaseComponent<RootDirection>(componentContext) {

    private val dialogComponent = DialogComponent(componentContext)

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel(
            deeplink = deeplink,
            authorizationFeature = getKoin().get(),
        )
    }

    private val navigation = StackNavigation<RootRouter>()

    internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
        source = navigation,
        serializer = RootRouter.serializer(),
        initialConfiguration = Auth(AuthRouter.Splash),
        handleBackButton = true,
        key = "RootComponent",
        childFactory = ::createChild,
    )

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    public fun handleDeeplink(deeplink: String) {
        if (childStack.value.active.configuration is RootRouter.Home) {
            viewModel.applyDeeplink(deeplink)
        } else {
            viewModel.enqueueDeeplink(deeplink)
        }
    }

    override suspend fun eventListener(direction: RootDirection) {
        when (direction) {
            RootDirection.Login -> {
                if (childStack.value.active.configuration !is Auth) {
                    navigation.replaceAll(Auth(AuthRouter.AuthProcess()))
                }
            }

            RootDirection.Home -> navigation.replaceAll(
                RootRouter.Home
            )

            RootDirection.Debug -> navigation.push(
                RootRouter.Debug
            )

            is RootDirection.Training -> navigation.push(
                RootRouter.Training(direction.stage)
            )

            is RootDirection.Exercises -> navigation.push(
                RootRouter.Exercises(direction.route)
            )

            RootDirection.WeightHistory -> navigation.push(
                RootRouter.Profile(ProfileRouter.Body)
            )

            RootDirection.Goal -> navigation.push(
                RootRouter.Profile(ProfileRouter.Goal)
            )

            RootDirection.Trainings -> navigation.push(
                RootRouter.Trainings
            )

            RootDirection.Back -> navigation.pop()
            RootDirection.Close -> close.invoke()
        }
    }

    private fun createChild(router: RootRouter, context: ComponentContext): Child {
        return when (router) {
            is Auth -> Authorization(
                AuthComponent(
                    componentContext = context,
                    initial = router.value,
                    toHome = viewModel::toHome,
                    close = viewModel::onClose
                ),
            )

            RootRouter.Home -> Child.Home(
                MainComponent(
                    componentContext = context,
                    createHome = { childContext, back ->
                        HomeRootComponent(
                            componentContext = childContext,
                            initial = HomeRouter.Home,
                            toTrainings = viewModel::toTrainings,
                            toGoal = viewModel::toGoal,
                            close = back,
                        )
                    },
                    createCalendar = { childContext, back ->
                        TrainingsRootComponent(
                            componentContext = childContext,
                            initial = TrainingsRouter.Trainings,
                            toTraining = viewModel::toTraining,
                            close = back,
                        )
                    },
                    createLibrary = { childContext, back ->
                        ExercisesRootComponent(
                            componentContext = childContext,
                            initial = ExercisesRouter.Library(ExercisesRouter.Mode.Browse),
                            close = back,
                        )
                    },
                    createProfile = { childContext, back ->
                        ProfileComponent(
                            componentContext = childContext,
                            initial = ProfileRouter.Menu,
                            toDebug = viewModel::toDebug,
                            close = back,
                        )
                    },
                    toTraining = viewModel::toTraining,
                    toGoal = viewModel::toGoal,
                    close = viewModel::onClose
                )
            )

            RootRouter.Trainings -> Trainings(
                TrainingsRootComponent(
                    componentContext = context,
                    initial = TrainingsRouter.Trainings,
                    toTraining = viewModel::toTraining,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Profile -> Profile(
                ProfileComponent(
                    componentContext = context,
                    initial = router.value,
                    toDebug = viewModel::toDebug,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Debug -> Debug(
                DebugComponent(
                    componentContext = context,
                    close = viewModel::onBack,
                )
            )

            is RootRouter.Training -> Training(
                TrainingComponent(
                    componentContext = context,
                    initial = TrainingRouter.Recording(router.stage),
                    toExercises = viewModel::toExercises,
                    toGoal = viewModel::toGoal,
                    close = viewModel::onBack
                )
            )

            is RootRouter.Exercises -> Child.Exercises(
                ExercisesRootComponent(
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

        val systemIsDark = AppTheme.current
        val systemLocaleTag = AppLocale.current

        LaunchedEffect(systemLocaleTag) {
            DateFormatting.install(systemLocaleTag)
        }

        AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
            RootScreen(this, state.value, loaders.value, viewModel)
            dialogComponent.Render()
        }
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) :
            Child(component)

        public data class Trainings(override val component: TrainingsRootComponent) :
            Child(component)

        public data class Home(override val component: MainComponent) :
            Child(component)

        public data class Profile(override val component: ProfileComponent) :
            Child(component)

        public data class Debug(override val component: DebugComponent) :
            Child(component)

        public data class Training(override val component: TrainingComponent) :
            Child(component)

        public data class Exercises(override val component: ExercisesRootComponent) :
            Child(component)
    }
}
