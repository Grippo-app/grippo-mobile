package com.grippo.profile.goal

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileGoalComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
) : BaseComponent<ProfileGoalDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileGoalViewModel(
            analytics = getKoin().get(),
            goalFeature = getKoin().get(),
            dialogController = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
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
        ProfileGoalScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT = PersistenceKey(
            key = "ProfileGoalDraft",
            serializer = ProfileGoalState.serializer(),
        )
    }
}
