package com.grippo.confirmation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

public class ConfirmationComponent(
    private val title: String,
    private val description: String?,
    private val onResult: () -> Unit,
    private val back: () -> Unit,
    componentContext: ComponentContext,
) : BaseComponent<ConfirmationDirection>(componentContext) {

    override val viewModel: ConfirmationViewModel = componentContext.retainedInstance {
        ConfirmationViewModel(
            title = title,
            description = description
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ConfirmationDirection) {
        when (direction) {
            ConfirmationDirection.Confirm -> onResult.invoke()
            ConfirmationDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ConfirmationScreen(state.value, loaders.value, viewModel)
    }
}
