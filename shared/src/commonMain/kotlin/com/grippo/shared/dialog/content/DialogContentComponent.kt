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
import com.grippo.core.foundation.platform.collectAsStateMultiplatform
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.date.picker.DatePickerComponent
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.profile.ProfileComponent
import com.grippo.draft.training.DraftTrainingComponent
import com.grippo.duration.picker.DurationPickerComponent
import com.grippo.error.display.ErrorDisplayComponent
import com.grippo.exercise.ExerciseComponent
import com.grippo.exercise.example.exerciseexample.ExerciseExampleComponent
import com.grippo.exercise.example.picker.ExerciseExamplePickerComponent
import com.grippo.goal.setup.suggestion.GoalSetupSuggestionComponent
import com.grippo.height.picker.HeightPickerComponent
import com.grippo.iteration.picker.IterationPickerComponent
import com.grippo.menu.picker.MenuPickerComponent
import com.grippo.month.picker.MonthPickerComponent
import com.grippo.muscle.loading.details.MuscleLoadingDetailsComponent
import com.grippo.performance.trend.details.PerformanceTrendDetailsComponent
import com.grippo.period.picker.PeriodPickerComponent
import com.grippo.primary.goal.picker.PrimaryGoalPickerComponent
import com.grippo.secondary.goal.picker.SecondaryGoalPickerComponent
import com.grippo.statistics.StatisticsComponent
import com.grippo.training.goal.details.TrainingGoalDetailsComponent
import com.grippo.training.profile.details.TrainingProfileDetailsComponent
import com.grippo.training.streak.details.TrainingStreakDetailsComponent
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
                    initial = WeightFormatState.of(router.initial),
                    onResult = { weight ->
                        val raw = weight.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.DurationPicker -> Child.DurationPicker(
                DurationPickerComponent(
                    componentContext = context,
                    initial = DurationFormatState.of(router.initial),
                    onResult = { duration ->
                        val raw = duration.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.HeightPicker -> Child.HeightPicker(
                HeightPickerComponent(
                    componentContext = context,
                    initial = HeightFormatState.of(router.initial ?: 0),
                    onResult = { height ->
                        val raw = height.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
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
                    back = { viewModel.onBack(null) },
                )
            )

            is DialogConfig.Exercise -> Child.Exercise(
                ExerciseComponent(
                    componentContext = context,
                    id = router.id,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.MuscleLoadingDetails -> Child.MuscleLoadingDetails(
                MuscleLoadingDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.TrainingStreakDetails -> Child.TrainingStreakDetails(
                TrainingStreakDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.TrainingProfileDetails -> Child.TrainingProfileDetails(
                TrainingProfileDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.PerformanceTrendDetails -> Child.PerformanceTrendDetails(
                PerformanceTrendDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    metricType = router.metricType,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.TrainingGoalDetails -> Child.TrainingGoalDetails(
                TrainingGoalDetailsComponent(
                    componentContext = context,
                    range = router.range,
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.Iteration -> Child.IterationPicker(
                IterationPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    number = router.number,
                    focus = router.focus,
                    example = router.example,
                    suggestions = router.suggestions,
                    onResult = { iteration -> viewModel.onBack { router.onResult.invoke(iteration) } },
                    back = { viewModel.onBack(null) }
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
                    title = router.title,
                    limitations = router.limitations,
                    onResult = { date ->
                        val raw = date.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.PeriodPicker -> Child.PeriodPicker(
                PeriodPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    title = router.title,
                    onResult = { date -> viewModel.onBack { router.onResult.invoke(date) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.PrimaryGoalPicker -> Child.PrimaryGoalPicker(
                PrimaryGoalPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    title = router.title,
                    onResult = { goal -> viewModel.onBack { router.onResult.invoke(goal) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.SecondaryGoalPicker -> Child.SecondaryGoalPicker(
                SecondaryGoalPickerComponent(
                    componentContext = context,
                    initial = router.initial,
                    title = router.title,
                    onResult = { goal -> viewModel.onBack { router.onResult.invoke(goal) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.GoalSetupSuggestion -> Child.GoalSetupSuggestion(
                GoalSetupSuggestionComponent(
                    componentContext = context,
                    onConfigure = { viewModel.onBack { router.onConfigure.invoke() } },
                    onLater = { viewModel.onBack { router.onLater.invoke() } },
                    back = { viewModel.onBack(null) }
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
                    title = router.title,
                    limitations = router.limitations,
                    onResult = { date ->
                        val raw = date.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.DraftTraining -> Child.DraftTraining(
                DraftTrainingComponent(
                    componentContext = context,
                    onContinue = { viewModel.onBack { router.onContinue.invoke() } },
                    onStartNew = { viewModel.onBack { router.onStartNew.invoke() } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.ExerciseExamplePicker -> Child.ExerciseExamplePicker(
                ExerciseExamplePickerComponent(
                    componentContext = context,
                    targetMuscleGroupId = router.targetMuscleGroupId,
                    onResult = { example -> viewModel.onBack { router.onResult.invoke(example) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.TrainingMenuPicker -> Child.MenuPicker(
                MenuPickerComponent(
                    componentContext = context,
                    items = TrainingMenu.entries,
                    onResult = { item -> viewModel.onBack { router.onResult.invoke(item as TrainingMenu) } },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.Profile -> Child.Profile(
                ProfileComponent(
                    componentContext = context,
                    onProfileResult = { action ->
                        viewModel.onBack {
                            router.onProfileResult.invoke(
                                action
                            )
                        }
                    },
                    onSettingsResult = { action ->
                        viewModel.onBack {
                            router.onSettingsResult.invoke(
                                action
                            )
                        }
                    },
                    close = { viewModel.onBack(null) }
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

            is DialogConfig.ConfirmTrainingCompletion -> Child.ConfirmTrainingCompletion(
                ConfirmTrainingCompletionComponent(
                    componentContext = context,
                    initial = DurationFormatState.of(router.initial),
                    onResult = { duration ->
                        val raw = duration.value
                        viewModel.onBack(raw?.let { { router.onResult.invoke(it) } })
                    },
                    back = { viewModel.onBack(null) }
                )
            )

            is DialogConfig.Statistics -> Child.Statistics(
                StatisticsComponent(
                    config = router,
                    componentContext = context,
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

        data class DurationPicker(override val component: DurationPickerComponent) :
            Child(component)

        data class HeightPicker(override val component: HeightPickerComponent) :
            Child(component)

        data class Profile(override val component: ProfileComponent) :
            Child(component)

        data class ErrorDisplay(override val component: ErrorDisplayComponent) :
            Child(component)

        data class ExerciseExample(override val component: ExerciseExampleComponent) :
            Child(component)

        data class Exercise(override val component: ExerciseComponent) :
            Child(component)

        data class MuscleLoadingDetails(override val component: MuscleLoadingDetailsComponent) :
            Child(component)

        data class TrainingStreakDetails(override val component: TrainingStreakDetailsComponent) :
            Child(component)

        data class TrainingProfileDetails(override val component: TrainingProfileDetailsComponent) :
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

        data class GoalSetupSuggestion(override val component: GoalSetupSuggestionComponent) :
            Child(component)

        data class MonthPicker(override val component: MonthPickerComponent) :
            Child(component)

        data class ExerciseExamplePicker(override val component: ExerciseExamplePickerComponent) :
            Child(component)

        data class IterationPicker(override val component: IterationPickerComponent) :
            Child(component)

        data class Confirmation(override val component: ConfirmationComponent) :
            Child(component)

        data class ConfirmTrainingCompletion(override val component: ConfirmTrainingCompletionComponent) :
            Child(component)

        data class DraftTraining(override val component: DraftTrainingComponent) :
            Child(component)

        data class MenuPicker(override val component: MenuPickerComponent) :
            Child(component)

        data class Statistics(override val component: StatisticsComponent) :
            Child(component)
    }
}
