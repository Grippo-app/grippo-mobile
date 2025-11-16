package com.grippo.toolkit.date.utils.internal

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.toJavaDuration

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

    actual override fun format(duration: Duration, localeTag: String?): String? {
        val loc = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault()
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
