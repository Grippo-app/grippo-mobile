package com.grippo.training

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.presentation.api.trainings.TrainingRouter
import com.grippo.training.preferences.TrainingPreferencesComponent
import com.grippo.training.recording.TrainingRecordingComponent
import com.grippo.training.success.TrainingSuccessComponent

public class TrainingComponent(
    componentContext: ComponentContext,
    initial: TrainingRouter,
    private val back: () -> Unit,
) : BaseComponent<TrainingDirection>(componentContext) {

    override val viewModel: TrainingViewModel = componentContext.retainedInstance {
        TrainingViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TrainingDirection) {
        when (direction) {
            TrainingDirection.Back -> back.invoke()
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
            TrainingRouter.Preferences -> Child.Preferences(
                TrainingPreferencesComponent(
                    componentContext = context,
                    toRecording = { navigation.push(TrainingRouter.Recording) },
                    back = back
                ),
            )

            TrainingRouter.Recording -> Child.Recording(
                TrainingRecordingComponent(
                    componentContext = context,
                    toSuccess = { navigation.push(TrainingRouter.Success) },
                    back = back
                ),
            )

            TrainingRouter.Success -> Child.Success(
                TrainingSuccessComponent(
                    componentContext = context,
                    back = back
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
        data class Preferences(override val component: TrainingPreferencesComponent) :
            Child(component)

        data class Recording(override val component: TrainingRecordingComponent) : Child(component)
        data class Success(override val component: TrainingSuccessComponent) : Child(component)
    }
}