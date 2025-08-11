package com.grippo.date.utils.platform

import com.grippo.date.utils.internal.DateFormatter
import kotlinx.datetime.LocalDateTime

internal expect class SystemDateFormatter() : DateFormatter {
    override fun format(
        value: LocalDateTime,
        pattern: String,
        localeTag: String?
    ): String?
}