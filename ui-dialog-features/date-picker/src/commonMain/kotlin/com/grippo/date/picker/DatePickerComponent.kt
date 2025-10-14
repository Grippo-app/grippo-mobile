package com.grippo.date.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.date.utils.DateRange

public class DatePickerComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val initial: DateFormatState,
    private val limitations: DateRange,
    private val onResult: (value: DateFormatState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<DatePickerDirection>(componentContext) {

    override val viewModel: DatePickerViewModel = componentContext.retainedInstance {
        DatePickerViewModel(
            initial = initial,
            limitations = limitations,
            title = title
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DatePickerDirection) {
        when (direction) {
            is DatePickerDirection.BackWithResult -> onResult.invoke(direction.value)
            DatePickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DatePickerScreen(state.value, loaders.value, viewModel)
    }
}