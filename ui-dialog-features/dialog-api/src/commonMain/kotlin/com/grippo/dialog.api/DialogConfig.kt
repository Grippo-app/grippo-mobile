package com.grippo.dialog.api

import com.grippo.core.state.error.AppErrorState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.menu.ProfileMenu
import com.grippo.core.state.menu.SettingsMenu
import com.grippo.core.state.menu.TrainingMenu
import com.grippo.core.state.metrics.performance.PerformanceMetricTypeState
import com.grippo.core.state.profile.GoalPrimaryGoalEnumState
import com.grippo.core.state.profile.GoalSecondaryGoalEnumState
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
public sealed class DialogConfig(
    @Transient public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {
    public abstract val key: String

    protected fun buildKey(vararg parts: Any?): String {
        return buildString {
            parts.forEachIndexed { index, part ->
                if (index > 0) append('|')
                val value = part?.toString() ?: "<null>"
                append(value.length)
                append(':')
                append(value)
            }
        }
    }

    @Serializable
    public data class ErrorDisplay(
        val error: AppErrorState,
        @Transient val onClose: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = onClose,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("ErrorDisplay", error)
    }

    @Serializable
    public data class ExerciseExample(
        val id: String,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("ExerciseExample", id)
    }

    @Serializable
    public data class Exercise(
        val id: String,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("Exercise", id)
    }

    @Serializable
    public data class MuscleLoadingDetails(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("MuscleLoadingDetails", range)
    }

    @Serializable
    public data class TrainingStreakDetails(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("TrainingStreakDetails", range)
    }

    @Serializable
    public data class TrainingProfileDetails(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("TrainingProfileDetails", range)
    }

    @Serializable
    public data class PerformanceTrendDetails(
        val range: DateRange,
        val metricType: PerformanceMetricTypeState,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("PerformanceTrendDetails", range, metricType)
    }

    @Serializable
    public data class TrainingGoalDetails(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("TrainingGoalDetails", range)
    }

    @Serializable
    public data class Iteration(
        val initial: IterationState,
        val number: Int,
        val example: ExerciseExampleState,
        val focus: IterationFocusState,
        val suggestions: List<IterationState>,
        @Transient val onResult: (iteration: IterationState) -> Unit = { },
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("Iteration", initial, number, example, focus, suggestions)
    }

    @Serializable
    public data class WeightPicker(
        val initial: Float?,
        @Transient val onResult: (value: Float) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("WeightPicker", initial)
    }

    @Serializable
    public data class DurationPicker(
        val initial: Duration?,
        @Transient val onResult: (value: Duration) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("DurationPicker", initial)
    }

    @Serializable
    public data class HeightPicker(
        val initial: Int?,
        @Transient val onResult: (value: Int) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("HeightPicker", initial)
    }

    @Serializable
    public data class DatePicker(
        val initial: LocalDateTime?,
        val format: DateFormat,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: LocalDateTime) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("DatePicker", initial, format, limitations, title)
    }

    @Serializable
    public data class PeriodPicker(
        val initial: DateRangeKind,
        val title: String,
        @Transient val onResult: (value: DateRangeKind) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("PeriodPicker", initial, title)
    }

    @Serializable
    public data class PrimaryGoalPicker(
        val initial: GoalPrimaryGoalEnumState?,
        val title: String,
        @Transient val onResult: (value: GoalPrimaryGoalEnumState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("PrimaryGoalPicker", initial, title)
    }

    @Serializable
    public data class SecondaryGoalPicker(
        val initial: GoalSecondaryGoalEnumState?,
        val title: String,
        @Transient val onResult: (value: GoalSecondaryGoalEnumState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("SecondaryGoalPicker", initial, title)
    }

    @Serializable
    public data class GoalSetupSuggestion(
        @Transient val onConfigure: () -> Unit = {},
        @Transient val onLater: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("GoalSetupSuggestion")
    }

    @Serializable
    public data class MonthPicker(
        val initial: LocalDateTime?,
        val format: DateFormat,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: LocalDateTime) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("MonthPicker", initial, format, limitations, title)
    }

    @Serializable
    public data class DraftTraining(
        @Transient val onContinue: () -> Unit = {},
        @Transient val onStartNew: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("DraftTraining")
    }

    @Serializable
    public data class ExerciseExamplePicker(
        val targetMuscleGroupId: String?,
        @Transient val onResult: (ExerciseExampleState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("ExerciseExamplePicker", targetMuscleGroupId)
    }

    @Serializable
    public data class TrainingMenuPicker(
        @Transient val onResult: (TrainingMenu) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("TrainingMenuPicker")
    }

    @Serializable
    public data class Profile(
        @Transient val onProfileResult: (ProfileMenu) -> Unit = {},
        @Transient val onSettingsResult: (SettingsMenu) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("Profile")
    }

    @Serializable
    public data class Confirmation(
        val title: String,
        val description: String?,
        @Transient val onResult: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("Confirmation", title, description)
    }

    @Serializable
    public data class ConfirmTrainingCompletion(
        val initial: Duration?,
        @Transient val onResult: (Duration) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        override val key: String
            get() = buildKey("ConfirmTrainingCompletion", initial)
    }

    @Serializable
    public sealed class Statistics : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        @Serializable
        public data class Trainings(
            public val range: DateRange,
        ) : Statistics() {
            override val key: String
                get() = buildKey("Statistics.Trainings", range)
        }

        @Serializable
        public data class Training(
            public val id: String,
        ) : Statistics() {
            override val key: String
                get() = buildKey("Statistics.Training", id)
        }
    }
}
