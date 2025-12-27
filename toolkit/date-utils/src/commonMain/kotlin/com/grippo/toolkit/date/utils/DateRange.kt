package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
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

    @Serializable
    @Immutable
    public enum class Range {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY,
        CUSTOM,
    }

    public fun range(): Range {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        return when (days) {
            1 -> Range.DAILY
            7 -> Range.WEEKLY
            in 28..31 -> Range.MONTHLY
            in 365..366 -> Range.YEARLY
            else -> Range.CUSTOM
        }
    }

    @Composable
    public fun label(): String? {
        return when (range()) {
            Range.DAILY -> AppTokens.strings.res(Res.string.daily)
            Range.WEEKLY -> AppTokens.strings.res(Res.string.weekly)
            Range.MONTHLY -> AppTokens.strings.res(Res.string.monthly)
            Range.YEARLY -> AppTokens.strings.res(Res.string.yearly)
            Range.CUSTOM -> null
        }
    }

    @Composable
    public fun formatted(): String {
        when (range()) {
            Range.DAILY -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmmm)
                return from
            }

            Range.WEEKLY -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            Range.MONTHLY -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateDdMmm)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateDdMmm)
                return "$from - $to"
            }

            Range.YEARLY -> {
                val from = DateCompose.rememberFormat(this.from, DateFormat.DateOnly.DateMmmDdYyyy)
                val to = DateCompose.rememberFormat(this.to, DateFormat.DateOnly.DateMmmDdYyyy)
                return "$from - $to"
            }

            Range.CUSTOM -> {
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
