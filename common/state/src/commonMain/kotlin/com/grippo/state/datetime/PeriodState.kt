package com.grippo.state.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.custom_range
import com.grippo.design.resources.provider.icons.Calendar
import com.grippo.design.resources.provider.icons.Day
import com.grippo.design.resources.provider.icons.Settings
import com.grippo.design.resources.provider.icons.Week
import com.grippo.design.resources.provider.this_day
import com.grippo.design.resources.provider.this_month
import com.grippo.design.resources.provider.this_week
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
    public data class CUSTOM(
        override val range: DateRange,
        val limitations: DateRange
    ) : PeriodState

    @Composable
    public fun text(): String = when (this) {
        is CUSTOM -> AppTokens.strings.res(Res.string.custom_range)
        is ThisDay -> AppTokens.strings.res(Res.string.this_day)
        is ThisWeek -> AppTokens.strings.res(Res.string.this_week)
        is ThisMonth -> AppTokens.strings.res(Res.string.this_month)
    }

    @Composable
    public fun icon(): ImageVector = when (this) {
        is CUSTOM -> AppTokens.icons.Settings
        is ThisDay -> AppTokens.icons.Day
        is ThisWeek -> AppTokens.icons.Week
        is ThisMonth -> AppTokens.icons.Calendar
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

            is CUSTOM -> {
                val sameDay = remember(range.from, range.to) { range.from.date == range.to.date }

                val from = DateCompose.rememberFormat(range.from, format)
                if (sameDay) return from

                val to = DateCompose.rememberFormat(range.to, format)
                return "$from - $to"
            }
        }
    }
}