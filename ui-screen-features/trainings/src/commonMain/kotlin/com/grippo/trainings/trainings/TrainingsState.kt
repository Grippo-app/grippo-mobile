package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateFormatState
import com.grippo.core.state.formatters.DateRangeFormatState
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

@Immutable
internal sealed interface TrainingsTimelinePeriod {
    val id: String
    val text: UiText

    fun defaultRange(): DateRange

    fun rangeFor(anchor: LocalDateTime): DateRange

    fun effectiveRange(anchor: LocalDateTime, limitations: DateRange): DateRange

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

        override fun effectiveRange(
            anchor: LocalDateTime,
            limitations: DateRange,
        ): DateRange = rangeFor(anchor).coerceWithin(limitations)

        companion object {
            const val ID: String = "daily"
        }
    }

    @Immutable
    data class Monthly(
        val monthReference: DateFormatState = defaultMonthReference(),
        val digest: DigestState = DigestState.Empty(
            range = DateRangeFormatState.of(DateRangePresets.monthly()),
        ),
        val weekDayLabels: ImmutableList<String> = DateTimeUtils.weekDayShortLabels()
            .toPersistentList(),
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

        override fun effectiveRange(
            anchor: LocalDateTime,
            limitations: DateRange,
        ): DateRange = rangeFor(anchor)

        companion object {
            const val ID: String = "monthly"

            fun from(
                range: DateRange,
                timeline: ImmutableList<TimelineState>,
            ): Monthly {
                val anchor = range.from.date
                val monthAnchor = LocalDate(anchor.year, anchor.month, 1)
                val today = DateTimeUtils.now().date

                var digestEntry: TimelineState.MonthlyDigest? = null
                val trainingDays = mutableListOf<TimelineState.MonthlyTrainingsDay>()
                for (value in timeline) {
                    when (value) {
                        is TimelineState.MonthlyDigest ->
                            if (digestEntry == null) digestEntry = value

                        is TimelineState.MonthlyTrainingsDay -> trainingDays += value
                        else -> Unit
                    }
                }

                val trainingCountByDate = trainingDays
                    .mapNotNull { day -> day.date.value?.let { it to day.trainings.size } }
                    .toMap()

                val daysInMonth = DateTimeUtils
                    .getDaysInMonth(monthAnchor.year, monthAnchor.month)

                val startOffset =
                    (monthAnchor.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber)
                        .let { if (it < 0) it + 7 else it }

                val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

                val firstDisplayedDate = monthAnchor
                    .minus(DatePeriod(days = startOffset))

                val weeks = (0 until totalCells)
                    .map { index ->
                        val date = firstDisplayedDate.plus(DatePeriod(days = index))
                        MonthlyCalendarDayState(
                            date = DateFormatState.of(
                                value = date,
                                range = DateRangePresets.infinity(),
                                format = DateFormat.DateOnly.DateDdMmm,
                            ),
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

                return Monthly(
                    monthReference = DateFormatState.of(
                        value = monthAnchor,
                        range = DateRangePresets.infinity(),
                        format = DateFormat.DateOnly.MmmYyyy,
                    ),
                    digest = digestEntry?.summary
                        ?: DigestState.Empty(range = DateRangeFormatState.of(range)),
                    weeks = weeks,
                )
            }

            private fun defaultMonthReference(): DateFormatState {
                val today = DateTimeUtils.now().date
                return DateFormatState.of(
                    value = LocalDate(today.year, today.month, 1),
                    range = DateRangePresets.infinity(),
                    format = DateFormat.DateOnly.MmmYyyy,
                )
            }
        }
    }

    companion object {
        val Variants: ImmutableList<TrainingsTimelinePeriod> =
            persistentListOf(Daily(), Monthly())

        fun variantById(id: String): TrainingsTimelinePeriod? =
            Variants.firstOrNull { it.id == id }
    }
}

@Immutable
internal data class MonthlyCalendarDayState(
    val date: DateFormatState,
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val trainingCount: Int,
) {
    val isClickable: Boolean get() = isCurrentMonth && trainingCount > 0
}
