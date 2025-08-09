package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import kotlinx.datetime.LocalDateTime

@Immutable
public sealed class DateFormatState(public open val value: LocalDateTime) {
    @Immutable
    public data class Valid(
        override val value: LocalDateTime,
    ) : DateFormatState(value = value)

    @Immutable
    public data class Invalid(
        override val value: LocalDateTime,
    ) : DateFormatState(value = value)

    public companion object {
        public fun of(value: LocalDateTime, range: DateRange): DateFormatState {
            return if (DateValidator.isValid(value, range)) {
                Valid(value)
            } else {
                Invalid(value)
            }
        }
    }
}

private object DateValidator {
    fun isValid(value: LocalDateTime, range: DateRange): Boolean {
        return value in range.from..range.to
    }
}