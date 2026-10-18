package com.grippo.authorization.registration

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.models.PersistenceKey
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.EmailFormatState

internal class RegistrationComponent(
    componentContext: ComponentContext,
    email: String?,
    private val toCreateProfile: () -> Unit,
    private val toHome: () -> Unit,
    private val back: () -> Unit,
) : BaseComponent<RegistrationDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        RegistrationViewModel(
            analytics = getKoin().get(),
            registerUseCase = getKoin().get(),
            initialState = restoreState(DRAFT)
                ?: RegistrationState(email = EmailFormatState.of(email)),
        )
    }

    init {
        saveState(DRAFT) { viewModel.state.value }
        backHandler.register(BackCallback(onBack = viewModel::onBack))
    }

    override suspend fun eventListener(direction: RegistrationDirection) {
        when (direction) {
            RegistrationDirection.CreateProfile -> toCreateProfile.invoke()
            RegistrationDirection.Home -> toHome.invoke()
            RegistrationDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        RegistrationScreen(
            state = state.value,
            loaders = loaders.value,
            contract = viewModel,
        )
    }

    private companion object {
        val DRAFT = PersistenceKey(
            key = "RegistrationDraft",
            serializer = RegistrationState.serializer(),
        )
    }
}
