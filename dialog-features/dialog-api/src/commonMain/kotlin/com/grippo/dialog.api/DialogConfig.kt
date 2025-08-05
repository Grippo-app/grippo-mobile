package com.grippo.dialog.api

import com.grippo.date.utils.DateRange
import com.grippo.state.datetime.PeriodState
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
public sealed class DialogConfig(public open val onDismiss: (() -> Unit)?) {
    @Serializable
    public data class ErrorDisplay(
        val title: String,
        val description: String?,
        val onClose: () -> Unit,
    ) : DialogConfig(onClose)

    @Serializable
    public data class ExerciseExample(
        val id: String,
    ) : DialogConfig(null)

    @Serializable
    public data class Exercise(
        val id: String,
    ) : DialogConfig(null)

    @Serializable
    public data class WeightPicker(
        val initial: Float,
        val onResult: (value: Float) -> Unit,
    ) : DialogConfig(null)

    @Serializable
    public data class HeightPicker(
        val initial: Int,
        val onResult: (value: Int) -> Unit,
    ) : DialogConfig(null)

    @Serializable
    public data class DatePicker(
        val initial: LocalDateTime,
        val limitations: DateRange,
        val onResult: (value: LocalDateTime) -> Unit,
    ) : DialogConfig(null)

    @Serializable
    public data class PeriodPicker(
        val initial: PeriodState,
        val available: List<PeriodState>,
        val onResult: (value: PeriodState) -> Unit,
    ) : DialogConfig(null)
}
