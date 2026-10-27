package com.grippo.error.display

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.error.AppErrorState
import com.grippo.dialog.api.DialogConfig

public class ErrorDisplayComponent(
    private val error: AppErrorState,
    mode: DialogConfig.ErrorDisplay.Mode,
    private val onRetry: () -> Unit,
    private val back: () -> Unit,
    componentContext: ComponentContext,
) : BaseComponent<ErrorDisplayDirection>(componentContext) {

    override val viewModel: ErrorDisplayViewModel = componentContext.retainedInstance {
        ErrorDisplayViewModel(
            error = error,
            mode = ErrorDisplayModeState.of(mode),
            analytics = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ErrorDisplayDirection) {
        when (direction) {
            ErrorDisplayDirection.Retry -> onRetry.invoke()
            ErrorDisplayDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ErrorDisplayScreen(state.value, loaders.value, viewModel)
    }
}