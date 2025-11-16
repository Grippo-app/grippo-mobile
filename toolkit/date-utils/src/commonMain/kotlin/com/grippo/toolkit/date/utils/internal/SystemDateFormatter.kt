package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration

internal expect class SystemDateFormatter() : DateFormatter {
    override fun format(
        value: LocalDateTime,
        pattern: String,
        localeTag: String?
    ): String?

    override fun format(
        value: LocalDate,
        pattern: String,
        localeTag: String?
    ): String?

    override fun format(
        value: LocalTime,
        pattern: String,
        localeTag: String?
    ): String?

    override fun format(
        duration: Duration,
        localeTag: String?
    ): String?
}
