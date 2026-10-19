package com.grippo.debug

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

public class DebugComponent(
    componentContext: ComponentContext,
    private val close: () -> Unit,
) : BaseComponent<DebugDirection>(componentContext) {

    override val viewModel: DebugViewModel = componentContext.retainedInstance {
        DebugViewModel(
            analytics = getKoin().get(),
            trainingFeature = getKoin().get(),
            generateTrainingUseCase = getKoin().get(),
            authorizationFeature = getKoin().get(),
            initialState = restoreState(DRAFT) ?: DebugState(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DebugDirection) {
        when (direction) {
            DebugDirection.Back -> close.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DebugScreen(state.value, loaders.value, viewModel)
    }

    private companion object {
        val DRAFT = PersistenceKey("DebugDraft", DebugState.serializer())
    }
}
