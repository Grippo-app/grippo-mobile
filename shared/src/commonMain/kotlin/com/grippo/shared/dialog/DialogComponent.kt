package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
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

    private val dialog = SlotNavigation<DialogConfig>()

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) : Child(component)
        data class HeightPicker(override val component: HeightPickerComponent) : Child(component)
    }

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel()
    }

    override suspend fun eventListener(direction: DialogDirection) {
    }

    internal val childSlot: Value<ChildSlot<DialogConfig, Child>> =
        childSlot(
            source = dialog,
            serializer = DialogConfig.serializer(),
            key = "DialogComponent",
            handleBackButton = true,
        ) { config, context ->
            when (config) {
                is DialogConfig.WeightPicker -> Child.WeightPicker(
                    WeightPickerComponent(
                        componentContext = context,
                        onDismiss = dialog::dismiss
                    )
                )

                is DialogConfig.HeightPicker -> Child.HeightPicker(
                    HeightPickerComponent(
                        componentContext = context,
                        onDismiss = dialog::dismiss
                    )
                )
            }
        }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogScreen(childSlot, dialog, state.value, loaders.value, viewModel)
    }
}