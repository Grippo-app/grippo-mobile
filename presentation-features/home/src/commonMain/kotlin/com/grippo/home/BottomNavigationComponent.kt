package com.grippo.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.home.profile.HomeProfileComponent
import com.grippo.home.statistics.HomeStatisticsComponent
import com.grippo.home.trainings.HomeTrainingsComponent
import com.grippo.presentation.api.bottom.navigation.BottomNavigationRouter

public class BottomNavigationComponent(
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExerciseLibrary: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<BottomNavigationDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Profile(override val component: HomeProfileComponent) : Child(component)
        data class Trainings(override val component: HomeTrainingsComponent) : Child(component)
        data class Statistics(override val component: HomeStatisticsComponent) : Child(component)
    }

    override val viewModel: BottomNavigationViewModel = componentContext.retainedInstance {
        BottomNavigationViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: BottomNavigationDirection) {
        when (direction) {
            BottomNavigationDirection.Trainings -> navigation.select(0)
            BottomNavigationDirection.Statistics -> navigation.select(1)
            BottomNavigationDirection.Profile -> navigation.select(2)
            BottomNavigationDirection.Back -> back.invoke()
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
            is BottomNavigationRouter.Trainings -> Child.Trainings(
                HomeTrainingsComponent(
                    componentContext = context,
                    back = back
                ),
            )

            is BottomNavigationRouter.Statistics -> Child.Statistics(
                HomeStatisticsComponent(
                    componentContext = context,
                    back = { navigation.select(0) }
                ),
            )

            BottomNavigationRouter.Profile -> Child.Profile(
                HomeProfileComponent(
                    componentContext = context,
                    toExcludedMuscles = toExcludedMuscles,
                    toExerciseLibrary = toExerciseLibrary,
                    toMissingEquipment = toMissingEquipment,
                    toWeightHistory = toWeightHistory,
                    back = { navigation.select(1) }
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