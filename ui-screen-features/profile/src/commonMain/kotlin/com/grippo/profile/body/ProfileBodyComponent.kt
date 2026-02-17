package com.grippo.profile.body

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileBodyComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileBodyDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileBodyViewModel(
            dialogController = getKoin().get()
        )
    }

    override suspend fun eventListener(direction: ProfileBodyDirection) {
        when (direction) {
            ProfileBodyDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileBodyScreen(state.value, loaders.value, viewModel)
    }
}