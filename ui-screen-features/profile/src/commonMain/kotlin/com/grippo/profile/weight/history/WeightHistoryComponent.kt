package com.grippo.profile.weight.history

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class WeightHistoryComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<WeightHistoryDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        WeightHistoryViewModel(
            dialogController = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: WeightHistoryDirection) {
        when (direction) {
            WeightHistoryDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        WeightHistoryScreen(state.value, loaders.value, viewModel)
    }
}