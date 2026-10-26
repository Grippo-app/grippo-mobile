package com.grippo.toolkit.date.utils.internal

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.toJavaDuration

internal actual class SystemDateFormatter actual constructor() {
    actual fun format(value: LocalDateTime, pattern: String, localeTag: String): String? {
        val loc = Locale.forLanguageTag(localeTag)
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalDateTime())
    }

    actual fun format(value: LocalDate, pattern: String, localeTag: String): String? {
        val loc = Locale.forLanguageTag(localeTag)
        val fmt = DateTimeFormatter.ofPattern(pattern, loc)
        return fmt.format(value.toJavaLocalDate())
    }

    actual fun format(duration: Duration, localeTag: String): String? {
        val loc = Locale.forLanguageTag(localeTag)
        val absolute = duration.toJavaDuration().abs()
        val isNegative = duration.isNegative()

        val hours = absolute.toHours()
        val minutes = absolute.minusHours(hours).toMinutes()

        val measures = mutableListOf<Measure>()
        if (hours != 0L) measures += Measure(hours, MeasureUnit.HOUR)
        if (minutes != 0L || measures.isEmpty()) measures += Measure(minutes, MeasureUnit.MINUTE)

        val formatted = MeasureFormat.getInstance(loc, MeasureFormat.FormatWidth.SHORT)
            .formatMeasures(*measures.toTypedArray())

        return if (isNegative) "-$formatted" else formatted
    }
}
