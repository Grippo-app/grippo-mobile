package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DateFormatState : FormatState<LocalDate> {

    public abstract val format: DateFormat.DateOnly

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: LocalDate,
        override val format: DateFormat.DateOnly,
    ) : DateFormatState(), FormatState.Valid<LocalDate>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: LocalDate?,
        override val format: DateFormat.DateOnly,
    ) : DateFormatState(), FormatState.Invalid<LocalDate>

    @Immutable
    @Serializable
    public data class Empty(
        override val format: DateFormat.DateOnly,
        override val display: String = "",
        override val value: LocalDate? = null,
    ) : DateFormatState(), FormatState.Empty<LocalDate>

    public companion object {
        public fun of(
            value: LocalDate?,
            range: DateRange,
            format: DateFormat.DateOnly,
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
        fun isValid(value: LocalDate, range: DateRange): Boolean {
            return value in range.from.date..range.to.date
        }
    }
}
