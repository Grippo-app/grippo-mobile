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
import com.grippo.error.display.ErrorDisplayComponent
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.shared.dialog.DialogComponent.Dialog.ErrorDisplay
import com.grippo.shared.dialog.DialogComponent.Dialog.HeightPicker
import com.grippo.shared.dialog.DialogComponent.Dialog.WeightPicker
import com.grippo.weight.picker.WeightPickerComponent

internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    internal sealed class Dialog(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) : Dialog(component)
        data class HeightPicker(override val component: HeightPickerComponent) : Dialog(component)
        data class ErrorDisplay(override val component: ErrorDisplayComponent) : Dialog(component)
    }

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel()
    }

    override suspend fun eventListener(direction: DialogDirection) {
        when (direction) {
            is DialogDirection.Activate -> dialog.activate(direction.config)
            is DialogDirection.Dismiss -> dialog.dismiss()
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
        return when (router) {
            is DialogConfig.WeightPicker -> WeightPicker(
                WeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = {
                        viewModel.dismiss {
                            router.onResult.invoke(it)
                        }
                    }
                )
            )

            is DialogConfig.HeightPicker -> HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = {
                        viewModel.dismiss {
                            router.onResult.invoke(it)
                        }
                    }
                )
            )

            is DialogConfig.ErrorDisplay -> ErrorDisplay(
                ErrorDisplayComponent(
                    componentContext = context,
                    title = router.title,
                    description = router.description,
                    onResult = {
                        viewModel.dismiss(null)
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