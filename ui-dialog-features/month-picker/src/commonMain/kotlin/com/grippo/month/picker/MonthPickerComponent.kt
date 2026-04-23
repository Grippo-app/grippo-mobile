package com.grippo.month.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.toolkit.date.utils.DateRange

public class MonthPickerComponent(
    componentContext: ComponentContext,
    private val title: String,
    private val initial: DateTimeFormatState,
    private val limitations: DateRange,
    private val onResult: (value: DateTimeFormatState) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<MonthPickerDirection>(componentContext) {

    override val viewModel: MonthPickerViewModel = componentContext.retainedInstance {
        MonthPickerViewModel(
            initial = initial,
            limitations = limitations,
            title = title
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: MonthPickerDirection) {
        when (direction) {
            is MonthPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            MonthPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MonthPickerScreen(state.value, loaders.value, viewModel)
    }
}
