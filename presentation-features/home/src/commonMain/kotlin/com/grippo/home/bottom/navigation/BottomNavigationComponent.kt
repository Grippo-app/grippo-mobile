package com.grippo.home.bottom.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.home.profile.ProfileComponent
import com.grippo.home.trainings.TrainingsComponent
import com.grippo.presentation.api.auth.AuthRouter
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter

internal class BottomNavigationComponent(
    componentContext: ComponentContext,
) : BaseComponent<BottomNavigationDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Profile(override val component: ProfileComponent) : Child(component)
        data class Trainings(override val component: TrainingsComponent) : Child(component)
    }


    override val viewModel = componentContext.retainedInstance {
        BottomNavigationViewModel()
    }

    override suspend fun eventListener(direction: BottomNavigationDirection) {
        when (direction) {
            BottomNavigationDirection.Profile -> TODO()
            BottomNavigationDirection.Trainings -> TODO()
        }
    }

    private val navigation = StackNavigation<AuthRouter>()

    internal val childStack: Value<ChildStack<BottomNavigationRouter, Child>> = childStack(
        source = navigation,
        serializer = BottomNavigationRouter.serializer(),
        initialStack = { listOf(BottomNavigationRouter.Profile) },
        key = "AuthComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: BottomNavigationRouter, context: ComponentContext): Child {
        return when (router) {
            BottomNavigationRouter.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = context,
                ),
            )

            is BottomNavigationRouter.Trainings -> Child.Trainings(
                TrainingsComponent(
                    componentContext = context,
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        BottomNavigationScreen(state.value, loaders.value, viewModel)
    }
}