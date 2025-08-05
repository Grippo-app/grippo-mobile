package com.grippo.state.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.custom_range
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Settings
import com.grippo.design.resources.this_day
import com.grippo.design.resources.this_month
import com.grippo.design.resources.this_week
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
        is ThisDay -> AppTokens.icons.Calendar
        is ThisWeek -> AppTokens.icons.Calendar
        is ThisMonth -> AppTokens.icons.Calendar
    }

    @Composable
    public fun range(format: DateFormat): String {
        val from = DateCompose.rememberFormat(range.from, format)
        val to = DateCompose.rememberFormat(range.to, format)
        return "$from - $to"
    }
}