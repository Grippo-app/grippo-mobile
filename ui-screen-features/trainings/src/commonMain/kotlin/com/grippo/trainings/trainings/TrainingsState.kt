package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.metrics.engagement.DigestState
import com.grippo.core.state.trainings.TimelineState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.trainings_period_daily
import com.grippo.design.resources.provider.trainings_period_monthly
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Immutable
internal data class TrainingsState(
    val period: TrainingsTimelinePeriod = TrainingsTimelinePeriod.Monthly(),
    val date: DateRange = TrainingsTimelinePeriod.Monthly().defaultRange(),
    val limitations: DateRange = DateTimeUtils.trailingYear(),
)

/**
 * Each period owns the data it needs to render — Daily carries its flat timeline items,
 * Monthly carries its pre-built calendar (cells, digest, labels). This makes invalid
 * combinations (e.g. "Daily mode with monthly calendar data") unrepresentable.
 */
@Immutable
internal sealed interface TrainingsTimelinePeriod {
    val id: String
    val text: UiText

    fun defaultRange(): DateRange

    fun rangeFor(anchor: LocalDateTime): DateRange

    @Immutable
    data class Daily(
        val items: ImmutableList<TimelineState.Daily.Item> = persistentListOf(),
    ) : TrainingsTimelinePeriod {
        override val id: String = ID
        override val text: UiText = UiText.Res(Res.string.trainings_period_daily)

        override fun defaultRange(): DateRange = DateRangePresets.daily()

        override fun rangeFor(anchor: LocalDateTime): DateRange = DateRange(
            from = DateTimeUtils.startOfDay(anchor),
            to = DateTimeUtils.endOfDay(anchor),
        )

        companion object {
            const val ID: String = "daily"
        }
    }

    /**
     * Monthly period state — contains everything the monthly view needs to render,
     * pre-computed by the ViewModel. All dates are exposed as [DateFormatState];
     * raw LocalDate/LocalDateTime intentionally do not leak out of the state.
     */
    @Immutable
    data class Monthly(
        val monthReference: DateFormatState = defaultMonthReference(),
        val digest: DigestState? = null,
        val weekDayLabels: ImmutableList<String> = WeekDayLabels,
        val weeks: ImmutableList<ImmutableList<MonthlyCalendarDayState>> = persistentListOf(),
    ) : TrainingsTimelinePeriod {
        override val id: String = ID
        override val text: UiText = UiText.Res(Res.string.trainings_period_monthly)

        override fun defaultRange(): DateRange = DateRangePresets.monthly()

        override fun rangeFor(anchor: LocalDateTime): DateRange {
            val date = anchor.date
            val startOfMonth = LocalDate(date.year, date.month, 1)
            val lastDay = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
            return DateRange(
                from = DateTimeUtils.startOfDay(startOfMonth),
                to = DateTimeUtils.endOfDay(lastDay),
            )
        }

        companion object {
            const val ID: String = "monthly"
        }
    }

    companion object {
        /**
         * Stable, data-free variants used by the Segment tab selector.
         * Tab only needs [id] and [text]; the actual payload lives
         * inside the state's current [period].
         */
        val Variants: ImmutableList<TrainingsTimelinePeriod> =
            persistentListOf(Daily(), Monthly())

        /** Resolves a fresh variant instance (with default, empty payload) by id. */
        fun variantById(id: String): TrainingsTimelinePeriod? =
            Variants.firstOrNull { it.id == id }
    }
}

@Immutable
internal data class MonthlyCalendarDayState(
    val date: DateFormatState,
    /** Day number shown in the cell (1..31) — exposed separately so the UI
     *  never has to reach into [date] just to render the number. */
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val trainingCount: Int,
) {
    val isClickable: Boolean get() = isCurrentMonth && trainingCount > 0
}

/**
 * Builds a fully-populated [TrainingsTimelinePeriod.Monthly] snapshot from a timeline.
 * Accepts [range] rather than a raw LocalDate anchor so call sites
 * stay in terms of higher-level types.
 */
internal fun buildMonthlyPeriod(
    range: DateRange,
    timeline: ImmutableList<TimelineState>,
): TrainingsTimelinePeriod.Monthly {
    val anchor = range.from.date
    val monthAnchor = LocalDate(anchor.year, anchor.month, 1)
    val today = DateTimeUtils.now().date

    var digestEntry: TimelineState.MonthlyDigest? = null
    val trainingDays = mutableListOf<TimelineState.MonthlyTrainingsDay>()
    for (value in timeline) {
        when (value) {
            is TimelineState.MonthlyDigest -> if (digestEntry == null) digestEntry = value
            is TimelineState.MonthlyTrainingsDay -> trainingDays += value
            else -> Unit
        }
    }

    val trainingCountByDate = trainingDays
        .mapNotNull { day -> day.date.value?.let { it to day.trainings.size } }
        .toMap()

    val daysInMonth = DateTimeUtils.getDaysInMonth(monthAnchor.year, monthAnchor.month)
    val startOffset = (monthAnchor.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)
        .let { if (it < 0) it + 7 else it }
    val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7
    val firstDisplayedDate = monthAnchor.minus(DatePeriod(days = startOffset))

    val weeks = (0 until totalCells)
        .map { index ->
            val date = firstDisplayedDate.plus(DatePeriod(days = index))
            MonthlyCalendarDayState(
                date = date.asDayFormat(),
                dayOfMonth = date.day,
                isCurrentMonth = date.month == monthAnchor.month &&
                        date.year == monthAnchor.year,
                isToday = date == today,
                trainingCount = trainingCountByDate[date] ?: 0,
            )
        }
        .chunked(7)
        .map { it.toPersistentList() }
        .toPersistentList()

    return TrainingsTimelinePeriod.Monthly(
        monthReference = monthAnchor.asMonthFormat(),
        digest = digestEntry?.summary,
        weekDayLabels = WeekDayLabels,
        weeks = weeks,
    )
}

private fun defaultMonthReference(): DateFormatState {
    val today = DateTimeUtils.now().date
    return LocalDate(today.year, today.month, 1).asMonthFormat()
}

private fun LocalDate.asDayFormat(): DateFormatState = DateFormatState.of(
    value = this,
    range = DateRangePresets.infinity(),
    format = DateFormat.DateOnly.DateDdMmm,
)

private fun LocalDate.asMonthFormat(): DateFormatState = DateFormatState.of(
    value = this,
    range = DateRangePresets.infinity(),
    format = DateFormat.DateOnly.MmmYyyy,
)

/**
 * Monday-to-Sunday short labels, localized once at process start.
 * Recomputing per view is wasteful — this is stable within a locale.
 */
private val WeekDayLabels: ImmutableList<String> = run {
    val mondayReference = LocalDate(2023, 1, 2)
    DayOfWeek.entries
        .map { day ->
            val labelDate = mondayReference.plus(DatePeriod(days = day.ordinal))
            DateTimeUtils.format(labelDate, DateFormat.DateOnly.WeekdayShort)
        }
        .toPersistentList()
}
