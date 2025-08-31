package com.grippo.shared.dialog.content

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.confirmation.ConfirmationComponent
import com.grippo.core.BaseComponent
import com.grippo.core.platform.collectAsStateMultiplatform
import com.grippo.date.picker.DatePickerComponent
import com.grippo.dialog.api.DialogConfig
import com.grippo.error.display.ErrorDisplayComponent
import com.grippo.exercise.ExerciseComponent
import com.grippo.exercise.example.exerciseexample.ExerciseExampleComponent
import com.grippo.exercise.example.picker.ExerciseExamplePickerComponent
import com.grippo.filter.picker.FilterPickerComponent
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.iteration.picker.IterationPickerComponent
import com.grippo.period.picker.PeriodPickerComponent
import com.grippo.weight.picker.WeightPickerComponent

internal class DialogContentComponent(
    initial: DialogConfig,
    componentContext: ComponentContext,
    private val back: (pendingResult: (() -> Unit)?) -> Unit
) : BaseComponent<DialogContentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogContentViewModel()
    }

    private val backCallback = BackCallback(onBack = { viewModel.onBack(null) })

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DialogContentDirection) {
        when (direction) {
            is DialogContentDirection.Back -> back.invoke(direction.pendingResult)
        }
    }

    internal val navigation = StackNavigation<DialogConfig>()

    internal val childStack: Value<ChildStack<DialogConfig, Child>> = childStack(
        source = navigation,
        serializer = DialogConfig.serializer(),
        initialStack = { listOf(initial) },
        key = "DialogContentComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: DialogConfig, context: ComponentContext): Child {
        return when (router) {
            is DialogConfig.WeightPicker -> Child.WeightPicker(
                WeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { weight -> viewModel.onBack { router.onResult.invoke(weight) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.HeightPicker -> Child.HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { height -> viewModel.onBack { router.onResult.invoke(height) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.ErrorDisplay -> Child.ErrorDisplay(
                ErrorDisplayComponent(
                    componentContext = context,
                    error = router.error,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.ExerciseExample -> Child.ExerciseExample(
                ExerciseExampleComponent(
                    componentContext = context,
                    id = router.id,
                    mode = router.mode,
                    back = { viewModel.onBack(null) },
                    onResult = { exercise -> viewModel.onBack { router.onResult.invoke(exercise) } },
                )
            )

            is DialogConfig.Exercise -> Child.Exercise(
                ExerciseComponent(
                    componentContext = context,
                    id = router.id,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.Iteration -> Child.IterationPicker(
                IterationPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    number = router.number,
                    focus = router.focus,
                    suggestions = router.suggestions,
                    onResult = { iteration -> viewModel.onBack { router.onResult.invoke(iteration) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.DatePicker -> Child.DatePicker(
                DatePickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    title = router.title,
                    limitations = router.limitations,
                    onResult = { date -> viewModel.onBack { router.onResult.invoke(date) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.PeriodPicker -> Child.PeriodPicker(
                PeriodPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    available = router.available,
                    onResult = { period -> viewModel.onBack { router.onResult.invoke(period) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.ExerciseExamplePicker -> Child.ExerciseExamplePicker(
                ExerciseExamplePickerComponent(
                    componentContext = context,
                    onResult = { example -> viewModel.onBack { router.onResult.invoke(example) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.FilterPicker -> Child.FilterPicker(
                FilterPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { period -> viewModel.onBack { router.onResult.invoke(period) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.Confirmation -> Child.Confirmation(
                ConfirmationComponent(
                    componentContext = context,
                    title = router.title,
                    description = router.description,
                    onResult = { viewModel.onBack { router.onResult.invoke() } },
                    back = { viewModel.onBack(null) }
                )
            )
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        DialogContentScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) :
            Child(component)

        data class HeightPicker(override val component: HeightPickerComponent) :
            Child(component)

        data class ErrorDisplay(override val component: ErrorDisplayComponent) :
            Child(component)

        data class ExerciseExample(override val component: ExerciseExampleComponent) :
            Child(component)

        data class Exercise(override val component: ExerciseComponent) :
            Child(component)

        data class DatePicker(override val component: DatePickerComponent) :
            Child(component)

        data class PeriodPicker(override val component: PeriodPickerComponent) :
            Child(component)

        data class ExerciseExamplePicker(override val component: ExerciseExamplePickerComponent) :
            Child(component)

        data class IterationPicker(override val component: IterationPickerComponent) :
            Child(component)

        data class Confirmation(override val component: ConfirmationComponent) :
            Child(component)

        data class FilterPicker(override val component: FilterPickerComponent) :
            Child(component)
    }
}