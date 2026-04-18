package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DateFormatState : FormatState<LocalDateTime> {

    public abstract val format: DateFormat

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: LocalDateTime,
        override val format: DateFormat,
    ) : DateFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: LocalDateTime?,
        override val format: DateFormat,
    ) : DateFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val format: DateFormat,
        override val display: String = "",
        override val value: LocalDateTime? = null,
    ) : DateFormatState()

    public companion object {
        public fun of(
            value: LocalDateTime?,
            range: DateRange,
            format: DateFormat,
        ): DateFormatState {
            if (value == null) return Empty(format = format)
            val display = DateTimeUtils.format(value, format)
            return when {
                DateValidator.isValid(value, range) -> Valid(
                    display = display,
                    value = value,
                    format = format
                )

                else -> Invalid(
                    display = display,
                    value = value,
                    format = format
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
