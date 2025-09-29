package com.grippo.menu.picker

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.state.menu.MenuItemState

public class MenuPickerComponent(
    componentContext: ComponentContext,
    title: String,
    items: List<MenuItemState>,
    private val onResult: (id: String) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<MenuPickerDirection>(componentContext) {

    override val viewModel: MenuPickerViewModel = componentContext.retainedInstance {
        MenuPickerViewModel(
            title = title,
            items = items,
        )
    }

    private val backCallback = BackCallback(onBack = viewModel::onDismiss)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: MenuPickerDirection) {
        when (direction) {
            is MenuPickerDirection.BackWithResult -> onResult.invoke(direction.id)
            MenuPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        MenuPickerScreen(state.value, loaders.value, viewModel)
    }
}
