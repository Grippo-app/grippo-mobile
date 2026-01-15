package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily
import com.grippo.design.resources.provider.last_30_days
import com.grippo.design.resources.provider.last_365_days
import com.grippo.design.resources.provider.last_7_days
import com.grippo.design.resources.provider.monthly
import com.grippo.design.resources.provider.weekly
import com.grippo.design.resources.provider.yearly
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
@Immutable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {

    @Serializable
    @Immutable
    public sealed class Range(public open val range: DateRange?) {
        @Immutable
        public data class Daily(override val range: DateRange = DateTimeUtils.thisDay()) :
            Range(range)

        @Immutable
        public data class Weekly(override val range: DateRange = DateTimeUtils.thisWeek()) :
            Range(range)

        @Immutable
        public data class Last7Days(override val range: DateRange = DateTimeUtils.trailingWeek()) :
            Range(range)

        @Immutable
        public data class Monthly(override val range: DateRange = DateTimeUtils.thisMonth()) :
            Range(range)

        @Immutable
        public data class Last30Days(override val range: DateRange = DateTimeUtils.trailingMonth()) :
            Range(range)

        @Immutable
        public data class Last365Days(override val range: DateRange = DateTimeUtils.trailingYear()) :
            Range(range)

        @Immutable
        public data class Yearly(override val range: DateRange = DateTimeUtils.thisYear()) :
            Range(range)

        @Immutable
        public data object Undefined : Range(null)
    }

    public fun range(): Range {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        return when {
            matches(DateTimeUtils.thisDay()) -> Range.Daily()
            matches(DateTimeUtils.thisWeek()) -> Range.Weekly()
            matches(DateTimeUtils.trailingWeek()) -> Range.Last7Days()
            matches(DateTimeUtils.thisMonth()) -> Range.Monthly()
            matches(DateTimeUtils.trailingMonth()) -> Range.Last30Days()
            matches(DateTimeUtils.trailingYear()) -> Range.Last365Days()
            matches(DateTimeUtils.thisYear()) -> Range.Yearly()
            days == 1 -> Range.Daily()
            days == 7 -> Range.Last7Days()
            days == 30 -> Range.Last30Days()
            days in 28..31 -> Range.Monthly()
            days in 365..366 -> Range.Yearly()
            else -> Range.Undefined
        }
    }

    @Composable
    public fun label(): String? {
        return when (range()) {
            is Range.Daily -> AppTokens.strings.res(Res.string.daily)
            is Range.Weekly -> AppTokens.strings.res(Res.string.weekly)
            is Range.Last7Days -> AppTokens.strings.res(Res.string.last_7_days)
            is Range.Monthly -> AppTokens.strings.res(Res.string.monthly)
            is Range.Last30Days -> AppTokens.strings.res(Res.string.last_30_days)
            is Range.Last365Days -> AppTokens.strings.res(Res.string.last_365_days)
            is Range.Yearly -> AppTokens.strings.res(Res.string.yearly)
            Range.Undefined -> null
        }
    }

    @Composable
    public fun formatted(): String {
        return when (range()) {
            is Range.Daily -> DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmmm)
            is Range.Weekly -> formatSpan(DateFormat.DateOnly.DateDdMmm)
            is Range.Last7Days -> formatSpan(DateFormat.DateOnly.DateDdMmm)
            is Range.Monthly -> formatSpan(DateFormat.DateOnly.DateDdMmm)
            is Range.Last30Days -> formatSpan(DateFormat.DateOnly.DateDdMmm)
            is Range.Last365Days -> formatSpan(DateFormat.DateOnly.DateMmmDdYyyy)
            is Range.Yearly -> formatSpan(DateFormat.DateOnly.DateMmmDdYyyy)
            Range.Undefined -> formatUndefined()
        }
    }

    private fun matches(other: DateRange): Boolean {
        return from == other.from && to == other.to
    }

    @Composable
    private fun formatSpan(format: DateFormat.DateOnly): String {
        val from = DateCompose.rememberFormat(this.from, format)
        val to = DateCompose.rememberFormat(this.to, format)
        return "$from - $to"
    }

    @Composable
    private fun formatUndefined(): String {
        val sameDay = remember(this.from, this.to) { this.from.date == this.to.date }
        val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateMmmDdYyyy)
        if (sameDay) return from
        val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateMmmDdYyyy)
        return "$from - $to"
    }
}

public operator fun DateRange.contains(ts: LocalDateTime): Boolean {
    return (ts >= from) && (ts <= to)
}
