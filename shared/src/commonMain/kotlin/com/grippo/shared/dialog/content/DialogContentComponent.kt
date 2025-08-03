package com.grippo.shared.dialog.content

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
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
import com.grippo.shared.dialog.empty.EmptyComponent
import com.grippo.weight.picker.WeightPickerComponent

internal class DialogContentComponent(
    initial: DialogConfig,
    componentContext: ComponentContext,
    private val back: () -> Unit
) : BaseComponent<DialogContentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogContentViewModel()
    }

    private val backCallback = BackCallback(onBack = viewModel::onBack)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DialogContentDirection) {
        when (direction) {
            DialogContentDirection.Back -> back.invoke()
        }
    }

    private val navigation = StackNavigation<DialogConfig>()

    internal val childStack: Value<ChildStack<DialogConfig, Child>> = childStack(
        source = navigation,
        serializer = DialogConfig.serializer(),
        initialStack = { listOf(initial) },
        key = "DialogConfig",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: DialogConfig, context: ComponentContext): Child {
        return when (router) {
            is DialogConfig.WeightPicker -> Child.WeightPicker(
                WeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { /*viewModel.dismiss { router.onResult.invoke(it) }*/ },
                    back = { /*viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.HeightPicker -> Child.HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { /*viewModel.dismiss { router.onResult.invoke(it) }*/ },
                    back = { /*viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.ErrorDisplay -> Child.ErrorDisplay(
                ErrorDisplayComponent(
                    componentContext = context,
                    title = router.title,
                    description = router.description,
                    back = { /*viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.ExerciseExample -> Child.ExerciseExample(
                ExerciseExampleComponent(
                    componentContext = context,
                    id = router.id,
                    back = { /*viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.DatePicker -> Child.DatePicker(
                DatePickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { /*viewModel.dismiss { router.onResult.invoke(it) }*/ },
                    back = {/* viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.Exercise -> Child.Exercise(
                ExerciseComponent(
                    componentContext = context,
                    id = router.id,
                    back = { /*viewModel.dismiss(null)*/ }
                )
            )

            is DialogConfig.PeriodPicker -> Child.PeriodPicker(
                PeriodPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    available = router.available,
                    onResult = { /*viewModel.dismiss { router.onResult.invoke(it) }*/ },
                    back = { /*viewModel.dismiss(null)*/ }
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

        data class Empty(override val component: EmptyComponent) :
            Child(component)
    }
}