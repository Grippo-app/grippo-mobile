package com.grippo.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
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
    initial: BottomNavigationRouter,
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toExerciseLibrary: () -> Unit,
    private val toDebug: () -> Unit,
    private val toWorkout: () -> Unit,
    private val toSystemSettings: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<BottomNavigationDirection>(componentContext) {

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Profile(override val component: HomeProfileComponent) : Child(component)
        data class Trainings(override val component: HomeTrainingsComponent) : Child(component)
        data class Statistics(override val component: HomeStatisticsComponent) : Child(component)
    }

    override val viewModel: BottomNavigationViewModel = componentContext.retainedInstance {
        BottomNavigationViewModel(
            initial = initial
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::back)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: BottomNavigationDirection) {
        when (direction) {
            BottomNavigationDirection.Trainings -> navigation.replaceAll(BottomNavigationRouter.Trainings)
            BottomNavigationDirection.Statistics -> navigation.replaceAll(BottomNavigationRouter.Statistics)
            BottomNavigationDirection.Profile -> navigation.replaceAll(BottomNavigationRouter.Profile)
            BottomNavigationDirection.Back -> back.invoke()
        }
    }

    private val navigation = StackNavigation<BottomNavigationRouter>()

    internal val childStack: Value<ChildStack<BottomNavigationRouter, Child>> = childStack(
        source = navigation,
        serializer = BottomNavigationRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "AuthProcessComponent",
        handleBackButton = true,
        childFactory = ::createChild,
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
                    back = back
                ),
            )

            BottomNavigationRouter.Profile -> Child.Profile(
                HomeProfileComponent(
                    componentContext = context,
                    toExcludedMuscles = toExcludedMuscles,
                    toExerciseLibrary = toExerciseLibrary,
                    toMissingEquipment = toMissingEquipment,
                    toWeightHistory = toWeightHistory,
                    toWorkout = toWorkout,
                    toDebug = toDebug,
                    toSystemSettings = toSystemSettings,
                    back = back
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        BottomNavigationScreen(this, state.value, loaders.value, viewModel)
    }
}