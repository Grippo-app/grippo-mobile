package com.grippo.shared.root

import com.arkivanov.decompose.ComponentContext
import androidx.compose.runtime.Composable
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent

public class RootComponent(
    componentContext: ComponentContext,
) : BaseComponent<RootDirection>(componentContext) {

    override val viewModel: RootViewModel = componentContext.retainedInstance {
        RootViewModel()
    }

    override suspend fun eventListener(rout: RootDirection) {
    }

    @Composable
    override fun Render() {
    }
}