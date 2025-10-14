package com.grippo.filter.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.filters.FilterValue

public class FilterPickerComponent(
    componentContext: ComponentContext,
    private val initial: List<FilterValue>,
    private val onResult: (values: List<FilterValue>) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<FilterPickerDirection>(componentContext) {

    override val viewModel: FilterPickerViewModel = componentContext.retainedInstance {
        FilterPickerViewModel(
            initial = initial,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: FilterPickerDirection) {
        when (direction) {
            is FilterPickerDirection.BackWithResult -> onResult.invoke(direction.values)
            FilterPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        FilterPickerScreen(state.value, loaders.value, viewModel)
    }
}