package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

internal expect class SystemDateFormatter() {
    fun format(
        value: LocalDateTime,
        pattern: String,
        localeTag: String
    ): String?

    fun format(
        value: LocalDate,
        pattern: String,
        localeTag: String
    ): String?

    fun format(
        duration: Duration,
        localeTag: String
    ): String?
}
