package com.grippo.date.utils

import com.grippo.date.utils.platform.SystemDateFormatter
import kotlinx.datetime.LocalDateTime
import kotlin.concurrent.Volatile

internal interface DateFormatter {
    fun format(value: LocalDateTime, pattern: String, localeTag: String? = null): String?
}

internal object DateFormatting {
    @Volatile
    var current: DateFormatter = SystemDateFormatter()
        private set

    fun install(formatter: DateFormatter) {
        current = formatter
    }
}