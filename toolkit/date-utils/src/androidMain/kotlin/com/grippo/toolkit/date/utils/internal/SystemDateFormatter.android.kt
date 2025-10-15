package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual class SystemDateFormatter actual constructor() : DateFormatter {
    actual override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        val loc = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalDateTime())
    }

    actual override fun format(value: LocalDate, pattern: String, localeTag: String?): String? {
        val loc = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalDate())
    }

    actual override fun format(value: LocalTime, pattern: String, localeTag: String?): String? {
        val loc = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalTime())
    }
}
