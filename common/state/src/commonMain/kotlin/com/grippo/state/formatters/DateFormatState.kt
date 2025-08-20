package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DateFormatState : FormatState<LocalDateTime> {

    @Immutable
    @Serializable
    public data class Valid(
        override val displayValue: String,
        override val value: LocalDateTime
    ) : DateFormatState() {
        override val isValid: Boolean = true
    }

    @Immutable
    @Serializable
    public data class Invalid(
        override val displayValue: String,
        override val value: LocalDateTime? = null
    ) : DateFormatState() {
        override val isValid: Boolean = false
    }

    public companion object {
        public fun of(internalValue: LocalDateTime, range: DateRange): DateFormatState {
            return if (DateValidator.isValid(internalValue, range)) {
                Valid(internalValue.toString(), internalValue)
            } else {
                Invalid(internalValue.toString(), internalValue)
            }
        }
    }

    private object DateValidator {
        fun isValid(value: LocalDateTime, range: DateRange): Boolean {
            return value in range.from..range.to
        }
    }
}