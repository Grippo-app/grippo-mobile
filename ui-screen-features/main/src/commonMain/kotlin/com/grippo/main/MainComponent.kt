package com.grippo.main

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.children.ChildNavState
import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.router.pages.Pages
import com.arkivanov.decompose.router.pages.PagesNavigation
import com.arkivanov.decompose.router.pages.childPages
import com.arkivanov.decompose.router.pages.select
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.exercises.ExercisesComponent
import com.grippo.home.HomeRootComponent
import com.grippo.profile.ProfileComponent
import com.grippo.screen.api.HomeRouter
import com.grippo.screen.api.ProfileRouter
import com.grippo.screen.api.TrainingsRouter
import com.grippo.trainings.TrainingsRootComponent

public class MainComponent(
    componentContext: ComponentContext,
    private val toTraining: (StageState) -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toExcludedMuscles: () -> Unit,
    private val toExperience: () -> Unit,
    private val toDebug: () -> Unit,
    private val toTrainings: () -> Unit,
    private val toSettings: () -> Unit,
    private val toSocial: () -> Unit,
    private val toGoal: () -> Unit,
    private val close: () -> Unit,
) : BaseComponent<MainDirection>(componentContext) {

    override val viewModel: MainViewModel = componentContext.retainedInstance { MainViewModel() }

    private val pagesNavigation = PagesNavigation<MainRouter>()

    internal val childPages: Value<ChildPages<MainRouter, Child>> = childPages(
        source = pagesNavigation,
        serializer = MainRouter.serializer(),
        initialPages = {
            Pages(
                items = listOf(
                    MainRouter.Home,
                    MainRouter.Calendar,
                    MainRouter.Exercises,
                    MainRouter.Profile,
                ),
                selectedIndex = 0,
            )
        },
        key = "MainComponent",
        pageStatus = { index, pages ->
            // All four tabs stay alive — the selected one is RESUMED, the rest
            // CREATED. Never DESTROYED, so tab state survives switching.
            if (index == pages.selectedIndex) {
                ChildNavState.Status.RESUMED
            } else {
                ChildNavState.Status.CREATED
            }
        },
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private fun createChild(router: MainRouter, context: ComponentContext): Child {
        return when (router) {
            MainRouter.Home -> Child.Home(
                HomeRootComponent(
                    componentContext = context,
                    initial = HomeRouter.Home,
                    toBody = toWeightHistory,
                    toMissingEquipment = toMissingEquipment,
                    toExcludedMuscles = toExcludedMuscles,
                    toExperience = toExperience,
                    toDebug = toDebug,
                    toTraining = toTraining,
                    toTrainings = toTrainings,
                    toSettings = toSettings,
                    toSocial = toSocial,
                    toGoal = toGoal,
                    close = close,
                )
            )

            MainRouter.Calendar -> Child.Calendar(
                TrainingsRootComponent(
                    componentContext = context,
                    initial = TrainingsRouter.Trainings,
                    toTraining = toTraining,
                    close = {},
                )
            )

            MainRouter.Exercises -> Child.Exercises(
                ExercisesComponent(componentContext = context)
            )

            MainRouter.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = context,
                    initial = ProfileRouter.Body,
                    close = {},
                )
            )
        }
    }

    override suspend fun eventListener(direction: MainDirection) {
        when (direction) {
            is MainDirection.SelectTab -> pagesNavigation.select(direction.index)
            MainDirection.StartTraining -> toTraining(StageState.Add)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MainScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        internal data class Home(override val component: HomeRootComponent) : Child(component)
        internal data class Calendar(override val component: TrainingsRootComponent) : Child(component)
        internal data class Exercises(override val component: ExercisesComponent) : Child(component)
        internal data class Profile(override val component: ProfileComponent) : Child(component)
    }
}
