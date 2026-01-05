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
import com.grippo.design.components.connection.snackbar.ConnectionSnackbar
import com.grippo.design.core.AppTheme
import com.grippo.home.HomeRootComponent
import com.grippo.profile.ProfileComponent
import com.grippo.screen.api.AuthRouter
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
) : BaseComponent<RootDirection>(componentContext) {

    private val dialogComponent = DialogComponent(componentContext)

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel(
            authorizationFeature = getKoin().get(),
            connectivity = getKoin().get()
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

    override suspend fun eventListener(direction: RootDirection) {
        when (direction) {
            RootDirection.Login -> if (childStack.value.active.instance !is Authorization) {
                navigation.replaceAll(Auth(AuthRouter.AuthProcess))
            }

            RootDirection.Home -> navigation.replaceAll(
                RootRouter.Home
            )

            RootDirection.Profile -> navigation.push(
                RootRouter.Profile(ProfileRouter.WeightHistory)
            )

            RootDirection.Debug -> navigation.push(
                RootRouter.Debug
            )

            is RootDirection.Training -> navigation.push(
                RootRouter.Training(direction.stage)
            )

            RootDirection.WeightHistory -> navigation.push(
                RootRouter.Profile(ProfileRouter.WeightHistory)
            )

            RootDirection.MissingEquipment -> navigation.push(
                RootRouter.Profile(ProfileRouter.Equipments)
            )

            RootDirection.ExcludedMuscles -> navigation.push(
                RootRouter.Profile(ProfileRouter.Muscles)
            )

            RootDirection.Experience -> navigation.push(
                RootRouter.Profile(ProfileRouter.Experience)
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
                HomeRootComponent(
                    componentContext = context,
                    initial = HomeRouter.Home,
                    toWeightHistory = viewModel::toWeightHistory,
                    toMissingEquipment = viewModel::toMissingEquipment,
                    toExcludedMuscles = viewModel::toExcludedMuscles,
                    toExperience = viewModel::toExperience,
                    toDebug = viewModel::toDebug,
                    toTraining = viewModel::toTraining,
                    toTrainings = viewModel::toTrainings,
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
                    initial = TrainingRouter.Recording(router.stage),
                    close = viewModel::onBack
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
            ConnectionSnackbar(state = state.value.connection)
            dialogComponent.Render()
        }
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) :
            Child(component)

        public data class Trainings(override val component: TrainingsRootComponent) :
            Child(component)

        public data class Home(override val component: HomeRootComponent) :
            Child(component)

        public data class Profile(override val component: ProfileComponent) :
            Child(component)

        public data class Debug(override val component: DebugComponent) :
            Child(component)

        public data class Training(override val component: TrainingComponent) :
            Child(component)
    }
}
