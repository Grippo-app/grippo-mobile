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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

@Stable
public object DateCompose {

    @Composable
    public fun rememberFormat(value: LocalDateTime, format: DateFormat): String {
        return remember(value, format) {
            DateTimeUtils.format(value, format)
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

        return remember(value, format, contextual) {

            if (contextual.not()) {
                return@remember DateTimeUtils.format(value, format)
            }

            val contextualResult = when {
                DateTimeUtils.isToday(value) -> today
                DateTimeUtils.isYesterday(value) -> yesterday
                DateTimeUtils.isTomorrow(value) -> tomorrow
                else -> null
            }
            contextualResult ?: DateTimeUtils.format(value, format)
        }
    }

    @Composable
    public fun rememberFormat(value: Duration): String {
        val localeTag = AppLocale.current.substringBefore(',').trim()

        return remember(key1 = value, key2 = localeTag) {
            SystemDateFormatter().format(duration = value, localeTag = localeTag) ?: "-"
        }
    }
}
