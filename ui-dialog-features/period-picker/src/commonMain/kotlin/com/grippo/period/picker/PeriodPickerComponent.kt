package com.grippo.period.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.PeriodFormatState

public class PeriodPickerComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val initial: PeriodFormatState,
    private val onResult: (value: PeriodFormatState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<PeriodPickerDirection>(componentContext) {

    override val viewModel: PeriodPickerViewModel = componentContext.retainedInstance {
        PeriodPickerViewModel(
            initial = initial,
            title = title
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: PeriodPickerDirection) {
        when (direction) {
            is PeriodPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            PeriodPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        PeriodPickerScreen(state.value, loaders.value, viewModel)
    }
}
