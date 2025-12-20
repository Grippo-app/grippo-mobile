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
import com.grippo.home.home.HomeComponent
import com.grippo.screen.api.HomeRouter

public class HomeRootComponent(
    initial: HomeRouter,
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toDebug: () -> Unit,
    private val toTraining: (stage: StageState) -> Unit,
    private val toTrainings: () -> Unit,
    private val close: () -> Unit,
) : BaseComponent<HomeRootDirection>(componentContext) {

    override val viewModel: HomeRootViewModel = componentContext.retainedInstance {
        HomeRootViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: HomeRootDirection) {
        when (direction) {
            HomeRootDirection.Back -> close.invoke()
            HomeRootDirection.ExcludedMuscles -> toExcludedMuscles.invoke()
            HomeRootDirection.MissingEquipment -> toMissingEquipment.invoke()
            HomeRootDirection.WeightHistory -> toWeightHistory.invoke()
            HomeRootDirection.Debug -> toDebug.invoke()
            HomeRootDirection.AddTraining -> toTraining.invoke(StageState.Add)
            HomeRootDirection.DraftTraining -> toTraining.invoke(StageState.Draft)
            HomeRootDirection.Trainings -> toTrainings.invoke()
        }
    }

    private val navigation = StackNavigation<HomeRouter>()

    internal val childStack: Value<ChildStack<HomeRouter, Child>> =
        childStack(
            source = navigation,
            serializer = HomeRouter.serializer(),
            initialStack = { listOf(initial) },
            key = "HomeRootComponent",
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        router: HomeRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            is HomeRouter.Home -> Child.Home(
                HomeComponent(
                    componentContext = context,
                    toExcludedMuscles = viewModel::toExcludedMuscles,
                    toMissingEquipment = viewModel::toMissingEquipment,
                    toWeightHistory = viewModel::toWeightHistory,
                    toAddTraining = viewModel::toAddTraining,
                    toDebug = viewModel::toDebug,
                    toTrainings = viewModel::toTrainings,
                    back = viewModel::onBack
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        HomeRootScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Home(override val component: HomeComponent) : Child(component)
    }
}
