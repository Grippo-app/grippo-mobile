package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.custom
import com.grippo.design.resources.provider.daily
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
    @Composable
    public fun label(): String {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        return when (days) {
            1 -> AppTokens.strings.res(Res.string.daily)
            7 -> AppTokens.strings.res(Res.string.weekly)
            in 28..31 -> AppTokens.strings.res(Res.string.monthly)
            in 365..366 -> AppTokens.strings.res(Res.string.yearly)
            else -> AppTokens.strings.res(Res.string.custom)
        }
    }

    @Composable
    public fun range(): String {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        when (days) {
            1 -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmmm)
                return from
            }

            7 -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            in 28..31 -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            in 365..366 -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateMmmDdYyyy)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateMmmDdYyyy)
                return "$from - $to"
            }

            else -> {
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
