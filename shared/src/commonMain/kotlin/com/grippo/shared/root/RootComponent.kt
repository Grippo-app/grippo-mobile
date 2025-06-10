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
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.design.core.AppTheme
import com.grippo.home.BottomNavigationComponent
import com.grippo.presentation.api.RootRouter
import com.grippo.presentation.api.profile.ProfileRouter
import com.grippo.profile.ProfileComponent
import com.grippo.shared.dialog.DialogComponent
import com.grippo.shared.root.RootComponent.Child.Authorization
import com.grippo.shared.root.RootComponent.Child.Home

public class RootComponent(
    componentContext: ComponentContext,
    private val onFinish: () -> Unit,
) : BaseComponent<RootDirection>(componentContext) {

    private val dialogComponent = DialogComponent(componentContext)

    public sealed class Child(public open val component: BaseComponent<*>) {
        public data class Authorization(override val component: AuthComponent) : Child(component)
        public data class Home(override val component: BottomNavigationComponent) : Child(component)
        public data class Profile(override val component: ProfileComponent) : Child(component)
    }

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel(
            authorizationFeature = getKoin().get()
        )
    }

    private val navigation = StackNavigation<RootRouter>()
    internal val childStack: Value<ChildStack<RootRouter, Child>> = childStack(
        source = navigation,
        serializer = RootRouter.serializer(),
        initialConfiguration = RootRouter.Auth,
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
                navigation.replaceAll(RootRouter.Auth)
            }

            RootDirection.Back -> onFinish.invoke()
        }
    }

    private fun createChild(router: RootRouter, context: ComponentContext): Child {
        return when (router) {
            RootRouter.Auth -> Authorization(
                AuthComponent(
                    componentContext = context,
                    toHome = { navigation.replaceAll(RootRouter.Home) },
                    onBack = { onFinish.invoke() }
                ),
            )

            RootRouter.Home -> Home(
                BottomNavigationComponent(
                    componentContext = context,
                    toWeightHistory = {},
                    toMissingEquipment = { navigation.push(RootRouter.Profile(ProfileRouter.Equipments)) },
                    toExcludedMuscles = { navigation.push(RootRouter.Profile(ProfileRouter.Muscles)) },
                    toExerciseLibrary = {},
                    onBack = { onFinish.invoke() }
                )
            )

            is RootRouter.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = context,
                    initial = router.value,
                    onBack = navigation::pop
                )
            )
        }
    }

    @Composable
    override fun Render() {
        AppTheme {
            val state = viewModel.state.collectAsStateMultiplatform()
            val loaders = viewModel.loaders.collectAsStateMultiplatform()
            RootScreen(this, state.value, loaders.value, viewModel)

            dialogComponent.Render()
        }
    }
}