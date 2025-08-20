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
import com.grippo.presentation.api.trainings.TrainingRouter
import com.grippo.training.exercise.ExerciseComponent
import com.grippo.training.recording.TrainingRecordingComponent
import com.grippo.training.setup.TrainingSetupComponent
import com.grippo.training.success.TrainingSuccessComponent

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
            TrainingDirection.Close -> close.invoke()
            TrainingDirection.Back -> navigation.pop()
            TrainingDirection.ToRecording -> navigation.push(TrainingRouter.Recording)
            is TrainingDirection.ToExercise -> navigation.push(TrainingRouter.Exercise(direction.exercise))
            TrainingDirection.ToSuccess -> navigation.replaceAll(TrainingRouter.Success)
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
            TrainingRouter.Setup -> Child.Setup(
                TrainingSetupComponent(
                    componentContext = context,
                    toRecording = viewModel::toRecording,
                    back = viewModel::onClose
                ),
            )

            TrainingRouter.Recording -> Child.Recording(
                TrainingRecordingComponent(
                    componentContext = context,
                    toSuccess = viewModel::toSuccess,
                    toExercise = viewModel::toExercise,
                    back = viewModel::onBack
                ),
            )

            is TrainingRouter.Exercise -> Child.Exercise(
                ExerciseComponent(
                    componentContext = context,
                    exercise = router.exercise,
                    back = viewModel::onBack
                ),
            )

            TrainingRouter.Success -> Child.Success(
                TrainingSuccessComponent(
                    componentContext = context,
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
        data class Setup(override val component: TrainingSetupComponent) :
            Child(component)

        data class Recording(override val component: TrainingRecordingComponent) :
            Child(component)

        data class Exercise(override val component: ExerciseComponent) :
            Child(component)

        data class Success(override val component: TrainingSuccessComponent) :
            Child(component)
    }
}