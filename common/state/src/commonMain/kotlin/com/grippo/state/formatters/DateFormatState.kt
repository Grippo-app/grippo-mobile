package com.grippo.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
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
        override val value: LocalDateTime?
    ) : DateFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: LocalDateTime? = null
    ) : DateFormatState()

    public companion object {
        private val Zero = LocalDateTime(1970, 1, 1, 0, 0, 0, 0)

        public fun of(value: LocalDateTime, range: DateRange): DateFormatState {
            return when {
                value == Zero -> Empty()

                DateValidator.isValid(value, range) -> Valid(
                    display = DateTimeUtils.toUtcIso(value),
                    value = value
                )

                else -> Invalid(
                    display = DateTimeUtils.toUtcIso(value),
                    value = value
                )
            }
        }
    }

    private object DateValidator {
        fun isValid(value: LocalDateTime, range: DateRange): Boolean {
            return value in range.from..range.to
        }
    }
}