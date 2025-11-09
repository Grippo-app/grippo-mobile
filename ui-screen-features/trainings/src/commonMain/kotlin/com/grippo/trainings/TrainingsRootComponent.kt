package com.grippo.trainings

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
import com.grippo.screen.api.TrainingsRouter

public class TrainingsRootComponent(
    initial: TrainingsRouter,
    componentContext: ComponentContext,
    private val toExcludedMuscles: () -> Unit,
    private val toMissingEquipment: () -> Unit,
    private val toWeightHistory: () -> Unit,
    private val toDebug: () -> Unit,
    private val toTraining: (stage: StageState) -> Unit,
    private val close: () -> Unit,
) : BaseComponent<TrainingsRootDirection>(componentContext) {

    override val viewModel: TrainingsRootViewModel = componentContext.retainedInstance {
        TrainingsRootViewModel(
            trainingFeature = getKoin().get(),
            dialogController = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingsRootDirection) {
        when (direction) {
            TrainingsRootDirection.Back -> close.invoke()
            TrainingsRootDirection.ToExcludedMuscles -> toExcludedMuscles.invoke()
            TrainingsRootDirection.ToMissingEquipment -> toMissingEquipment.invoke()
            TrainingsRootDirection.ToWeightHistory -> toWeightHistory.invoke()
            TrainingsRootDirection.ToDebug -> toDebug.invoke()
            TrainingsRootDirection.ToAddTraining -> toTraining.invoke(StageState.Add)
            TrainingsRootDirection.ToDraftTraining -> toTraining.invoke(StageState.Draft)
            is TrainingsRootDirection.ToEditTraining -> toTraining.invoke(
                StageState.Edit(direction.id)
            )
        }
    }

    private val navigation = StackNavigation<TrainingsRouter>()

    internal val childStack: Value<ChildStack<TrainingsRouter, Child>> =
        childStack(
            source = navigation,
            serializer = TrainingsRouter.serializer(),
            initialStack = { listOf(initial) },
            key = "TrainingsRootComponent",
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        router: TrainingsRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            is TrainingsRouter.Trainings -> Child.Trainings(
                TrainingsComponent(
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
        TrainingsRootScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Trainings(override val component: TrainingsComponent) : Child(component)
    }
}
