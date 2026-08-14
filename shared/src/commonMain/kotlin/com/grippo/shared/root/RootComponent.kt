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
import com.grippo.main.MainComponent
import com.grippo.profile.ProfileComponent
import com.grippo.screen.api.AuthRouter
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
            authorizationFeature = getKoin().get(),
            connectivity = getKoin().get(),
            deeplink = deeplink,
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
        if (childStack.value.active.configuration is RootRouter.Main) {
            viewModel.applyDeeplink(deeplink)
        } else {
            viewModel.enqueueDeeplink(deeplink)
        }
    }

    override suspend fun eventListener(direction: RootDirection) {
        // Login only replaces to Auth when we are not already on the Authorization
        // child — preserves the prior active-guard semantics.
        if (direction == RootDirection.Login && childStack.value.active.instance is Authorization) return
        when (val nav = direction.toNav()) {
            is RootNav.Push -> navigation.push(nav.router)
            is RootNav.ReplaceAll -> navigation.replaceAll(nav.router)
            RootNav.Pop -> navigation.pop()
            RootNav.Close -> close.invoke()
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

            RootRouter.Main -> Child.Main(
                MainComponent(
                    componentContext = context,
                    toTraining = viewModel::toTraining,
                    toWeightHistory = viewModel::toWeightHistory,
                    toMissingEquipment = viewModel::toMissingEquipment,
                    toExcludedMuscles = viewModel::toExcludedMuscles,
                    toExperience = viewModel::toExperience,
                    toDebug = viewModel::toDebug,
                    toTrainings = viewModel::toTrainings,
                    toSettings = viewModel::toSettings,
                    toSocial = viewModel::toSocial,
                    toGoal = viewModel::toGoal,
                    close = viewModel::onClose,
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
                    close = viewModel::onBack,
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
            dialogComponent.Render()
        }
    }

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) :
            Child(component)

        public data class Trainings(override val component: TrainingsRootComponent) :
            Child(component)

        public data class Main(override val component: MainComponent) :
            Child(component)

        public data class Profile(override val component: ProfileComponent) :
            Child(component)

        public data class Debug(override val component: DebugComponent) :
            Child(component)

        public data class Training(override val component: TrainingComponent) :
            Child(component)
    }
}
