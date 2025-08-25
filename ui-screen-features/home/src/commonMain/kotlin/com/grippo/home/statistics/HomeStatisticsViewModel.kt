package com.grippo.home.statistics

import com.grippo.core.BaseViewModel
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.components.chart.DSRadarAxis
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSRadarSeries
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.components.chart.DSSparklinePoint
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.arms_muscle
import com.grippo.design.resources.provider.axis_day
import com.grippo.design.resources.provider.axis_hour
import com.grippo.design.resources.provider.axis_month
import com.grippo.design.resources.provider.back_muscle
import com.grippo.design.resources.provider.bench_press
import com.grippo.design.resources.provider.chest_muscle
import com.grippo.design.resources.provider.core_muscle
import com.grippo.design.resources.provider.deadlift
import com.grippo.design.resources.provider.legs_muscle
import com.grippo.design.resources.provider.muscle_group
import com.grippo.design.resources.provider.overhead_press
import com.grippo.design.resources.provider.percent
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.row_ex
import com.grippo.design.resources.provider.series_current
import com.grippo.design.resources.provider.shoulders_muscle
import com.grippo.design.resources.provider.squat
import com.grippo.design.resources.provider.strength_progress
import com.grippo.design.resources.provider.volume
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.state.datetime.PeriodState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

internal class HomeStatisticsViewModel(
    private val dialogController: DialogController,
    private val colorProvider: ColorProvider,
    private val stringProvider: StringProvider
) : BaseViewModel<HomeStatisticsState, HomeStatisticsDirection, HomeStatisticsLoader>(
    HomeStatisticsState()
), HomeStatisticsContract {

    private var themeColors: AppColor? = null

    init {
        // Resolve theme-aware colors once, then generate all chart data with these colors
        safeLaunch {
            val colors = colorProvider.get()
            themeColors = colors
            regenerateAllFor(state.value.period, colors)
        }
    }

    override fun onSelectPeriod() {
        val custom = (state.value.period as? PeriodState.CUSTOM) ?: PeriodState.CUSTOM(
            range = DateTimeUtils.thisWeek(),
            limitations = DateTimeUtils.trailingYear()
        )

        val available = listOf(
            PeriodState.ThisDay,
            PeriodState.ThisWeek,
            PeriodState.ThisMonth,
            custom
        )

        val dialog = DialogConfig.PeriodPicker(
            initial = state.value.period,
            available = available,
            onResult = { value ->
                // First update period in state
                update { it.copy(period = value) }
                // Then regenerate charts for the newly selected period
                safeLaunch {
                    val colors = themeColors ?: colorProvider.get().also { themeColors = it }
                    regenerateAllFor(value, colors)
                }
            }
        )

        dialogController.show(dialog)
    }

    override fun onFiltersClick() {
        val dialog = DialogConfig.FilterPicker(
            initial = persistentListOf(),
            onResult = { value ->
            }
        )

        dialogController.show(dialog)
    }

    override fun onBack() {
        navigateTo(HomeStatisticsDirection.Back)
    }

    private suspend fun generateAreaData(period: PeriodState, colors: AppColor) {
        val labels = buildTimelineLabels(period)
        val values = labels.indices.map { i -> (6f + (i % 7) * 1.6f + (i % 3)) }
        val points =
            values.mapIndexed { i, v -> DSAreaPoint(x = i.toFloat(), y = v, xLabel = labels[i]) }
        update { it.copy(areaData = DSAreaData(points = points)) }
    }

    private suspend fun generateBarData(period: PeriodState, colors: AppColor) {
        val palette = colors.charts.categorical.palette
        val timeLabels = buildTimelineLabels(period)
        val entries: List<Pair<String, Float>> = timeLabels.mapIndexed { i, l ->
            l to (5f + (i % 6) * 2 + (i % 4))
        }
        val items = entries.mapIndexed { i, (label, value) ->
            DSBarItem(label = label, value = value, color = palette[i % palette.size])
        }
        val xName = when (deduceGranularity(period)) {
            Granularity.Hour -> stringProvider.get(Res.string.axis_hour)
            Granularity.WeekDayNames -> stringProvider.get(Res.string.axis_day)
            Granularity.Day -> stringProvider.get(Res.string.axis_day)
            Granularity.Month -> stringProvider.get(Res.string.axis_month)
        }
        val yName = stringProvider.get(Res.string.volume)
        update {
            it.copy(
                barData = DSBarData(
                    items = items,
                    xName = xName,
                    yName = yName,
                    yUnit = null
                )
            )
        }
    }

    private suspend fun generateHeatmapData(period: PeriodState, colors: AppColor) {
        val rows = 6
        val labelsRow = listOf(
            stringProvider.get(Res.string.chest_muscle),
            stringProvider.get(Res.string.back_muscle),
            stringProvider.get(Res.string.legs_muscle),
            stringProvider.get(Res.string.shoulders_muscle),
            stringProvider.get(Res.string.arms_muscle),
            stringProvider.get(Res.string.core_muscle),
        )
        val labelsCol = buildTimelineLabels(period)
        val cols = labelsCol.size

        val values01 = List(rows * cols) { idx ->
            val r = idx / cols
            val c = idx % cols
            val base = (r + 1) * (c + 3)
            ((base % 10) / 10f)
        }

        val rowDimText = stringProvider.get(Res.string.muscle_group)
        val colDimText = when (deduceGranularity(period)) {
            Granularity.Hour -> stringProvider.get(Res.string.axis_hour)
            Granularity.WeekDayNames -> stringProvider.get(Res.string.axis_day)
            Granularity.Day -> stringProvider.get(Res.string.axis_day)
            Granularity.Month -> stringProvider.get(Res.string.axis_month)
        }

        update {
            it.copy(
                heatmapData = DSHeatmapData(
                    rows = rows,
                    cols = cols,
                    values01 = values01,
                    rowLabels = labelsRow,
                    colLabels = labelsCol,
                    rowDim = rowDimText,
                    colDim = colDimText,
                    valueUnit = null,
                )
            )
        }
    }

    private suspend fun generateRadarData(period: PeriodState, colors: AppColor) {
        val axes = listOf(
            DSRadarAxis("chest", stringProvider.get(Res.string.chest_muscle)),
            DSRadarAxis("back", stringProvider.get(Res.string.back_muscle)),
            DSRadarAxis("legs", stringProvider.get(Res.string.legs_muscle)),
            DSRadarAxis("shoulders", stringProvider.get(Res.string.shoulders_muscle)),
            DSRadarAxis("arms", stringProvider.get(Res.string.arms_muscle)),
            DSRadarAxis("core", stringProvider.get(Res.string.core_muscle)),
        )
        val palette = colors.charts.radar.palette
        val factor = when (period) {
            PeriodState.ThisDay -> 0.95f
            PeriodState.ThisWeek -> 1.0f
            PeriodState.ThisMonth -> 0.9f
            is PeriodState.CUSTOM -> 0.85f
        }
        val current = DSRadarSeries(
            name = stringProvider.get(Res.string.series_current),
            color = palette.firstOrNull() ?: colors.text.primary,
            valuesByAxisId = mapOf(
                "chest" to 0.75f * factor,
                "back" to 0.6f * factor,
                "legs" to 0.9f * factor,
                "shoulders" to 0.55f * factor,
                "arms" to 0.7f * factor,
                "core" to 0.5f * factor
            )
        )
        update { it.copy(radarData = DSRadarData(axes = axes, series = listOf(current))) }
    }

    private suspend fun generateProgressData(period: PeriodState, colors: AppColor) {
        val palette = colors.charts.progress.palette
        val shift = when (period) {
            PeriodState.ThisDay -> -4f
            PeriodState.ThisWeek -> 0f
            PeriodState.ThisMonth -> 3f
            is PeriodState.CUSTOM -> -2f
        }
        val clamp: (Float) -> Float = { v -> v.coerceIn(0f, 100f) }
        val items = listOf(
            DSProgressItem(
                stringProvider.get(Res.string.bench_press),
                clamp(72f + shift),
                palette[0 % palette.size]
            ),
            DSProgressItem(
                stringProvider.get(Res.string.deadlift),
                clamp(100f + shift),
                palette[1 % palette.size]
            ),
            DSProgressItem(
                stringProvider.get(Res.string.squat),
                clamp(86f + shift),
                palette[2 % palette.size]
            ),
            DSProgressItem(
                stringProvider.get(Res.string.overhead_press),
                clamp(58f + shift),
                palette[3 % palette.size]
            ),
            DSProgressItem(
                stringProvider.get(Res.string.row_ex),
                clamp(64f + shift),
                palette[4 % palette.size]
            ),
        )
        val percent = stringProvider.get(Res.string.percent)
        val title = stringProvider.get(Res.string.strength_progress)
        update {
            it.copy(
                progressData = DSProgressData(
                    items = items,
                    valueUnit = percent,
                    title = title
                )
            )
        }
    }

    private suspend fun generateSparklineData(period: PeriodState, colors: AppColor) {
        val count = buildTimelineLabels(period).size
        val values: List<Float> = List(count) { i -> (4 + (i % 6) + (i % 4)).toFloat() }
        val points = values.mapIndexed { i, v -> DSSparklinePoint(x = i.toFloat(), y = v) }
        update { it.copy(sparklineData = DSSparklineData(points = points)) }
    }

    private suspend fun generateVolumeData(period: PeriodState, colors: AppColor) {
        val baseVolume = when (period) {
            PeriodState.ThisDay -> 1250f
            PeriodState.ThisWeek -> 8750f
            PeriodState.ThisMonth -> 35000f
            is PeriodState.CUSTOM -> {
                val daysInclusive = inclusiveDays(period.range)
                when {
                    daysInclusive <= 1L -> 1250f
                    daysInclusive in 2L..7L -> 8750f
                    daysInclusive in 8L..31L -> 35000f
                    else -> 105000f
                }
            }
        }
        val volume = VolumeFormatState.of(baseVolume)
        update { it.copy(volume = volume) }
    }

    private suspend fun generateRepetitionsData(period: PeriodState, colors: AppColor) {
        val baseRepetitions = when (period) {
            PeriodState.ThisDay -> 45
            PeriodState.ThisWeek -> 315
            PeriodState.ThisMonth -> 1260
            is PeriodState.CUSTOM -> {
                val daysInclusive = inclusiveDays(period.range)
                when {
                    daysInclusive <= 1L -> 45
                    daysInclusive in 2L..7L -> 315
                    daysInclusive in 8L..31L -> 1260
                    else -> 3780
                }
            }
        }
        val repetitions = RepetitionsFormatState.of(baseRepetitions)
        update { it.copy(repetitions = repetitions) }
    }

    private suspend fun generateIntensityData(period: PeriodState, colors: AppColor) {
        val baseIntensity = when (period) {
            PeriodState.ThisDay -> 75f
            PeriodState.ThisWeek -> 78f
            PeriodState.ThisMonth -> 82f
            is PeriodState.CUSTOM -> {
                val daysInclusive = inclusiveDays(period.range)
                when {
                    daysInclusive <= 1L -> 75f
                    daysInclusive in 2L..7L -> 78f
                    daysInclusive in 8L..31L -> 82f
                    else -> 85f
                }
            }
        }
        val intensity = IntensityFormatState.of(baseIntensity)
        update { it.copy(intensity = intensity) }
    }

    private suspend fun regenerateAllFor(period: PeriodState, colors: AppColor) {
        generateAreaData(period, colors)
        generateBarData(period, colors)
        generateHeatmapData(period, colors)
        generateRadarData(period, colors)
        generateProgressData(period, colors)
        generateSparklineData(period, colors)
        generateVolumeData(period, colors)
        generateRepetitionsData(period, colors)
        generateIntensityData(period, colors)
    }

    // --- timeline helpers ---
    private enum class Granularity { Hour, WeekDayNames, Day, Month }

    private fun deduceGranularity(period: PeriodState): Granularity {
        return when (period) {
            PeriodState.ThisDay -> Granularity.Hour
            PeriodState.ThisWeek -> Granularity.WeekDayNames
            PeriodState.ThisMonth -> Granularity.Day
            is PeriodState.CUSTOM -> {
                val daysInclusive = inclusiveDays(period.range)
                when {
                    daysInclusive <= 1L -> Granularity.Hour
                    daysInclusive in 2L..7L -> Granularity.WeekDayNames
                    daysInclusive in 8L..31L -> Granularity.Day
                    else -> Granularity.Month
                }
            }
        }
    }

    private fun buildTimelineLabels(period: PeriodState): List<String> {
        val rng = period.range
        return when (deduceGranularity(period)) {
            Granularity.Hour -> buildHours(rng)
            Granularity.WeekDayNames -> buildWeekDays(rng)
            Granularity.Day -> buildDays(rng)
            Granularity.Month -> buildMonths(rng)
        }
    }

    private fun buildHours(range: com.grippo.date.utils.DateRange): List<String> {
        val date = range.from.date
        return (0 until 24 step 2).map { h ->
            val t = LocalDateTime(date.year, date.month, date.dayOfMonth, h, 0)
            DateTimeUtils.format(t, DateFormat.TIME_24H_H_MM)
        }
    }

    private fun buildDays(range: com.grippo.date.utils.DateRange): List<String> {
        val startDate: LocalDate = range.from.date
        val endDate: LocalDate = range.to.date
        val labels = mutableListOf<String>()
        var cur = startDate
        while (cur <= endDate) {
            val ldt = LocalDateTime(cur.year, cur.month, cur.dayOfMonth, 0, 0)
            labels += DateTimeUtils.format(ldt, DateFormat.DATE_DD_MMM)
            cur = cur.plus(DatePeriod(days = 1))
        }
        return labels
    }

    private fun buildMonths(range: com.grippo.date.utils.DateRange): List<String> {
        val start = range.from.date
        val end = range.to.date
        val labels = mutableListOf<String>()
        var y = start.year
        var m = start.month
        fun incMonth() {
            val next = m.ordinal + 1
            if (next >= kotlinx.datetime.Month.entries.size) {
                m = kotlinx.datetime.Month.JANUARY
                y += 1
            } else {
                m = kotlinx.datetime.Month.entries[next]
            }
        }

        var cur = LocalDate(y, m, 1)
        while (cur <= LocalDate(end.year, end.month, 1)) {
            val ldt = LocalDateTime(cur.year, cur.month, 1, 0, 0)
            labels += DateTimeUtils.format(ldt, DateFormat.MONTH_SHORT)
            incMonth()
            cur = LocalDate(y, m, 1)
        }
        return labels
    }

    private fun buildWeekDays(range: com.grippo.date.utils.DateRange): List<String> {
        val startDate: LocalDate = range.from.date
        val endDate: LocalDate = range.to.date
        val labels = mutableListOf<String>()
        var cur = startDate
        while (cur <= endDate) {
            val ldt = LocalDateTime(cur.year, cur.month, cur.dayOfMonth, 0, 0)
            labels += DateTimeUtils.format(ldt, DateFormat.WEEKDAY_SHORT)
            cur = cur.plus(DatePeriod(days = 1))
        }
        return labels
    }

    private fun inclusiveDays(range: com.grippo.date.utils.DateRange): Long {
        val startDate: LocalDate = range.from.date
        val endDate: LocalDate = range.to.date
        var count = 0L
        var cur = startDate
        while (cur <= endDate) {
            count++
            cur = cur.plus(DatePeriod(days = 1))
        }
        return count
    }
}