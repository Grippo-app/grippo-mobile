package com.grippo.shared.dialog.content

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.instancekeeper.retainedInstance
import com.grippo.confirm.training.completion.ConfirmTrainingCompletionComponent
import com.grippo.confirmation.ConfirmationComponent
import com.grippo.core.foundation.BaseComponent
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.exercise_details_btn
import com.grippo.design.resources.provider.weight_picker_title
import com.grippo.design.resources.provider.duration_picker_title
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.date.picker.DatePickerComponent
import com.grippo.dialog.api.DialogConfig
import com.grippo.draft.training.DraftTrainingComponent
import com.grippo.duration.picker.DurationPickerComponent
import com.grippo.error.display.ErrorDisplayComponent
import com.grippo.exercise.ExerciseComponent
import com.grippo.exercise.example.exerciseexample.ExerciseExampleDetailsComponent
import com.grippo.goal.setup.suggestion.GoalSetupSuggestionComponent
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.iteration.picker.IterationPickerComponent
import com.grippo.language.picker.LanguagePickerComponent
import com.grippo.menu.picker.MenuPickerComponent
import com.grippo.month.picker.MonthPickerComponent
import com.grippo.muscle.loading.details.MuscleLoadingDetailsComponent
import com.grippo.performance.trend.details.PerformanceTrendDetailsComponent
import com.grippo.period.picker.PeriodPickerComponent
import com.grippo.primary.goal.picker.PrimaryGoalPickerComponent
import com.grippo.secondary.goal.picker.SecondaryGoalPickerComponent
import com.grippo.shared.dialog.DialogStep
import com.grippo.shared.dialog.internal.SessionDialogNavigation
import com.grippo.start.training.StartTrainingComponent
import com.grippo.statistics.StatisticsComponent
import com.grippo.training.goal.details.TrainingGoalDetailsComponent
import com.grippo.training.streak.details.TrainingStreakDetailsComponent
import com.grippo.weight.picker.WeightPickerComponent
import kotlinx.collections.immutable.toPersistentList

internal class DialogContentComponent(
    initial: DialogStep,
    private val navigationFor: (DialogStep) -> SessionDialogNavigation,
    componentContext: ComponentContext,
) : BaseComponent<DialogContentDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogContentViewModel()
    }

    private val backCallback = BackCallback(onBack = {
        navigationFor(childStack.value.active.configuration).back()
    })

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: DialogContentDirection) {
    }

    internal val navigation = StackNavigation<DialogStep>()

    internal val childStack: Value<ChildStack<DialogStep, Child>> = childStack(
        source = navigation,
        serializer = null, // Keep live callbacks; never restore configurations from saved state.
        initialStack = { listOf(initial) },
        key = "DialogContentComponent.v3",
        handleBackButton = false,
        childFactory = ::createChild,
    )

    private fun createChild(step: DialogStep, context: ComponentContext): Child {
        val router = step.config
        val dialogNavigation = navigationFor(step)
        return when (router) {
            is DialogConfig.WeightPicker -> Child.WeightPicker(
                WeightPickerComponent(
                    componentContext = context,
                    initial = WeightFormatState.of(router.initial),
                    onResult = { weight ->
                        val raw = weight.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.DurationPicker -> Child.DurationPicker(
                DurationPickerComponent(
                    componentContext = context,
                    initial = DurationFormatState.of(router.initial),
                    onResult = { duration ->
                        val raw = duration.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.HeightPicker -> Child.HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = HeightFormatState.of(router.initial ?: 0),
                    onResult = { height ->
                        val raw = height.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.ErrorDisplay -> Child.ErrorDisplay(
                ErrorDisplayComponent(
                    componentContext = context,
                    error = router.error,
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.ExerciseExampleDetails -> Child.ExerciseExampleDetails(
                ExerciseExampleDetailsComponent(
                    componentContext = context,
                    id = router.id,
                    mode = router.mode,
                    onAction = {
                        val action = router.mode as? DialogConfig.ExerciseExampleDetails.Mode.Action
                        if (action != null) {
                            action.onClick.invoke(dialogNavigation)
                        } else {
                            dialogNavigation.finish(null)
                        }
                    },
                    back = { dialogNavigation.finish(null) },
                )
            )

            is DialogConfig.Exercise -> Child.Exercise(
                ExerciseComponent(
                    onShowExampleDetails = { id ->
                        dialogNavigation.push(
                            DialogConfig.ExerciseExampleDetails(
                                title = UiText.Res(Res.string.exercise_details_btn),
                                id = id,
                            )
                        )
                    },
                    componentContext = context,
                    id = router.id,
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.MuscleLoadingDetails -> Child.MuscleLoadingDetails(
                MuscleLoadingDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.TrainingStreakDetails -> Child.TrainingStreakDetails(
                TrainingStreakDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.PerformanceTrendDetails -> Child.PerformanceTrendDetails(
                PerformanceTrendDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    metricType = router.metricType,
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.TrainingGoalDetails -> Child.TrainingGoalDetails(
                TrainingGoalDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { dialogNavigation.finish(null) },
                    onAddGoal = { dialogNavigation.finish { router.onAddGoal.invoke() } }
                )
            )

            is DialogConfig.Iteration -> Child.IterationPicker(
                IterationPickerComponent(
                    onPickBodyWeight = { initial, onResult ->
                        dialogNavigation.push(
                            DialogConfig.WeightPicker(
                                title = UiText.Res(Res.string.weight_picker_title),
                                initial = initial,
                                onResult = onResult,
                            )
                        )
                    },
                    componentContext = context,
                    initial = router.initial,
                    number = router.number,
                    focus = router.focus,
                    example = router.example,
                    suggestions = router.suggestions,
                    previousIteration = router.previousIteration,
                    onResult = { iteration -> dialogNavigation.finish { router.onResult.invoke(iteration) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.DatePicker -> Child.DatePicker(
                DatePickerComponent(
                    componentContext = context,
                    initial = DateTimeFormatState.of(
                        value = router.initial,
                        range = router.limitations,
                        format = router.format,
                    ),
                    limitations = router.limitations,
                    onResult = { date ->
                        val raw = date.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.PeriodPicker -> Child.PeriodPicker(
                PeriodPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { date -> dialogNavigation.finish { router.onResult.invoke(date) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.PrimaryGoalPicker -> Child.PrimaryGoalPicker(
                PrimaryGoalPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { goal -> dialogNavigation.finish { router.onResult.invoke(goal) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.SecondaryGoalPicker -> Child.SecondaryGoalPicker(
                SecondaryGoalPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { goal -> dialogNavigation.finish { router.onResult.invoke(goal) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.LanguagePicker -> Child.LanguagePicker(
                LanguagePickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    onResult = { language -> dialogNavigation.finish { router.onResult.invoke(language) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.GoalSetupSuggestion -> Child.GoalSetupSuggestion(
                GoalSetupSuggestionComponent(
                    componentContext = context,
                    onConfigure = { dialogNavigation.finish { router.onConfigure.invoke() } },
                    onLater = { dialogNavigation.finish { router.onLater.invoke() } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.MonthPicker -> Child.MonthPicker(
                MonthPickerComponent(
                    componentContext = context,
                    initial = DateTimeFormatState.of(
                        value = router.initial,
                        range = router.limitations,
                        format = router.format,
                    ),
                    limitations = router.limitations,
                    onResult = { date ->
                        val raw = date.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.DraftTraining -> Child.DraftTraining(
                DraftTrainingComponent(
                    componentContext = context,
                    onContinue = { dialogNavigation.finish { router.onContinue.invoke() } },
                    onStartNew = { dialogNavigation.finish { router.onStartNew.invoke() } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.StartTraining -> Child.StartTraining(
                StartTrainingComponent(
                    componentContext = context,
                    onStartEmpty = { dialogNavigation.finish { router.onStartEmpty.invoke() } },
                    onUseExercises = { exercises -> dialogNavigation.finish { router.onUseExercises.invoke(exercises) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.TrainingMenuPicker -> Child.MenuPicker(
                MenuPickerComponent(
                    componentContext = context,
                    items = TrainingMenu.entries,
                    onResult = { item -> dialogNavigation.finish { router.onResult.invoke(item as TrainingMenu) } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.Confirmation -> Child.Confirmation(
                ConfirmationComponent(
                    componentContext = context,
                    description = router.description,
                    onResult = { dialogNavigation.finish { router.onResult.invoke() } },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.ConfirmTrainingCompletion -> Child.ConfirmTrainingCompletion(
                ConfirmTrainingCompletionComponent(
                    onPickDuration = { initial, onResult ->
                        dialogNavigation.push(
                            DialogConfig.DurationPicker(
                                title = UiText.Res(Res.string.duration_picker_title),
                                initial = initial,
                                onResult = onResult,
                            )
                        )
                    },
                    componentContext = context,
                    initial = DurationFormatState.of(router.initial),
                    exercises = router.exercises.toPersistentList(),
                    onResult = { duration ->
                        val raw = duration.value
                        dialogNavigation.finish(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { dialogNavigation.finish(null) }
                )
            )

            is DialogConfig.Statistics -> Child.Statistics(
                StatisticsComponent(
                    config = router,
                    componentContext = context,
                    back = { dialogNavigation.finish(null) }
                )
            )
        }
    }

    @Composable
    override fun Render() {
        DialogContentScreen(this)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class WeightPicker(override val component: WeightPickerComponent) :
            Child(component)

        data class DurationPicker(override val component: DurationPickerComponent) :
            Child(component)

        data class HeightPicker(override val component: HeightPickerComponent) :
            Child(component)

        data class ErrorDisplay(override val component: ErrorDisplayComponent) :
            Child(component)

        data class ExerciseExampleDetails(override val component: ExerciseExampleDetailsComponent) :
            Child(component)

        data class Exercise(override val component: ExerciseComponent) :
            Child(component)

        data class MuscleLoadingDetails(override val component: MuscleLoadingDetailsComponent) :
            Child(component)

        data class TrainingStreakDetails(override val component: TrainingStreakDetailsComponent) :
            Child(component)

        data class PerformanceTrendDetails(override val component: PerformanceTrendDetailsComponent) :
            Child(component)

        data class TrainingGoalDetails(override val component: TrainingGoalDetailsComponent) :
            Child(component)

        data class DatePicker(override val component: DatePickerComponent) :
            Child(component)

        data class PeriodPicker(override val component: PeriodPickerComponent) :
            Child(component)

        data class PrimaryGoalPicker(override val component: PrimaryGoalPickerComponent) :
            Child(component)

        data class SecondaryGoalPicker(override val component: SecondaryGoalPickerComponent) :
            Child(component)

        data class LanguagePicker(override val component: LanguagePickerComponent) :
            Child(component)

        data class GoalSetupSuggestion(override val component: GoalSetupSuggestionComponent) :
            Child(component)

        data class MonthPicker(override val component: MonthPickerComponent) :
            Child(component)



        data class IterationPicker(override val component: IterationPickerComponent) :
            Child(component)

        data class Confirmation(override val component: ConfirmationComponent) :
            Child(component)

        data class ConfirmTrainingCompletion(override val component: ConfirmTrainingCompletionComponent) :
            Child(component)

        data class DraftTraining(override val component: DraftTrainingComponent) :
            Child(component)

        data class StartTraining(override val component: StartTrainingComponent) :
            Child(component)

        data class MenuPicker(override val component: MenuPickerComponent) :
            Child(component)

        data class Statistics(override val component: StatisticsComponent) :
            Child(component)
    }
}
