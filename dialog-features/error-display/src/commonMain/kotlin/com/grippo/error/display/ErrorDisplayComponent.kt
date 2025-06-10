package com.grippo.error.display

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform

public class ErrorDisplayComponent(
    private val title: String,
    private val description: String,
    private val back: () -> Unit,
    componentContext: ComponentContext,
) : BaseComponent<ErrorDisplayDirection>(componentContext) {

    override val viewModel: ErrorDisplayViewModel = componentContext.retainedInstance {
        ErrorDisplayViewModel(
            title = title,
            description = description
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::dismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ErrorDisplayDirection) {
        when (direction) {
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