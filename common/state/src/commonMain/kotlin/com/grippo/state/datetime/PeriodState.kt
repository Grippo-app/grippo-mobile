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
import com.grippo.design.resources.custom
import com.grippo.design.resources.daily
import com.grippo.design.resources.icons.Calendar
import com.grippo.design.resources.icons.Settings
import com.grippo.design.resources.monthly
import com.grippo.design.resources.weekly
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public sealed interface PeriodState {
    public val range: DateRange

    @Serializable
    @Immutable
    public data object DAILY : PeriodState {
        override val range: DateRange = DateTimeUtils.thisDay()
    }

    @Serializable
    @Immutable
    public data object WEEKLY : PeriodState {
        override val range: DateRange = DateTimeUtils.thisWeek()
    }

    @Serializable
    @Immutable
    public data object MONTHLY : PeriodState {
        override val range: DateRange = DateTimeUtils.thisMonth()
    }

    @Serializable
    @Immutable
    public data class CUSTOM(override val range: DateRange) : PeriodState

    @Composable
    public fun text(): String = when (this) {
        is CUSTOM -> AppTokens.strings.res(Res.string.custom)
        is DAILY -> AppTokens.strings.res(Res.string.daily)
        is WEEKLY -> AppTokens.strings.res(Res.string.weekly)
        is MONTHLY -> AppTokens.strings.res(Res.string.monthly)
    }


    @Composable
    public fun icon(): ImageVector = when (this) {
        is CUSTOM -> AppTokens.icons.Settings
        is DAILY -> AppTokens.icons.Calendar
        is WEEKLY -> AppTokens.icons.Calendar
        is MONTHLY -> AppTokens.icons.Calendar
    }

    @Composable
    public fun range(format: DateFormat): String {
        val from = DateCompose.rememberFormat(range.from, format)
        val to = DateCompose.rememberFormat(range.to, format)
        return "$from - $to"
    }
}