package com.grippo.core.state.formatters

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily
import com.grippo.design.resources.provider.last_14_days
import com.grippo.design.resources.provider.last_30_days
import com.grippo.design.resources.provider.last_365_days
import com.grippo.design.resources.provider.last_60_days
import com.grippo.design.resources.provider.last_7_days
import com.grippo.design.resources.provider.monthly
import com.grippo.design.resources.provider.period_last_days
import com.grippo.design.resources.provider.period_this_month
import com.grippo.design.resources.provider.period_this_week
import com.grippo.design.resources.provider.period_this_year
import com.grippo.design.resources.provider.period_today
import com.grippo.design.resources.provider.weekly
import com.grippo.design.resources.provider.yearly
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangeKind
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
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

    @Composable
    public fun shortStartEnd(): String {
        val range = value ?: return display
        val format = DateRangeFormatter.dateFormat(kind)
        val from = DateCompose.rememberFormat(value = range.from, format = format)
        if (DateRangeFormatter.isSingleDate(value = range, kind = kind)) {
            return from
        }
        val to = DateCompose.rememberFormat(value = range.to, format = format)
        return DateRangeFormatter.join(from = from, to = to)
    }

    @Composable
    public fun shortEnd(): String? {
        val range = value ?: return null
        val format = DateRangeFormatter.dateFormat(kind)
        return DateCompose.rememberFormat(value = range.to, format = format)
    }

    @Composable
    public fun periodPhrase(): String? = when (kind) {
        DateRangeKind.Daily -> AppTokens.strings.res(Res.string.period_today)
        DateRangeKind.Weekly -> AppTokens.strings.res(Res.string.period_this_week)
        DateRangeKind.Monthly -> AppTokens.strings.res(Res.string.period_this_month)
        DateRangeKind.Yearly -> AppTokens.strings.res(Res.string.period_this_year)
        DateRangeKind.Last7Days -> lastDaysPhrase(days = 7)
        DateRangeKind.Last14Days -> lastDaysPhrase(days = 14)
        DateRangeKind.Last30Days -> lastDaysPhrase(days = 30)
        DateRangeKind.Last60Days -> lastDaysPhrase(days = 60)
        DateRangeKind.Last365Days -> lastDaysPhrase(days = 365)
        DateRangeKind.Custom, DateRangeKind.Infinity -> null
    }

    @Composable
    private fun lastDaysPhrase(days: Int): String {
        return AppTokens.strings.res(Res.plurals.period_last_days, days, days)
    }

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

        public fun of(range: DateRange): DateRangeFormatState {
            val kind = DateRangePresets.classify(range)
            val display = DateRangeFormatter.format(range, kind)
            return if (DateRangeValidator.isValid(range, kind)) {
                Valid(display = display, value = range, kind = kind)
            } else {
                Invalid(display = display, value = range, kind = kind)
            }
        }

        public fun of(
            kind: DateRangeKind,
            anchor: LocalDate = DateTimeUtils.today(),
        ): DateRangeFormatState {
            val range = DateRangePresets.resolve(kind = kind, anchor = anchor)
                ?: return Empty(kind = kind)
            val display = DateRangeFormatter.format(range, kind)
            return if (DateRangeValidator.isValid(range, kind)) {
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
            val kind = DateRangeKind.Custom
            val display = DateRangeFormatter.format(range, kind)
            return if (DateRangeValidator.isValid(range, kind)) {
                Valid(display = display, value = range, kind = kind)
            } else {
                Invalid(display = display, value = range, kind = kind)
            }
        }
    }

    private object DateRangeValidator {
        fun isValid(range: DateRange, kind: DateRangeKind): Boolean {
            if (!range.isWellFormed()) return false
            return isPresetBucket(kind)
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
        private const val RANGE_DASH = '\u2013'

        fun format(value: DateRange, kind: DateRangeKind): String {
            val format = dateFormat(kind)
            val from = DateTimeUtils.format(value = value.from, format = format)
            if (isSingleDate(value = value, kind = kind)) {
                return from
            }
            val to = DateTimeUtils.format(value = value.to, format = format)
            return join(from = from, to = to)
        }

        fun join(from: String, to: String): String {
            return "$from $RANGE_DASH $to"
        }

        fun dateFormat(kind: DateRangeKind): DateFormat.DateOnly = when (kind) {
            DateRangeKind.Daily -> DateFormat.DateOnly.DateDdMmmm
            DateRangeKind.Weekly,
            DateRangeKind.Last7Days,
            DateRangeKind.Last14Days,
            DateRangeKind.Monthly,
            DateRangeKind.Last30Days,
            DateRangeKind.Last60Days -> DateFormat.DateOnly.DateDdMmm

            DateRangeKind.Last365Days,
            DateRangeKind.Yearly,
            DateRangeKind.Custom,
            DateRangeKind.Infinity -> DateFormat.DateOnly.DateDdMmmYyyy
        }

        fun isSingleDate(value: DateRange, kind: DateRangeKind): Boolean =
            kind == DateRangeKind.Daily ||
                ((kind == DateRangeKind.Custom || kind == DateRangeKind.Infinity) && value.from.date == value.to.date)
    }
}
