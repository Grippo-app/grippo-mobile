package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.dialog.api.DialogConfig
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.weight.picker.WeightPickerComponent

internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    internal sealed class Dialog(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) : Dialog(component)
        data class HeightPicker(override val component: HeightPickerComponent) : Dialog(component)
    }

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel()
    }

    override suspend fun eventListener(direction: DialogDirection) {
        when (direction) {
            is DialogDirection.Activate -> dialog.activate(direction.config)
            DialogDirection.Dismiss -> dialog.dismiss()
        }
    }

    private val dialog = SlotNavigation<DialogConfig>()

    internal val childSlot: Value<ChildSlot<DialogConfig, Dialog>> = childSlot(
        source = dialog,
        serializer = DialogConfig.serializer(),
        key = "DialogComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: DialogConfig, context: ComponentContext): Dialog {
        println("new child")
        return when (router) {
            is DialogConfig.WeightPicker -> Dialog.WeightPicker(
                WeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = {
                        viewModel.dismiss()
                        router.onResult.invoke(it)
                    }
                )
            )

            is DialogConfig.HeightPicker -> Dialog.HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = {
                        viewModel.dismiss()
                        router.onResult.invoke(it)
                    }
                )
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogScreen(childSlot, state.value, loaders.value, viewModel)
    }
}