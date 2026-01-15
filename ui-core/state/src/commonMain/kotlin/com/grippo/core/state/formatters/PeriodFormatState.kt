package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class PeriodFormatState : FormatState<DateRange> {

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: DateRange
    ) : PeriodFormatState()

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: DateRange?
    ) : PeriodFormatState()

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: DateRange? = null
    ) : PeriodFormatState()

    public companion object {
        private val Zero = LocalDateTime(1970, 1, 1, 0, 0, 0, 0)

        public fun of(value: DateRange): PeriodFormatState {
            return when {
                value.from == Zero && value.to == Zero -> Empty()

                DateValidator.isValid(value) -> Valid(
                    display = display(value),
                    value = value
                )

                else -> Invalid(
                    display = display(value),
                    value = value
                )
            }
        }

        private fun display(value: DateRange): String {
            return "${
                DateTimeUtils.format(value.from, DateFormat.DateOnly.DateDdMmmm)
            } - ${
                DateTimeUtils.format(value.to, DateFormat.DateOnly.DateDdMmmm)
            }"
        }
    }

    private object DateValidator {
        fun isValid(value: DateRange): Boolean {
            return true
        }
    }
}
