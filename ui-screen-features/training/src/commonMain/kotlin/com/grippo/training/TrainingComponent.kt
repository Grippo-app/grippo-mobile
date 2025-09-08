package com.grippo.training

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.screen.api.TrainingRouter
import com.grippo.training.completed.TrainingCompletedComponent
import com.grippo.training.exercise.ExerciseComponent
import com.grippo.training.recording.TrainingRecordingComponent

public class TrainingComponent(
    componentContext: ComponentContext,
    initial: TrainingRouter,
    private val close: () -> Unit,
) : BaseComponent<TrainingDirection>(componentContext) {

    override val viewModel: TrainingViewModel = componentContext.retainedInstance {
        TrainingViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onClose)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingDirection) {
        when (direction) {
            TrainingDirection.ToRecording -> navigation.replaceAll(
                TrainingRouter.Recording
            )

            is TrainingDirection.ToExercise -> navigation.push(
                TrainingRouter.Exercise(direction.exercise)
            )

            is TrainingDirection.ToCompleted -> navigation.replaceAll(
                TrainingRouter.Completed(direction.exercises, direction.startAt)
            )

            TrainingDirection.Close -> close.invoke()
            TrainingDirection.Back -> navigation.pop()

        }
    }

    private val navigation = StackNavigation<TrainingRouter>()

    internal val childStack: Value<ChildStack<TrainingRouter, Child>> = childStack(
        source = navigation,
        serializer = TrainingRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "TrainingComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: TrainingRouter, context: ComponentContext): Child {
        return when (router) {
            TrainingRouter.Recording -> Child.Recording(
                TrainingRecordingComponent(
                    componentContext = context,
                    toCompleted = viewModel::toCompleted,
                    toExercise = viewModel::toExercise,
                    back = viewModel::onClose
                ),
            )

            is TrainingRouter.Exercise -> Child.Exercise(
                ExerciseComponent(
                    componentContext = context,
                    exercise = router.exercise,
                    back = viewModel::onBack,
                ),
            )

            is TrainingRouter.Completed -> Child.Completed(
                TrainingCompletedComponent(
                    componentContext = context,
                    exercises = router.exercises,
                    startAt = router.startAt,
                    back = viewModel::onClose
                ),
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TrainingScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Recording(override val component: TrainingRecordingComponent) :
            Child(component)

        data class Exercise(override val component: ExerciseComponent) :
            Child(component)

        data class Completed(override val component: TrainingCompletedComponent) :
            Child(component)
    }
}