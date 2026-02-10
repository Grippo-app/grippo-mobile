package com.grippo.dialog.api

import com.grippo.core.state.error.AppErrorState
import com.grippo.core.state.examples.ExerciseExampleState
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.menu.MenuItemState
import com.grippo.core.state.metrics.PerformanceMetricTypeState
import com.grippo.core.state.profile.ProfileMenu
import com.grippo.core.state.profile.SettingsMenu
import com.grippo.core.state.trainings.IterationFocusState
import com.grippo.core.state.trainings.IterationState
import com.grippo.toolkit.date.utils.DateRange
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Duration

@Serializable
public sealed class DialogConfig(
    @Transient public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {

    @Serializable
    public data class ErrorDisplay(
        val error: AppErrorState,
        @Transient val onClose: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = onClose,
        dismissBySwipe = true
    )

    @Serializable
    public data class ExerciseExample(
        val id: String,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class Exercise(
        val id: String,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class MuscleLoading(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class TrainingStreak(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class TrainingProfile(
        val range: DateRange,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class PerformanceTrend(
        val range: DateRange,
        val metricType: PerformanceMetricTypeState,
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

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
    )

    @Serializable
    public data class WeightPicker(
        val initial: WeightFormatState,
        @Transient val onResult: (value: WeightFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class DurationPicker(
        val initial: DurationFormatState,
        @Transient val onResult: (value: DurationFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class HeightPicker(
        val initial: HeightFormatState,
        @Transient val onResult: (value: HeightFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class DatePicker(
        val initial: DateFormatState,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: DateFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class PeriodPicker(
        val initial: DateRange.Range,
        val title: String,
        @Transient val onResult: (value: DateRange.Range) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class MonthPicker(
        val initial: DateFormatState,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: DateFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class DraftTraining(
        @Transient val onContinue: () -> Unit = {},
        @Transient val onStartNew: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class ExerciseExamplePicker(
        val targetMuscleGroupId: String?,
        @Transient val onResult: (ExerciseExampleState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class MenuPicker(
        val items: List<MenuItemState>,
        @Transient val onResult: (id: String) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class Profile(
        @Transient val onProfileResult: (ProfileMenu) -> Unit = {},
        @Transient val onSettingsResult: (SettingsMenu) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class Confirmation(
        val title: String,
        val description: String?,
        @Transient val onResult: () -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class ConfirmTrainingCompletion(
        val value: Duration,
        @Transient val onResult: (Duration) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public sealed class Statistics : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    ) {
        @Serializable
        public data class Trainings(
            public val range: DateRange,
        ) : Statistics()

        @Serializable
        public data class Training(
            public val id: String,
        ) : Statistics()
    }
}
