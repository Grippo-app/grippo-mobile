package com.grippo.trainings.trainings

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.trainings.TrainingListValue
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.delete_btn
import com.grippo.design.resources.provider.edit_btn
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.selected
import com.grippo.design.resources.provider.trainings_period_daily
import com.grippo.design.resources.provider.trainings_period_monthly
import com.grippo.design.resources.provider.trainings_period_weekly
import com.grippo.design.resources.provider.view_stats_btn
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource

@Immutable
internal data class TrainingsState(
    val period: TrainingsTimelinePeriod = TrainingsTimelinePeriod.Daily,
    val date: DateRange = TrainingsTimelinePeriod.Daily.defaultRange(),
    val limitations: DateRange = DateTimeUtils.trailingYear(),
    val trainings: ImmutableMap<Int, ImmutableList<TrainingListValue>> = persistentMapOf(),
)

@Immutable
internal enum class TrainingMenu(val id: String) {
    Edit("edit"),
    Delete("delete"),
    Overview("overview");

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
            Overview -> stringProvider.get(Res.string.view_stats_btn)
        }
    }
}

@Immutable
internal enum class TrainingsTimelinePeriod(
    val id: String,
    val titleRes: StringResource,
) {
    Daily(id = "daily", titleRes = Res.string.trainings_period_daily),
    Weekly(id = "weekly", titleRes = Res.string.trainings_period_weekly),
    Monthly(id = "monthly", titleRes = Res.string.trainings_period_monthly);

    fun label(): UiText = UiText.Res(titleRes)

    fun defaultRange(): DateRange = when (this) {
        Daily -> DateTimeUtils.thisDay()
        Weekly -> DateTimeUtils.thisWeek()
        Monthly -> DateTimeUtils.thisMonth()
    }

    fun rangeFor(anchor: LocalDateTime): DateRange = when (this) {
        Daily -> DateRange(
            from = DateTimeUtils.startOfDay(anchor),
            to = DateTimeUtils.endOfDay(anchor)
        )

        Weekly -> {
            val date = anchor.date
            val daysToStart = date.dayOfWeek.ordinal
            val daysToEnd = DayOfWeek.SUNDAY.ordinal - date.dayOfWeek.ordinal
            val startDate = date.minus(DatePeriod(days = daysToStart))
            val endDate = date.plus(DatePeriod(days = daysToEnd))

            DateRange(
                from = DateTimeUtils.startOfDay(startDate),
                to = DateTimeUtils.startOfDay(endDate)
            )
        }

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