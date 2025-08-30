package com.grippo.dialog.api

import com.grippo.date.utils.DateRange
import com.grippo.state.datetime.PeriodState
import com.grippo.state.error.AppErrorState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.filters.FilterValue
import com.grippo.state.formatters.DateFormatState
import com.grippo.state.formatters.HeightFormatState
import com.grippo.state.formatters.WeightFormatState
import com.grippo.state.trainings.IterationFocus
import com.grippo.state.trainings.IterationState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public sealed class DialogConfig(
    public open val onDismiss: (() -> Unit)?,
    public open val dismissBySwipe: Boolean,

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
    public data class Iteration(
        val initial: IterationState,
        val number: Int,
        val focus: IterationFocus,
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
        dismissBySwipe = false
    )

    @Serializable
    public data class HeightPicker(
        val initial: HeightFormatState,
        @Transient val onResult: (value: HeightFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = false
    )

    @Serializable
    public data class DatePicker(
        val initial: DateFormatState,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: DateFormatState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = false
    )

    @Serializable
    public data class PeriodPicker(
        val initial: PeriodState,
        val available: List<PeriodState>,
        @Transient val onResult: (value: PeriodState) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )

    @Serializable
    public data class FilterPicker(
        val initial: List<FilterValue>,
        @Transient val onResult: (List<FilterValue>) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true
    )


    @Serializable
    public data class ExerciseExamplePicker(
        @Transient val onResult: (ExerciseExampleState) -> Unit = {},
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
}