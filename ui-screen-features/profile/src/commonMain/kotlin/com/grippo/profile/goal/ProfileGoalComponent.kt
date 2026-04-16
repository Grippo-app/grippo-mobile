package com.grippo.profile.goal

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileGoalComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
) : BaseComponent<ProfileGoalDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileGoalViewModel(
            goalFeature = getKoin().get()
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ProfileGoalDirection) {
        when (direction) {
            ProfileGoalDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileGoalScreen(state.value, loaders.value, viewModel)
    }
}
