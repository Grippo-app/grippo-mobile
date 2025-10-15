package com.grippo.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.time_ago_day
import com.grippo.design.resources.provider.time_ago_days
import com.grippo.design.resources.provider.time_ago_less_than_day
import com.grippo.design.resources.provider.time_ago_month
import com.grippo.design.resources.provider.time_ago_months
import com.grippo.design.resources.provider.time_ago_year
import com.grippo.design.resources.provider.time_ago_years
import com.grippo.design.resources.provider.today
import com.grippo.design.resources.provider.tomorrow
import com.grippo.design.resources.provider.yesterday
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@Stable
public object DateCompose {

    @Composable
    public fun rememberAgo(value: LocalDateTime): String {
        val daysRes = AppTokens.strings.res(Res.string.time_ago_days)
        val dayRes = AppTokens.strings.res(Res.string.time_ago_day)
        val monthsRes = AppTokens.strings.res(Res.string.time_ago_months)
        val monthRes = AppTokens.strings.res(Res.string.time_ago_month)
        val yearsRes = AppTokens.strings.res(Res.string.time_ago_years)
        val yearRes = AppTokens.strings.res(Res.string.time_ago_year)
        val lessThanDayRes = AppTokens.strings.res(Res.string.time_ago_less_than_day)

        return remember(value) {
            val duration = DateTimeUtils.ago(value)

            val days = duration.inWholeDays
            val months = days / 30
            val years = days / 365

            when {
                years > 0 -> {
                    val res = if (years == 1L) yearRes else yearsRes
                    res.replace("%d", years.toString())
                }

                months > 0 -> {
                    val res = if (months == 1L) monthRes else monthsRes
                    res.replace("%d", months.toString())
                }

                days > 0 -> {
                    val res = if (days == 1L) dayRes else daysRes
                    res.replace("%d", days.toString())
                }

                else -> lessThanDayRes
            }
        }
    }

    @Composable
    public fun rememberFormat(value: LocalDateTime, format: DateFormat): String {
        return remember(value, format) {
            DateTimeUtils.format(value, format)
        }
    }

    @Composable
    public fun rememberFormat(value: LocalDate, format: DateFormat): String {
        val today: String = AppTokens.strings.res(Res.string.today)
        val tomorrow: String = AppTokens.strings.res(Res.string.tomorrow)
        val yesterday: String = AppTokens.strings.res(Res.string.yesterday)

        return remember(value, format) {
            val contextual = when {
                DateTimeUtils.isToday(value) -> today
                DateTimeUtils.isYesterday(value) -> yesterday
                DateTimeUtils.isTomorrow(value) -> tomorrow
                else -> null
            }

            contextual ?: DateTimeUtils.format(value, format)
        }
    }
}