package com.grippo.confirm.training.completion

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import kotlin.time.Duration

public class ConfirmTrainingCompletionComponent(
    componentContext: ComponentContext,
    private val value: Duration,
    private val onResult: (Duration) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ConfirmTrainingCompletionDirection>(componentContext) {

    override val viewModel: ConfirmTrainingCompletionViewModel = componentContext.retainedInstance {
        ConfirmTrainingCompletionViewModel(
            duration = value,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ConfirmTrainingCompletionDirection) {
        when (direction) {
            ConfirmTrainingCompletionDirection.Confirm -> onResult.invoke(value)
            ConfirmTrainingCompletionDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ConfirmTrainingCompletionScreen(state.value, loaders.value, viewModel)
    }
}
