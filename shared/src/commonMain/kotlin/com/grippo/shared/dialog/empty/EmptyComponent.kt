package com.grippo.shared.dialog.empty

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.BaseViewModel
import com.grippo.core.models.BaseDirection
import com.grippo.core.models.BaseLoader

internal class EmptyComponent(
    componentContext: ComponentContext,
) : BaseComponent<BaseDirection>(componentContext) {

    internal class EmptyViewModel : BaseViewModel<Unit, BaseDirection, BaseLoader>(Unit)

    override val viewModel = componentContext.retainedInstance {
        EmptyViewModel()
    }

    override suspend fun eventListener(direction: BaseDirection) {
    }

    @Composable
    override fun Render() {
    }
}