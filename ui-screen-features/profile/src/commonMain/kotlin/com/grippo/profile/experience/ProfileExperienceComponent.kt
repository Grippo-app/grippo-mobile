package com.grippo.profile.experience

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileExperienceComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileExperienceDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileExperienceViewModel(
            userFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileExperienceDirection) {
        when (direction) {
            ProfileExperienceDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileExperienceScreen(state.value, loaders.value, viewModel)
    }
}
