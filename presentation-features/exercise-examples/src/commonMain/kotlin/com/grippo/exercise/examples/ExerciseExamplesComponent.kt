package com.grippo.exercise.examples

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.exercise.examples.list.ExerciseExampleListComponent
import com.grippo.presentation.api.exercise.examples.ExerciseExamplesRouter

public class ExerciseExamplesComponent(
    initial: ExerciseExamplesRouter,
    componentContext: ComponentContext,
    private val close: () -> Unit
) : BaseComponent<ExerciseExamplesDirection>(componentContext) {

    override val viewModel: ExerciseExamplesViewModel = componentContext.retainedInstance {
        ExerciseExamplesViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseExamplesDirection) {
        when (direction) {
            ExerciseExamplesDirection.Back -> close.invoke()
        }
    }

    private val navigation = StackNavigation<ExerciseExamplesRouter>()

    internal val childStack: Value<ChildStack<ExerciseExamplesRouter, Child>> = childStack(
        source = navigation,
        serializer = ExerciseExamplesRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "ExerciseExamplesComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: ExerciseExamplesRouter, context: ComponentContext): Child {
        return when (router) {
            ExerciseExamplesRouter.List -> Child.List(
                ExerciseExampleListComponent(
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
        ExerciseExamplesScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class List(override val component: ExerciseExampleListComponent) : Child(component)
    }
}
