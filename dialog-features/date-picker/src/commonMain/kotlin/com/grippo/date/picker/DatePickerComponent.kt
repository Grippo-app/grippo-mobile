package com.grippo.date.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import kotlinx.datetime.LocalDateTime

public class DatePickerComponent(
    componentContext: ComponentContext,
    private val initial: LocalDateTime,
    private val onResult: (value: LocalDateTime) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<DatePickerDirection>(componentContext) {

    override val viewModel: DatePickerViewModel = componentContext.retainedInstance {
        DatePickerViewModel(initial)
    }

    private val backCallback = BackCallback(onBack = viewModel::dismiss)

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