package com.grippo.home.bottom.navigation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.home.profile.ProfileComponent
import com.grippo.home.trainings.TrainingsComponent
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
            BottomNavigationDirection.Profile -> navigation.select(0)
            BottomNavigationDirection.Trainings -> navigation.select(1)
        }
    }

    private val navigation = PagesNavigation<BottomNavigationRouter>()

    internal val childPages: Value<ChildPages<BottomNavigationRouter, Child>> = childPages(
        source = navigation,
        serializer = BottomNavigationRouter.serializer(),
        initialPages = {
            Pages<BottomNavigationRouter>(
                items = listOf(
                    BottomNavigationRouter.Trainings,
                    BottomNavigationRouter.Profile,
                ),
                selectedIndex = 0
            )
        },
        key = "BottomNavigationComponent",
        handleBackButton = true,
        childFactory = ::createChild
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
        BottomNavigationScreen(childPages, state.value, loaders.value, viewModel)
    }
}