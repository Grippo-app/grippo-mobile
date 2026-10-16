package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.today
import com.grippo.design.resources.provider.tomorrow
import com.grippo.design.resources.provider.yesterday
import com.grippo.toolkit.date.utils.internal.SystemDateFormatter
import com.grippo.toolkit.localization.AppLocale
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.time.Duration

@Stable
public object DateCompose {

    @Composable
    public fun rememberFormat(value: LocalDateTime, format: DateFormat): String {
        val localeTag = AppLocale.current.split(',').firstOrNull { it.isNotBlank() }?.trim()

        return remember(value, format, localeTag) {
            SystemDateFormatter().format(value = value, pattern = format.pattern, localeTag = localeTag) ?: "-"
        }
    }

    @Composable
    public fun rememberFormat(
        value: LocalDate,
        format: DateFormat.DateOnly,
        contextual: Boolean = true
    ): String {
        val today: String = AppTokens.strings.res(Res.string.today)
        val tomorrow: String = AppTokens.strings.res(Res.string.tomorrow)
        val yesterday: String = AppTokens.strings.res(Res.string.yesterday)
        val localeTag = AppLocale.current.split(',').firstOrNull { it.isNotBlank() }?.trim()
        val referenceDate = DateTimeUtils.now().date

        return remember(value, format, contextual, localeTag, referenceDate, today, tomorrow, yesterday) {
            val contextualResult = if (contextual) {
                when (value) {
                    referenceDate -> today
                    referenceDate.minus(DatePeriod(days = 1)) -> yesterday
                    referenceDate.plus(DatePeriod(days = 1)) -> tomorrow
                    else -> null
                }
            } else {
                null
            }

            contextualResult
                ?: SystemDateFormatter().format(value = value, pattern = format.pattern, localeTag = localeTag)
                ?: "-"
        }
    }

    @Composable
    public fun rememberFormat(value: Duration): String {
        val localeTag = AppLocale.current.split(',').firstOrNull { it.isNotBlank() }?.trim()

        return remember(key1 = value, key2 = localeTag) {
            SystemDateFormatter().format(duration = value, localeTag = localeTag) ?: "-"
        }
    }
}
