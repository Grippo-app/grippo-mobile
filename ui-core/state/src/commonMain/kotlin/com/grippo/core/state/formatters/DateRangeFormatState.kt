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
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed class DateRangeFormatState : FormatState<DateRange> {

    public abstract val kind: DateRangeKind

    @Immutable
    @Serializable
    public data class Valid(
        override val display: String,
        override val value: DateRange,
        override val kind: DateRangeKind,
    ) : DateRangeFormatState(), FormatState.Valid<DateRange>

    @Immutable
    @Serializable
    public data class Invalid(
        override val display: String,
        override val value: DateRange?,
        override val kind: DateRangeKind,
    ) : DateRangeFormatState(), FormatState.Invalid<DateRange>

    @Immutable
    @Serializable
    public data class Empty(
        override val display: String = "",
        override val value: DateRange? = null,
        override val kind: DateRangeKind = DateRangeKind.Custom,
    ) : DateRangeFormatState(), FormatState.Empty<DateRange>

    public fun label(): UiText? = when (kind) {
        DateRangeKind.Daily -> UiText.Res(Res.string.daily)
        DateRangeKind.Weekly -> UiText.Res(Res.string.weekly)
        DateRangeKind.Last7Days -> UiText.Res(Res.string.last_7_days)
        DateRangeKind.Last14Days -> UiText.Res(Res.string.last_14_days)
        DateRangeKind.Monthly -> UiText.Res(Res.string.monthly)
        DateRangeKind.Last30Days -> UiText.Res(Res.string.last_30_days)
        DateRangeKind.Last60Days -> UiText.Res(Res.string.last_60_days)
        DateRangeKind.Last365Days -> UiText.Res(Res.string.last_365_days)
        DateRangeKind.Yearly -> UiText.Res(Res.string.yearly)
        DateRangeKind.Custom, DateRangeKind.Infinity -> null
    }

    public companion object {

        /**
         * Wraps an existing [DateRange], classifying it into a [DateRangeKind].
         * Produces [Valid] for preset-kind matches, [Invalid] for Custom/Infinity
         * (no user validation was attempted — `error` stays null).
         */
        public fun of(range: DateRange): DateRangeFormatState {
            val kind = DateRangePresets.classify(range)
            val display = DateRangeFormatter.format(range, kind)
            return if (isPresetBucket(kind)) {
                Valid(display = display, value = range, kind = kind)
            } else {
                Invalid(display = display, value = range, kind = kind)
            }
        }

        /**
         * Resolves a preset [DateRangeKind] into a [DateRangeFormatState].
         * Passing [DateRangeKind.Custom] yields [Empty] — Custom has no
         * canonical resolution without explicit endpoints.
         */
        public fun of(kind: DateRangeKind): DateRangeFormatState {
            val range = DateRangePresets.resolve(kind)
                ?: return Empty(kind = kind)
            val display = DateRangeFormatter.format(range, kind)
            return if (isPresetBucket(kind)) {
                Valid(display = display, value = range, kind = kind)
            } else {
                Invalid(display = display, value = range, kind = kind)
            }
        }

        public fun of(
            from: LocalDateTime?,
            to: LocalDateTime?,
        ): DateRangeFormatState {
            if (from == null || to == null) {
                return Empty(kind = DateRangeKind.Custom)
            }
            val range = DateRange(from = from, to = to)
            val display = DateRangeFormatter.format(range, DateRangeKind.Custom)
            val isError = range.from > range.to
            return if (isError) {
                Valid(display = display, value = range, kind = DateRangeKind.Custom)
            } else {
                Invalid(
                    display = display,
                    value = range,
                    kind = DateRangeKind.Custom,
                )
            }
        }

        private fun isPresetBucket(kind: DateRangeKind): Boolean = when (kind) {
            DateRangeKind.Daily,
            DateRangeKind.Weekly,
            DateRangeKind.Last7Days,
            DateRangeKind.Last14Days,
            DateRangeKind.Monthly,
            DateRangeKind.Last30Days,
            DateRangeKind.Last60Days,
            DateRangeKind.Last365Days,
            DateRangeKind.Yearly -> true

            DateRangeKind.Custom,
            DateRangeKind.Infinity -> false
        }
    }

    private object DateRangeFormatter {
        fun format(value: DateRange, kind: DateRangeKind): String = when (kind) {
            DateRangeKind.Daily -> DateTimeUtils.format(value.from, DateFormat.DateOnly.DateDdMmmm)
            DateRangeKind.Weekly,
            DateRangeKind.Last7Days,
            DateRangeKind.Last14Days,
            DateRangeKind.Monthly,
            DateRangeKind.Last30Days,
            DateRangeKind.Last60Days -> span(value, DateFormat.DateOnly.DateDdMmm)

            DateRangeKind.Last365Days,
            DateRangeKind.Yearly -> span(value, DateFormat.DateOnly.DateMmmDdYyyy)

            DateRangeKind.Custom,
            DateRangeKind.Infinity -> undefined(value)
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
