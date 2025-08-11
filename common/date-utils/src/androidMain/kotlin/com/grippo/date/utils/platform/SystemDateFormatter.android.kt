package com.grippo.date.utils.platform

import com.grippo.date.utils.internal.DateFormatter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual class SystemDateFormatter actual constructor() : DateFormatter {
    actual override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        val loc = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalDateTime())
    }
}