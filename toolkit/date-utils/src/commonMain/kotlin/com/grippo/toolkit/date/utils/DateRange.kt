package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily
import com.grippo.design.resources.provider.last_30_days
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
        public data class Daily(override val range: DateRange = DateTimeUtils.thisDay()) :
            Range(range)

        public data class Weekly(override val range: DateRange = DateTimeUtils.thisWeek()) :
            Range(range)

        public data class Last7Days(override val range: DateRange = DateTimeUtils.trailingWeek()) :
            Range(range)

        public data class Monthly(override val range: DateRange = DateTimeUtils.thisMonth()) :
            Range(range)

        public data class Last30Days(override val range: DateRange = DateTimeUtils.trailingMonth()) :
            Range(range)

        public data class Yearly(override val range: DateRange = DateTimeUtils.thisYear()) :
            Range(range)

        public data object Undefined : Range(null)

        public companion object {
            public fun preset(): List<DateRange> {
                return listOf()
            }
        }
    }

    public fun range(): Range {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        return when (days) {
            1 -> Range.Daily()
            7 -> Range.Last7Days()
            30 -> Range.Last30Days()
            in 28..31 -> Range.Monthly()
            in 365..366 -> Range.Yearly()
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
            is Range.Yearly -> AppTokens.strings.res(Res.string.yearly)
            Range.Undefined -> null
        }
    }

    @Composable
    public fun formatted(): String {
        when (range()) {
            is Range.Daily -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmmm)
                return from
            }

            is Range.Weekly -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            is Range.Last7Days -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            is Range.Monthly -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            is Range.Last30Days -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            is Range.Yearly -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateMmmDdYyyy)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateMmmDdYyyy)
                return "$from - $to"
            }

            Range.Undefined -> {
                val sameDay = remember(this.from, this.to) { this.from.date == this.to.date }

                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateMmmDdYyyy)
                if (sameDay) return from

                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateMmmDdYyyy)
                return "$from - $to"
            }
        }
    }
}

public operator fun DateRange.contains(ts: LocalDateTime): Boolean {
    return (ts >= from) && (ts <= to)
}
