package com.grippo.exercises

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
import com.grippo.exercises.library.ExerciseLibraryComponent
import com.grippo.screen.api.ExercisesRouter

public class ExercisesRootComponent(
    initial: ExercisesRouter,
    componentContext: ComponentContext,
    private val close: () -> Unit,
) : BaseComponent<ExercisesRootDirection>(componentContext) {

    override val viewModel: ExercisesRootViewModel = componentContext.retainedInstance {
        ExercisesRootViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExercisesRootDirection) {
        when (direction) {
            ExercisesRootDirection.Back -> close.invoke()
        }
    }

    private val navigation = StackNavigation<ExercisesRouter>()

    internal val childStack: Value<ChildStack<ExercisesRouter, Child>> =
        childStack(
            source = navigation,
            serializer = ExercisesRouter.serializer(),
            initialStack = { listOf(initial) },
            key = "ExercisesRootComponent",
            handleBackButton = true,
            childFactory = ::createChild,
        )

    private fun createChild(
        router: ExercisesRouter,
        context: ComponentContext
    ): Child {
        return when (router) {
            is ExercisesRouter.Library -> Child.Library(
                ExerciseLibraryComponent(
                    componentContext = context,
                    mode = router.mode,
                    back = viewModel::onBack,
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExercisesRootScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Library(override val component: ExerciseLibraryComponent) : Child(component)
    }
}
