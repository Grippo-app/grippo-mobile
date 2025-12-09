package com.grippo.core.state.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.custom_range
import com.grippo.design.resources.provider.icons.Google
import com.grippo.design.resources.provider.icons.Settings
import com.grippo.design.resources.provider.icons.Week
import com.grippo.design.resources.provider.icons.Year
import com.grippo.design.resources.provider.this_day
import com.grippo.design.resources.provider.this_month
import com.grippo.design.resources.provider.this_week
import com.grippo.design.resources.provider.this_year
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed interface PeriodState {
    public val range: DateRange

    @Serializable
    @Immutable
    public data object ThisDay : PeriodState {
        override val range: DateRange = DateTimeUtils.thisDay()
    }

    @Serializable
    @Immutable
    public data object ThisWeek : PeriodState {
        override val range: DateRange = DateTimeUtils.thisWeek()
    }

    @Serializable
    @Immutable
    public data object ThisMonth : PeriodState {
        override val range: DateRange = DateTimeUtils.thisMonth()
    }

    @Serializable
    @Immutable
    public data object ThisYear : PeriodState {
        override val range: DateRange = DateTimeUtils.thisYear()
    }

    @Serializable
    @Immutable
    public data class Custom(
        override val range: DateRange,
        val limitations: DateRange
    ) : PeriodState

    @Composable
    public fun label(): String = when (this) {
        is Custom -> {
            val sameDay = remember(range.from, range.to) { range.from.date == range.to.date }

            val from = DateCompose.rememberFormat(range.from, DateFormat.DateOnly.DateDdMmm)
            if (sameDay) {
                from
            } else {
                val to = DateCompose.rememberFormat(range.to, DateFormat.DateOnly.DateDdMmm)
                "$from - $to"
            }
        }

        is ThisDay -> AppTokens.strings.res(Res.string.this_day)
        is ThisWeek -> AppTokens.strings.res(Res.string.this_week)
        is ThisMonth -> AppTokens.strings.res(Res.string.this_month)
        is ThisYear -> AppTokens.strings.res(Res.string.this_year)
    }

    @Composable
    public fun title(): String = when (this) {
        is Custom -> AppTokens.strings.res(Res.string.custom_range)
        is ThisDay -> AppTokens.strings.res(Res.string.this_day)
        is ThisWeek -> AppTokens.strings.res(Res.string.this_week)
        is ThisMonth -> AppTokens.strings.res(Res.string.this_month)
        is ThisYear -> AppTokens.strings.res(Res.string.this_year)
    }

    @Composable
    public fun icon(): ImageVector = when (this) {
        is Custom -> AppTokens.icons.Google
        is ThisDay -> AppTokens.icons.Google
        is ThisWeek -> AppTokens.icons.Google
        is ThisMonth -> AppTokens.icons.Google
        is ThisYear -> AppTokens.icons.Google
    }

    @Composable
    public fun range(format: DateFormat): String {
        when (this) {
            ThisDay -> {
                val from = DateCompose.rememberFormat(range.from, format)
                return from
            }

            ThisWeek -> {
                val from = DateCompose.rememberFormat(range.from, format)
                val to = DateCompose.rememberFormat(range.to, format)
                return "$from - $to"
            }

            ThisMonth -> {
                val from = DateCompose.rememberFormat(range.from, format)
                val to = DateCompose.rememberFormat(range.to, format)
                return "$from - $to"
            }

            is Custom -> {
                val sameDay = remember(range.from, range.to) { range.from.date == range.to.date }

                val from = DateCompose.rememberFormat(range.from, format)
                if (sameDay) return from

                val to = DateCompose.rememberFormat(range.to, format)
                return "$from - $to"
            }

            ThisYear -> {
                val from = DateCompose.rememberFormat(range.from, format)
                val to = DateCompose.rememberFormat(range.to, format)
                return "$from - $to"
            }
        }
    }
}