package com.grippo.home

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
import com.grippo.home.statistics.StatisticsComponent
import com.grippo.home.trainings.TrainingsComponent
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter

public class BottomNavigationComponent(
    componentContext: ComponentContext,
) : BaseComponent<BottomNavigationDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Profile(override val component: ProfileComponent) : Child(component)
        data class Trainings(override val component: TrainingsComponent) : Child(component)
        data class Statistics(override val component: StatisticsComponent) : Child(component)
    }

    override val viewModel: BottomNavigationViewModel = componentContext.retainedInstance {
        BottomNavigationViewModel()
    }

    override suspend fun eventListener(direction: BottomNavigationDirection) {
        when (direction) {
            BottomNavigationDirection.Trainings -> navigation.select(0)
            BottomNavigationDirection.Statistics -> navigation.select(1)
            BottomNavigationDirection.Profile -> navigation.select(2)
        }
    }

    private val navigation = PagesNavigation<BottomNavigationRouter>()

    internal val childPages: Value<ChildPages<BottomNavigationRouter, Child>> = childPages(
        source = navigation,
        serializer = BottomNavigationRouter.serializer(),
        initialPages = {
            Pages(
                items = listOf(
                    BottomNavigationRouter.Trainings,
                    BottomNavigationRouter.Statistics,
                    BottomNavigationRouter.Profile,
                ),
                selectedIndex = viewModel.state.value.selectedIndex
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

            is BottomNavigationRouter.Statistics -> Child.Statistics(
                StatisticsComponent(
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