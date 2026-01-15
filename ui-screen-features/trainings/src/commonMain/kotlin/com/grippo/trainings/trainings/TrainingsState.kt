package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.trainings.TimelineState
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.details
import com.grippo.design.resources.provider.edit_btn
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.selected
import com.grippo.design.resources.provider.trainings_period_daily
import com.grippo.design.resources.provider.trainings_period_monthly
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@Immutable
internal data class TrainingsState(
    val period: TrainingsTimelinePeriod = TrainingsTimelinePeriod.Monthly,
    val date: DateRange = TrainingsTimelinePeriod.Monthly.defaultRange(),
    val limitations: DateRange = run {
        val currentYear = DateRange.Range.Yearly().range
        val previousYear = DateTimeUtils.shift(currentYear, DatePeriod(years = -1))
        val nextYear = DateTimeUtils.shift(currentYear, DatePeriod(years = 1))
        DateRange(from = previousYear.from, to = nextYear.to)
    },
    val timeline: ImmutableList<TimelineState> = persistentListOf(),
)

@Immutable
internal enum class TrainingMenu(val id: String) {
    Details("overview"),
    Edit("edit"),
    Delete("delete");

    companion object {
        suspend fun title(stringProvider: StringProvider): String {
            return stringProvider.get(Res.string.selected)
        }

        fun of(id: String): TrainingMenu? {
            return entries.firstOrNull { it.id == id }
        }
    }

    suspend fun text(stringProvider: StringProvider): String {
        return when (this) {
            Delete -> stringProvider.get(Res.string.delete_btn)
            Edit -> stringProvider.get(Res.string.edit_btn)
            Details -> stringProvider.get(Res.string.details)
        }
    }
}

@Immutable
internal enum class TrainingsTimelinePeriod(
    val id: String,
    val text: UiText,
) {
    Daily(id = "daily", text = UiText.Res(Res.string.trainings_period_daily)),
    Monthly(id = "monthly", text = UiText.Res(Res.string.trainings_period_monthly));

    fun defaultRange(): DateRange = when (this) {
        Daily -> DateRange.Range.Daily().range
        Monthly -> DateRange.Range.Monthly().range
    }

    fun rangeFor(anchor: LocalDateTime): DateRange = when (this) {
        Daily -> DateRange(
            from = DateTimeUtils.startOfDay(anchor),
            to = DateTimeUtils.endOfDay(anchor)
        )

        Monthly -> {
            val date = anchor.date
            val startOfMonth = LocalDate(date.year, date.month, 1)
            val lastDay = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

            DateRange(
                from = DateTimeUtils.startOfDay(startOfMonth),
                to = DateTimeUtils.startOfDay(lastDay)
            )
        }
    }
}
