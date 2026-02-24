package com.grippo.profile.social

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileSocialComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
) : BaseComponent<ProfileSocialDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileSocialViewModel(
            browserRedirector = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileSocialDirection) {
        when (direction) {
            ProfileSocialDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileSocialScreen(state = state.value, loaders = loaders.value, contract = viewModel)
    }
}
