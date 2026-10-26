package com.grippo.language.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.settings.LanguageEnumState

public class LanguagePickerComponent(
    componentContext: ComponentContext,
    private val initial: LanguageEnumState,
    private val onResult: (value: LanguageEnumState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<LanguagePickerDirection>(componentContext) {

    override val viewModel: LanguagePickerViewModel = componentContext.retainedInstance {
        LanguagePickerViewModel(
            initial = initial,
            analytics = getKoin().get(),
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: LanguagePickerDirection) {
        when (direction) {
            is LanguagePickerDirection.BackWithResult -> onResult.invoke(direction.value)
            LanguagePickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        LanguagePickerScreen(state.value, loaders.value, viewModel)
    }
}
