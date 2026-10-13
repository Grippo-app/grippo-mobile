package com.grippo.profile.body

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform

internal class ProfileBodyComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<ProfileBodyDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        ProfileBodyViewModel(
            analytics = getKoin().get(),
            dialogController = getKoin().get(),
            weightHistoryFeature = getKoin().get(),
            userFeature = getKoin().get(),
            updateWeightUseCase = getKoin().get(),
            stringProvider = getKoin().get(),
            notificationManager = getKoin().get(),
            initialState = restoreState(DRAFT),
        )
    }

    init {
        saveState(DRAFT) { viewModel.state.value }
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

    private companion object {
        val DRAFT = PersistenceKey("ProfileBodyDraft", ProfileBodyState.serializer())
    }
}
