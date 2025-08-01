package com.grippo.shared.dialog

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.core.BaseComponent
import com.grippo.core.collectAsStateMultiplatform
import com.grippo.date.picker.DatePickerComponent
import com.grippo.dialog.api.DialogConfig
import com.grippo.error.display.ErrorDisplayComponent
import com.grippo.exercise.ExerciseComponent
import com.grippo.exercise.example.exerciseexample.ExerciseExampleComponent
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.period.picker.PeriodPickerComponent
import com.grippo.shared.dialog.DialogComponent.Dialog.DatePicker
import com.grippo.shared.dialog.DialogComponent.Dialog.ErrorDisplay
import com.grippo.shared.dialog.DialogComponent.Dialog.Exercise
import com.grippo.shared.dialog.DialogComponent.Dialog.ExerciseExample
import com.grippo.shared.dialog.DialogComponent.Dialog.HeightPicker
import com.grippo.shared.dialog.DialogComponent.Dialog.PeriodPicker
import com.grippo.shared.dialog.DialogComponent.Dialog.WeightPicker
import com.grippo.weight.picker.WeightPickerComponent

internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    internal sealed class Dialog(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) :
            Dialog(component)

        data class HeightPicker(override val component: HeightPickerComponent) :
            Dialog(component)

        data class ErrorDisplay(override val component: ErrorDisplayComponent) :
            Dialog(component)

        data class ExerciseExample(override val component: ExerciseExampleComponent) :
            Dialog(component)

        data class Exercise(override val component: ExerciseComponent) :
            Dialog(component)

        data class DatePicker(override val component: DatePickerComponent) :
            Dialog(component)

        data class PeriodPicker(override val component: PeriodPickerComponent) :
            Dialog(component)
    }

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel(dialogProvider = getKoin().get())
    }

    private val backCallback = BackCallback(onBack = { viewModel.dismiss(null) })

    init {
        backHandler.register(backCallback)
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
                    onResult = { viewModel.dismiss { router.onResult.invoke(it) } },
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.HeightPicker -> HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { viewModel.dismiss { router.onResult.invoke(it) } },
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.ErrorDisplay -> ErrorDisplay(
                ErrorDisplayComponent(
                    componentContext = context,
                    title = router.title,
                    description = router.description,
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.ExerciseExample -> ExerciseExample(
                ExerciseExampleComponent(
                    componentContext = context,
                    id = router.id,
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.DatePicker -> DatePicker(
                DatePickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { viewModel.dismiss { router.onResult.invoke(it) } },
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.Exercise -> Exercise(
                ExerciseComponent(
                    componentContext = context,
                    id = router.id,
                    back = { viewModel.dismiss(null) }
                )
            )

            is DialogConfig.PeriodPicker -> PeriodPicker(
                PeriodPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    available = router.available,
                    onResult = { viewModel.dismiss { router.onResult.invoke(it) } },
                    back = { viewModel.dismiss(null) }
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
