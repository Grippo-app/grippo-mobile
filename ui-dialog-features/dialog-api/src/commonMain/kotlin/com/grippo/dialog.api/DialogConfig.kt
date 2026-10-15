package com.grippo.dialog.api

import com.grippo.core.state.error.AppErrorState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.trainings.ExerciseState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.dialog.api.DialogController.Session
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

public sealed class DialogConfig(
    public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {
    /** Toolbar text supplied by the caller when opening this dialog. */
    public abstract val title: UiText

    /** Compares dialog content for duplicate navigation requests, excluding callbacks. */
    public fun hasSameContentAs(other: DialogConfig): Boolean {
        if (title != other.title) return false
        return when (this) {
            is ErrorDisplay -> other is ErrorDisplay && error == other.error
            is ExerciseExampleDetails -> other is ExerciseExampleDetails && id == other.id && mode.hasSameContentAs(other.mode)
            is Exercise -> other is Exercise && id == other.id
            is MuscleLoadingDetails -> other is MuscleLoadingDetails && range == other.range
            is TrainingStreakDetails -> other is TrainingStreakDetails && range == other.range
            is PerformanceTrendDetails -> other is PerformanceTrendDetails &&
                range == other.range && metricType == other.metricType
            is TrainingGoalDetails -> other is TrainingGoalDetails && range == other.range
            is Iteration -> other is Iteration && initial == other.initial && number == other.number &&
                example == other.example && focus == other.focus && suggestions == other.suggestions &&
                previousIteration == other.previousIteration
            is WeightPicker -> other is WeightPicker && initial == other.initial
            is DurationPicker -> other is DurationPicker && initial == other.initial
            is HeightPicker -> other is HeightPicker && initial == other.initial
            is DatePicker -> other is DatePicker && initial == other.initial &&
                format == other.format && limitations == other.limitations
            is PeriodPicker -> other is PeriodPicker && initial == other.initial
            is PrimaryGoalPicker -> other is PrimaryGoalPicker && initial == other.initial
            is SecondaryGoalPicker -> other is SecondaryGoalPicker && initial == other.initial
            is GoalSetupSuggestion -> other is GoalSetupSuggestion
            is MonthPicker -> other is MonthPicker && initial == other.initial &&
                format == other.format && limitations == other.limitations
            is DraftTraining -> other is DraftTraining
            is StartTraining -> other is StartTraining
            is TrainingMenuPicker -> other is TrainingMenuPicker
            is Confirmation -> other is Confirmation && description == other.description
            is ConfirmTrainingCompletion -> other is ConfirmTrainingCompletion && initial == other.initial
            is Statistics.Trainings -> other is Statistics.Trainings && range == other.range
            is Statistics.Training -> other is Statistics.Training && id == other.id
        }
    }

    private fun ExerciseExampleDetails.Mode.hasSameContentAs(other: ExerciseExampleDetails.Mode): Boolean = when (this) {
        ExerciseExampleDetails.Mode.Default -> other == ExerciseExampleDetails.Mode.Default
        is ExerciseExampleDetails.Mode.Action -> other is ExerciseExampleDetails.Mode.Action && title == other.title
    }

    public data class ErrorDisplay(
        public override val title: UiText,
        val error: AppErrorState,
        val onClose: () -> Unit = {},
    ) : DialogConfig(onDismiss = onClose)

    public data class ExerciseExampleDetails(
        public override val title: UiText,
        val id: String,
        val mode: Mode = Mode.Default,
    ) : DialogConfig() {
        public sealed interface Mode {

            public data object Default : Mode

            public data class Action(
                val title: UiText,
                val onClick: (Session) -> Unit = {},
            ) : Mode
        }
    }

    public data class Exercise(
        public override val title: UiText,
        val id: String,
    ) : DialogConfig()

    public data class MuscleLoadingDetails(
        public override val title: UiText,
        val range: DateRange,
    ) : DialogConfig()

    public data class TrainingStreakDetails(
        public override val title: UiText,
        val range: DateRange,
    ) : DialogConfig()

    public data class PerformanceTrendDetails(
        public override val title: UiText,
        val range: DateRange,
        val metricType: PerformanceMetricTypeState,
    ) : DialogConfig()

    public data class TrainingGoalDetails(
        public override val title: UiText,
        val range: DateRange,
        val onAddGoal: () -> Unit = { },
    ) : DialogConfig()

    public data class Iteration(
        public override val title: UiText,
        val initial: IterationState,
        val number: Int,
        val example: ExerciseExampleState,
        val focus: IterationFocusState,
        val suggestions: List<IterationState>,
        val previousIteration: IterationState? = null,
        val onResult: (iteration: IterationState) -> Unit = { },
    ) : DialogConfig()

    public data class WeightPicker(
        public override val title: UiText,
        val initial: Float?,
        val onResult: (value: Float) -> Unit = {},
    ) : DialogConfig()

    public data class DurationPicker(
        public override val title: UiText,
        val initial: Duration?,
        val onResult: (value: Duration) -> Unit = {},
    ) : DialogConfig()

    public data class HeightPicker(
        public override val title: UiText,
        val initial: Int?,
        val onResult: (value: Int) -> Unit = {},
    ) : DialogConfig()

    public data class DatePicker(
        val initial: LocalDateTime?,
        val format: DateFormat,
        val limitations: DateRange,
        override val title: UiText,
        val onResult: (value: LocalDateTime) -> Unit = {},
    ) : DialogConfig()

    public data class PeriodPicker(
        val initial: DateRangeKind,
        override val title: UiText,
        val onResult: (value: DateRangeKind) -> Unit = {},
    ) : DialogConfig()

    public data class PrimaryGoalPicker(
        val initial: GoalPrimaryGoalEnumState?,
        override val title: UiText,
        val onResult: (value: GoalPrimaryGoalEnumState) -> Unit = {},
    ) : DialogConfig()

    public data class SecondaryGoalPicker(
        val initial: GoalSecondaryGoalEnumState?,
        override val title: UiText,
        val onResult: (value: GoalSecondaryGoalEnumState) -> Unit = {},
    ) : DialogConfig()

    public data class GoalSetupSuggestion(
        public override val title: UiText,
        val onConfigure: () -> Unit = {},
        val onLater: () -> Unit = {},
    ) : DialogConfig()

    public data class MonthPicker(
        val initial: LocalDateTime?,
        val format: DateFormat,
        val limitations: DateRange,
        override val title: UiText,
        val onResult: (value: LocalDateTime) -> Unit = {},
    ) : DialogConfig()

    public data class DraftTraining(
        public override val title: UiText,
        val onContinue: () -> Unit = {},
        val onStartNew: () -> Unit = {},
    ) : DialogConfig()

    public data class StartTraining(
        public override val title: UiText,
        val onStartEmpty: () -> Unit = {},
        val onUseExercises: (exercises: List<ExerciseState>) -> Unit = {},
    ) : DialogConfig(dismissBySwipe = false)

    public data class TrainingMenuPicker(
        public override val title: UiText,
        val onResult: (TrainingMenu) -> Unit = {},
    ) : DialogConfig()

    public data class Confirmation(
        override val title: UiText,
        val description: UiText?,
        val onResult: () -> Unit = {},
    ) : DialogConfig()

    public data class ConfirmTrainingCompletion(
        public override val title: UiText,
        val initial: Duration?,
        val onResult: (Duration) -> Unit = {},
    ) : DialogConfig()

    public sealed class Statistics : DialogConfig() {

        public data class Trainings(
            public override val title: UiText,
            public val range: DateRange,
        ) : Statistics()

        public data class Training(
            public override val title: UiText,
            public val id: String,
        ) : Statistics()
    }
}
