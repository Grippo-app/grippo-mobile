package com.grippo.dialog.profile

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.profile.ProfileActivityMenu

public class ProfileComponent(
    componentContext: ComponentContext,
    private val onResult: (ProfileActivityMenu) -> Unit,
    private val close: () -> Unit,
) : BaseComponent<ProfileDirection>(componentContext) {

    override val viewModel: ProfileViewModel = componentContext.retainedInstance {
        ProfileViewModel(
            userFeature = getKoin().get(),
            authorizationFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileDirection) {
        when (direction) {
            is ProfileDirection.BackWithResult -> onResult.invoke(direction.item)
            ProfileDirection.Back -> close.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(state.value, loaders.value, viewModel)
    }
}