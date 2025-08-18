package com.grippo.training.exercise

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform

internal class ExerciseComponent(
    componentContext: ComponentContext,
    private val id: String?,
    private val back: () -> Unit,
) : BaseComponent<ExerciseDirection>(componentContext) {

    override val viewModel: ExerciseViewModel = componentContext.retainedInstance {
        ExerciseViewModel(
            id = id,
            dialogController = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ExerciseDirection) {
        when (direction) {
            ExerciseDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ExerciseScreen(state.value, loaders.value, viewModel)
    }
}
