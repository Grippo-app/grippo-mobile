package com.grippo.toolkit.date.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.max

@Serializable
public data class DateRange(
    val from: LocalDateTime,
    val to: LocalDateTime,
) {

    @Serializable
    public sealed class Range {
        @Transient
        public open val range: DateRange? = null

        @Serializable
        public data class Daily(override val range: DateRange = DateTimeUtils.thisDay()) :
            Range()

        @Serializable
        public data class Weekly(override val range: DateRange = DateTimeUtils.thisWeek()) :
            Range()

        @Serializable
        public data class Last7Days(override val range: DateRange = DateTimeUtils.trailingWeek()) :
            Range()

        @Serializable
        public data class Last14Days(override val range: DateRange = DateTimeUtils.trailing14Days()) :
            Range()

        @Serializable
        public data class Monthly(override val range: DateRange = DateTimeUtils.thisMonth()) :
            Range()

        @Serializable
        public data class Last30Days(override val range: DateRange = DateTimeUtils.trailingMonth()) :
            Range()

        @Serializable
        public data class Last60Days(override val range: DateRange = DateTimeUtils.trailing60Days()) :
            Range()

        @Serializable
        public data class Last365Days(override val range: DateRange = DateTimeUtils.trailingYear()) :
            Range()

        @Serializable
        public data class Yearly(override val range: DateRange = DateTimeUtils.thisYear()) :
            Range()

        @Serializable
        public data class Undefined(override val range: DateRange? = null) : Range()

        @Serializable
        public data class Infinity(override val range: DateRange = DateTimeUtils.infinity()) :
            Range()
    }

    public fun range(): Range {
        val days = max(0, from.date.daysUntil(to.date) + 1)

        return when {
            matches(DateTimeUtils.thisDay()) -> Range.Daily(this)
            matches(DateTimeUtils.thisWeek()) -> Range.Weekly(this)
            matches(DateTimeUtils.trailingWeek()) -> Range.Last7Days(this)
            matches(DateTimeUtils.trailing14Days()) -> Range.Last14Days(this)
            matches(DateTimeUtils.thisMonth()) -> Range.Monthly(this)
            matches(DateTimeUtils.trailingMonth()) -> Range.Last30Days(this)
            matches(DateTimeUtils.trailing60Days()) -> Range.Last60Days(this)
            matches(DateTimeUtils.trailingYear()) -> Range.Last365Days(this)
            matches(DateTimeUtils.thisYear()) -> Range.Yearly(this)
            days == 1 -> Range.Daily(this)
            days == 7 -> Range.Last7Days(this)
            days == 14 -> Range.Last14Days(this)
            days == 30 -> Range.Last30Days(this)
            days == 60 -> Range.Last60Days(this)
            days in 28..31 -> Range.Monthly(this)
            days in 365..366 -> Range.Yearly(this)
            else -> Range.Undefined(this)
        }
    }

    private fun matches(other: DateRange): Boolean {
        return from == other.from && to == other.to
    }
}

public operator fun DateRange.contains(ts: LocalDateTime): Boolean {
    return (ts >= from) && (ts <= to)
}
