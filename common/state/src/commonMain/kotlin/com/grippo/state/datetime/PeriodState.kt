package com.grippo.state.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.grippo.date.utils.DateCompose
import com.grippo.date.utils.DateFormat
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.daily
import com.grippo.design.resources.monthly
import com.grippo.design.resources.weekly
import kotlinx.datetime.LocalDateTime

@Immutable
public sealed interface PeriodState {
    @Immutable
    public data object DAILY : PeriodState

    @Immutable
    public data object WEEKLY : PeriodState

    @Immutable
    public data object MONTHLY : PeriodState

    @Immutable
    public data class CUSTOM(
        val from: LocalDateTime,
        val to: LocalDateTime,
    ) : PeriodState

    @Composable
    public fun text(): String {
        return when (this) {
            is CUSTOM -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.MM_d)
                val to = DateCompose.rememberFormat(this.to, DateFormat.MM_d)
                "$from - $to"
            }

            DAILY -> AppTokens.strings.res(Res.string.daily)
            MONTHLY -> AppTokens.strings.res(Res.string.monthly)
            WEEKLY -> AppTokens.strings.res(Res.string.weekly)
        }
    }
}