package com.grippo.home

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.stage.StageState
import com.grippo.home.trainings.HomeTrainingsComponent
import com.grippo.screen.api.HomeRouter

public class HomeComponent(
    initial: HomeRouter,
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toDebug: () -> Unit,
    private val toTraining: (stage: StageState) -> Unit,
    private val close: () -> Unit,
) : BaseComponent<HomeDirection>(componentContext) {

    override val viewModel: HomeViewModel = componentContext.retainedInstance {
        HomeViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeDirection) {
        when (direction) {
            HomeDirection.Back -> close.invoke()
            HomeDirection.ToExcludedMuscles -> toExcludedMuscles.invoke()
            HomeDirection.ToMissingEquipment -> toMissingEquipment.invoke()
            HomeDirection.ToWeightHistory -> toWeightHistory.invoke()
            HomeDirection.ToDebug -> toDebug.invoke()
            HomeDirection.ToAddTraining -> toTraining.invoke(StageState.Add)
            HomeDirection.ToDraftTraining -> toTraining.invoke(StageState.Draft)
            is HomeDirection.ToEditTraining -> toTraining.invoke(
                StageState.Edit(direction.id)
            )
        }
    }

    private val navigation = StackNavigation<HomeRouter>()

    internal val childStack: Value<ChildStack<HomeRouter, Child>> =
        childStack(
            source = navigation,
            serializer = HomeRouter.serializer(),
            initialStack = { listOf(initial) },
            key = "HomeComponent",
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        router: HomeRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            is HomeRouter.Trainings -> Child.Trainings(
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
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Trainings(override val component: HomeTrainingsComponent) : Child(component)
    }
}
