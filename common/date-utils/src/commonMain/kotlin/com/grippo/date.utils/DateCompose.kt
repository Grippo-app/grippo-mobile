package com.grippo.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.Res
import com.grippo.design.resources.time_ago_day
import com.grippo.design.resources.time_ago_days
import com.grippo.design.resources.time_ago_month
import com.grippo.design.resources.time_ago_months
import com.grippo.design.resources.time_ago_year
import com.grippo.design.resources.time_ago_years
import kotlinx.datetime.LocalDateTime

@Stable
public object DateCompose {

    private val dateUtils = DateTimeUtils

    @Composable
    public fun rememberAgo(value: LocalDateTime): String {
        val daysRes = AppTokens.strings.res(Res.string.time_ago_days)
        val dayRes = AppTokens.strings.res(Res.string.time_ago_day)
        val monthsRes = AppTokens.strings.res(Res.string.time_ago_months)
        val monthRes = AppTokens.strings.res(Res.string.time_ago_month)
        val yearsRes = AppTokens.strings.res(Res.string.time_ago_years)
        val yearRes = AppTokens.strings.res(Res.string.time_ago_year)

        return remember(value) {
            val duration = dateUtils.ago(value)

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

                else -> dayRes.replace("%d", 0.toString())
            }
        }
    }

    @Composable
    public fun rememberFormat(value: LocalDateTime, format: DateFormat): String =
        remember(value, format) {
            dateUtils.format(value, format)
        }
}