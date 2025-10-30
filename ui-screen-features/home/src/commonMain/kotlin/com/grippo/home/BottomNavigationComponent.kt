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
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.home.statistics.HomeStatisticsComponent
import com.grippo.home.trainings.HomeTrainingsComponent
import com.grippo.screen.api.BottomNavigationRouter

public class BottomNavigationComponent(
    initial: BottomNavigationRouter,
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toDebug: () -> Unit,
    private val toTraining: (stage: StageState) -> Unit,
    private val close: () -> Unit,
) : BaseComponent<BottomNavigationDirection>(componentContext) {

    override val viewModel: BottomNavigationViewModel = componentContext.retainedInstance {
        BottomNavigationViewModel(
            initial = initial,
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: BottomNavigationDirection) {
        when (direction) {
            BottomNavigationDirection.Trainings -> navigation.replaceAll(BottomNavigationRouter.Trainings)
            BottomNavigationDirection.Statistics -> navigation.replaceAll(BottomNavigationRouter.Statistics)
            BottomNavigationDirection.Back -> close.invoke()
            BottomNavigationDirection.ToExcludedMuscles -> toExcludedMuscles.invoke()
            BottomNavigationDirection.ToMissingEquipment -> toMissingEquipment.invoke()
            BottomNavigationDirection.ToWeightHistory -> toWeightHistory.invoke()
            BottomNavigationDirection.ToDebug -> toDebug.invoke()
            BottomNavigationDirection.ToAddTraining -> toTraining.invoke(StageState.Add)
            BottomNavigationDirection.ToDraftTraining -> toTraining.invoke(StageState.Draft)
            is BottomNavigationDirection.ToEditTraining -> toTraining.invoke(
                StageState.Edit(direction.id)
            )
        }
    }

    private val navigation = StackNavigation<BottomNavigationRouter>()

    internal val childStack: Value<ChildStack<BottomNavigationRouter, Child>> = childStack(
        source = navigation,
        serializer = BottomNavigationRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "BottomNavigationComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: BottomNavigationRouter, context: ComponentContext): Child {
        return when (router) {
            is BottomNavigationRouter.Trainings -> Child.Trainings(
                HomeTrainingsComponent(
                    componentContext = context,
                    toEditTraining = viewModel::toEditTraining,
                    toExcludedMuscles = viewModel::toExcludedMuscles,
                    toMissingEquipment = viewModel::toMissingEquipment,
                    toWeightHistory = viewModel::toWeightHistory,
                    toAddTraining = viewModel::toAddTraining,
                    toDebug = viewModel::toDebug,
                    back = viewModel::onBack
                ),
            )

            is BottomNavigationRouter.Statistics -> Child.Statistics(
                HomeStatisticsComponent(
                    componentContext = context,
                    back = viewModel::onBack
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

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Trainings(override val component: HomeTrainingsComponent) : Child(component)
        data class Statistics(override val component: HomeStatisticsComponent) : Child(component)
    }
}