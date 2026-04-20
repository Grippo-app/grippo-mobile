package com.grippo.core.state.formatters

import androidx.compose.runtime.Immutable
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily
import com.grippo.design.resources.provider.last_14_days
import com.grippo.design.resources.provider.last_30_days
import com.grippo.design.resources.provider.last_365_days
import com.grippo.design.resources.provider.last_60_days
import com.grippo.design.resources.provider.last_7_days
import com.grippo.design.resources.provider.monthly
import com.grippo.design.resources.provider.weekly
import com.grippo.design.resources.provider.yearly
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DateRangeFormatState : FormatState<DateRange> {

    public abstract val kind: DateRange.Range

    override val value: DateRange?
        get() = kind.range

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val kind: DateRange.Range,
    ) : DateRangeFormatState(), FormatState.Valid<DateRange>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val kind: DateRange.Range,
    ) : DateRangeFormatState(), FormatState.Invalid<DateRange>

    @Immutable
    @Serializable
    public data class Empty(
        override val kind: DateRange.Range = DateRange.Range.Undefined(),
        override val display: String = "",
    ) : DateRangeFormatState(), FormatState.Empty<DateRange>

    public fun label(): UiText? = when (kind) {
        is DateRange.Range.Daily -> UiText.Res(Res.string.daily)
        is DateRange.Range.Weekly -> UiText.Res(Res.string.weekly)
        is DateRange.Range.Last7Days -> UiText.Res(Res.string.last_7_days)
        is DateRange.Range.Last14Days -> UiText.Res(Res.string.last_14_days)
        is DateRange.Range.Monthly -> UiText.Res(Res.string.monthly)
        is DateRange.Range.Last30Days -> UiText.Res(Res.string.last_30_days)
        is DateRange.Range.Last60Days -> UiText.Res(Res.string.last_60_days)
        is DateRange.Range.Last365Days -> UiText.Res(Res.string.last_365_days)
        is DateRange.Range.Yearly -> UiText.Res(Res.string.yearly)
        is DateRange.Range.Undefined -> null
        is DateRange.Range.Infinity -> null
    }

    public companion object {
        public fun of(value: DateRange): DateRangeFormatState {
            val kind = value.range()
            val display = DateRangeFormatter.format(value, kind)
            return when {
                DateRangeValidator.isValid(kind) -> Valid(
                    display = display,
                    kind = kind,
                )

                else -> Invalid(
                    display = display,
                    kind = kind,
                )
            }
        }

        public fun of(range: DateRange.Range): DateRangeFormatState {
            val resolved = range.range
                ?: return Empty(kind = range)

            val display = DateRangeFormatter.format(resolved, range)
            return when {
                DateRangeValidator.isValid(range) -> Valid(
                    display = display,
                    kind = range,
                )

                else -> Invalid(
                    display = display,
                    kind = range,
                )
            }
        }
    }

    private object DateRangeValidator {
        fun isValid(kind: DateRange.Range): Boolean = when (kind) {
            is DateRange.Range.Daily,
            is DateRange.Range.Weekly,
            is DateRange.Range.Last7Days,
            is DateRange.Range.Last14Days,
            is DateRange.Range.Monthly,
            is DateRange.Range.Last30Days,
            is DateRange.Range.Last60Days,
            is DateRange.Range.Last365Days,
            is DateRange.Range.Yearly,
                -> true

            is DateRange.Range.Undefined,
            is DateRange.Range.Infinity,
                -> false
        }
    }

    private object DateRangeFormatter {
        fun format(value: DateRange, kind: DateRange.Range): String = when (kind) {
            is DateRange.Range.Daily -> DateTimeUtils.format(
                value.from,
                DateFormat.DateOnly.DateDdMmmm,
            )

            is DateRange.Range.Weekly,
            is DateRange.Range.Last7Days,
            is DateRange.Range.Last14Days,
            is DateRange.Range.Monthly,
            is DateRange.Range.Last30Days,
            is DateRange.Range.Last60Days,
                -> span(value, DateFormat.DateOnly.DateDdMmm)

            is DateRange.Range.Last365Days,
            is DateRange.Range.Yearly,
                -> span(value, DateFormat.DateOnly.DateMmmDdYyyy)

            is DateRange.Range.Undefined,
            is DateRange.Range.Infinity,
                -> undefined(value)
        }

        private fun span(value: DateRange, format: DateFormat.DateOnly): String {
            val from = DateTimeUtils.format(value.from, format)
            val to = DateTimeUtils.format(value.to, format)
            return "$from - $to"
        }

        private fun undefined(value: DateRange): String {
            val sameDay = value.from.date == value.to.date
            val from = DateTimeUtils.format(value.from, DateFormat.DateOnly.DateMmmDdYyyy)
            if (sameDay) return from
            val to = DateTimeUtils.format(value.to, DateFormat.DateOnly.DateMmmDdYyyy)
            return "$from - $to"
        }
    }
}
