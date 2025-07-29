package com.grippo.period.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.state.datetime.PeriodState

public class PeriodPickerComponent(
    componentContext: ComponentContext,
    private val initial: PeriodState,
    private val onResult: (value: PeriodState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<PeriodPickerDirection>(componentContext) {

    override val viewModel: PeriodPickerViewModel = componentContext.retainedInstance {
        PeriodPickerViewModel(initial)
    }

    private val backCallback = BackCallback(onBack = viewModel::dismiss)

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