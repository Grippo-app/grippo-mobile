package com.grippo.data.features.api.metrics.volume

import com.grippo.data.features.api.metrics.volume.models.WeeklyVolume
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus

public class WeeklyVolumeUseCase {

    private companion object {
        private const val TYPICAL_WEEKS: Int = 4
    }

    // Ends at Sunday, not now: createdAt is server time and can be ahead of the device clock.
    public fun observationRange(now: LocalDateTime): DateRange {
        val weekRange = currentWeekRange(now)
        val typicalStart = weekRange.from.date.minus(DatePeriod(days = TYPICAL_WEEKS * 7))
        return DateRange(
            from = DateTimeUtils.startOfDay(typicalStart),
            to = weekRange.to,
        )
    }

    public fun fromTrainings(trainings: List<Training>, now: LocalDateTime): WeeklyVolume {
        val weekRange = currentWeekRange(now)
        val weekStart = weekRange.from.date
        val typicalStart = weekStart.minus(DatePeriod(days = TYPICAL_WEEKS * 7))
        val measured = trainings.filter { training ->
            training.volume.isFinite() && training.volume >= 0f
        }
        val thisWeek = measured.filter { training ->
            training.createdAt >= weekRange.from && training.createdAt <= weekRange.to
        }
        val pastVolumes = measured
            .filter { training -> training.createdAt.date in typicalStart..<weekStart }
            .map { training -> training.volume.toDouble() }

        return WeeklyVolume(
            weekRange = weekRange,
            currentVolume = thisWeek.sumOf { training -> training.volume.toDouble() }.toFloat(),
            trainingsThisWeek = thisWeek.size,
            typicalVolume = pastVolumes.takeIf { it.isNotEmpty() }?.let { it.sum() / TYPICAL_WEEKS }?.toFloat(),
        )
    }

    private fun currentWeekRange(now: LocalDateTime): DateRange {
        val weekStart = DateTimeUtils.startOfIsoWeek(now.date)
        return DateRange(
            from = DateTimeUtils.startOfDay(weekStart),
            to = DateTimeUtils.endOfDay(weekStart.plus(DatePeriod(days = 6))),
        )
    }
}
