package com.grippo.list.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.item.ItemState

public class ListPickerComponent(
    componentContext: ComponentContext,
    title: String,
    items: List<ItemState>,
    private val onResult: (id: String) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<ListPickerDirection>(componentContext) {

    override val viewModel: ListPickerViewModel = componentContext.retainedInstance {
        ListPickerViewModel(
            title = title,
            items = items,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: ListPickerDirection) {
        when (direction) {
            is ListPickerDirection.BackWithResult -> onResult.invoke(direction.id)
            ListPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ListPickerScreen(state.value, loaders.value, viewModel)
    }
}
