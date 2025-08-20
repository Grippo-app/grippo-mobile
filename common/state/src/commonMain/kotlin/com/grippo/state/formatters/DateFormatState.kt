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
        override val display: String,
        override val value: LocalDateTime
    ) : DateFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: LocalDateTime? = null
    ) : DateFormatState()

    public companion object {
        public fun of(value: LocalDateTime, range: DateRange): DateFormatState {
            return if (DateValidator.isValid(value, range)) {
                Valid(value.toString(), value)
            } else {
                Invalid(value.toString(), value)
            }
        }
    }

    private object DateValidator {
        fun isValid(value: LocalDateTime, range: DateRange): Boolean {
            return value in range.from..range.to
        }
    }
}