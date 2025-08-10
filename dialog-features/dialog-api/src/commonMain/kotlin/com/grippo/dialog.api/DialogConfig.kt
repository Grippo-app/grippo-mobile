package com.grippo.dialog.api

import com.grippo.date.utils.DateRange
import com.grippo.state.datetime.PeriodState
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
public sealed class DialogConfig(
    public open val onDismiss: (() -> Unit)?,
    public open val dismissBySwipe: Boolean,

    ) {
    @Serializable
    public data class ErrorDisplay(
        val title: String,
        val description: String?,
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
    public data class WeightPicker(
        val initial: Float,
        @Transient val onResult: (value: Float) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = false
    )

    @Serializable
    public data class HeightPicker(
        val initial: Int,
        @Transient val onResult: (value: Int) -> Unit = {},
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = false
    )

    @Serializable
    public data class DatePicker(
        val initial: LocalDateTime,
        val limitations: DateRange,
        val title: String,
        @Transient val onResult: (value: LocalDateTime) -> Unit = {},
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
}
