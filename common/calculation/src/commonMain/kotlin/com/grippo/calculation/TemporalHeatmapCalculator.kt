package com.grippo.calculation

import com.grippo.calculation.models.Bucket
import com.grippo.calculation.models.BucketScale
import com.grippo.calculation.models.Instruction
import com.grippo.date.utils.DateFormat
import com.grippo.date.utils.DateRange
import com.grippo.date.utils.DateTimeUtils
import com.grippo.design.components.chart.DSHeatmapData
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.tooltip_muscle_load_description_day
import com.grippo.design.resources.provider.tooltip_muscle_load_description_month
import com.grippo.design.resources.provider.tooltip_muscle_load_description_training
import com.grippo.design.resources.provider.tooltip_muscle_load_description_year
import com.grippo.design.resources.provider.tooltip_muscle_load_title_day
import com.grippo.design.resources.provider.tooltip_muscle_load_title_month
import com.grippo.design.resources.provider.tooltip_muscle_load_title_training
import com.grippo.design.resources.provider.tooltip_muscle_load_title_year
import com.grippo.design.resources.provider.w
import com.grippo.state.datetime.PeriodState
import com.grippo.state.exercise.examples.ExerciseExampleState
import com.grippo.state.formatters.UiText
import com.grippo.state.muscles.MuscleEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 🔥 Temporal Heatmap (Muscle × Time)
 *
 * Builds a muscle-by-time heatmap of workload aggregated over the selected period.
 * - X axis = time buckets (DAY/WEEK/MONTH depending on PeriodState)
 * - Y axis = muscle groups (or individual muscles if no groups are provided)
 * - Cell value = Σ workload for that muscle (Volume or Reps, auto-chosen like in LoadCalculator)
 *
 * Output values are normalized to [0..1] for coloring via your OrangeRed gradient.
 *
 * Inputs:
 * - trainings: List<TrainingState>
 * - period: PeriodState
 * - examples: List<ExerciseExampleState> (for muscle bundles and shares)
 * - groups: List<MuscleGroupState<MuscleRepresentationState.Plain>> (rows; falls back to muscles)
 */
public class TemporalHeatmapCalculator(
    private val stringProvider: StringProvider
) {

    // -------------------------- Public API --------------------------

    /** 📊 Heatmap: rows = muscle groups, cols = time buckets (day/week/month), values01 normalized. */
    public suspend fun calculateMuscleGroupHeatmapFromTrainings(
        trainings: List<TrainingState>,
        period: PeriodState,
        examples: List<ExerciseExampleState>,
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
    ): Pair<DSHeatmapData, Instruction> {
        val inRange = trainings.filter { it.createdAt in period.range }
        val scale = deriveScale(period)

        // Buckets along X (time)
        val buckets = buildBuckets(period.range, scale)
        val colLabels = defaultTimeLabels(buckets, scale)

        // Row dimension = provided muscle groups, otherwise individual muscles encountered
        val rowSpec = buildRowSpec(groups, examples)
        val rows = rowSpec.labels.size
        val cols = buckets.size.coerceAtLeast(1)

        // Auto workload choice using ALL exercises in-range (stable across buckets)
        val allExercises = inRange.flatMap { it.exercises }
        val workload = chooseWorkload(allExercises)

        // Pre-index for lookups
        val exampleMap = examples.associateBy { it.value.id }

        // Accumulate raw values (not normalized) per (row, col)
        val raw = FloatArray(rows * cols) { 0f }

        // Iterate buckets → sum workload per muscle → roll up to group rows
        buckets.forEachIndexed { colIdx, b ->
            // trainings inside the bucket
            val ts = inRange.asSequence().filter { it.belongsTo(b) }.toList()
            if (ts.isEmpty()) return@forEachIndexed

            // Per-muscle absolute workload in this bucket (Volume or Reps)
            val perMuscle = computeBucketMuscleWorkload(
                trainings = ts,
                examples = exampleMap,
                workload = workload
            )

            if (perMuscle.isEmpty()) return@forEachIndexed

            // Roll-up into row indices (group → sum of its muscles)
            rowSpec.rows.forEachIndexed { rowIdx, row ->
                val v = when (row) {
                    is Row.Group -> row.muscles.sumOf { (perMuscle[it] ?: 0f).toDouble() }.toFloat()
                    is Row.Single -> perMuscle[row.muscle] ?: 0f
                }
                raw[rowIdx * cols + colIdx] = v
            }
        }

        // Normalize to [0..1] within the current view
        val maxVal = raw.maxOrNull() ?: 0f
        val values01 = normalize01(raw, maxVal).toList()

        val data = DSHeatmapData(
            rows = rows,
            cols = cols,
            values01 = values01,
            rowLabels = rowSpec.labels,
            colLabels = colLabels,
            rowDim = if (rowSpec.usesGroups) "MuscleGroup" else "Muscle",
            colDim = when (scale) {
                BucketScale.DAY -> "Day"
                BucketScale.WEEK -> "Week"
                BucketScale.MONTH -> "Month"
                BucketScale.EXERCISE -> "Day"
            },
            valueUnit = when (workload) {
                Workload.Volume -> "kg·reps"
                Workload.Reps -> "reps"
            }
        )

        val tip = instructionForMuscleHeatmap(period) // reuses your muscle-load copy per scale
        return data to tip
    }

    // -------------------------- Row spec (groups or singles) --------------------------

    private sealed interface Row {
        data class Group(val name: String, val muscles: List<MuscleEnumState>) : Row
        data class Single(val name: String, val muscle: MuscleEnumState) : Row
    }

    private data class RowSpec(
        val rows: List<Row>,
        val labels: List<String>,
        val usesGroups: Boolean
    )

    /** If groups provided → use them (stable order). Else derive singles from examples. */
    private suspend fun buildRowSpec(
        groups: List<MuscleGroupState<MuscleRepresentationState.Plain>>,
        examples: List<ExerciseExampleState>
    ): RowSpec {
        return if (groups.isNotEmpty()) {
            val rows = groups.map { g ->
                Row.Group(
                    name = g.type.title().text(stringProvider),
                    muscles = g.muscles.map { it.value.type }
                )
            }
            RowSpec(rows, rows.map { it.name }, usesGroups = true)
        } else {
            // Derive all muscles present in example bundles to keep grid compact
            val muscles: LinkedHashSet<MuscleEnumState> = linkedSetOf()
            examples.forEach { ex ->
                ex.bundles.forEach { b -> muscles += b.muscle.type }
            }
            val rows = muscles.map { m -> Row.Single(name = m.toString(), muscle = m) }
            RowSpec(rows, rows.map { it.name }, usesGroups = false)
        }
    }

    // -------------------------- Bucket aggregation --------------------------

    /** Compute absolute workload per muscle inside a single bucket. */
    private fun computeBucketMuscleWorkload(
        trainings: List<TrainingState>,
        examples: Map<String, ExerciseExampleState>,
        workload: Workload
    ): Map<MuscleEnumState, Float> {
        val perMuscle = mutableMapOf<MuscleEnumState, Float>()

        trainings.forEach { t ->
            t.exercises.forEach { ex ->
                val exampleId = ex.exerciseExample?.id ?: return@forEach
                val example = examples[exampleId] ?: return@forEach

                val exWorkload = when (workload) {
                    Workload.Volume -> ex.iterations.fold(0f) { acc, itn ->
                        val w = (itn.volume.value ?: 0f)
                        val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                        val load = if (w > WEIGHT_EPS_KG) w else 0f
                        if (r == 0 || load <= 0f) acc else acc + load * r
                    }

                    Workload.Reps -> ex.iterations.fold(0) { acc, itn ->
                        acc + (itn.repetitions.value ?: 0).coerceAtLeast(0)
                    }.toFloat()
                }.coerceAtLeast(0f)

                if (exWorkload <= 0f) return@forEach

                // Distribute by example bundles (% shares)
                val denom = example.bundles.fold(0f) { acc, b ->
                    acc + (b.percentage.value ?: 0).coerceAtLeast(0).toFloat()
                }
                if (denom <= 0f) return@forEach

                example.bundles.forEach { b ->
                    val p = (b.percentage.value ?: 0).coerceAtLeast(0)
                    if (p == 0) return@forEach
                    val share = p / denom
                    val muscle = b.muscle.type
                    perMuscle[muscle] = (perMuscle[muscle] ?: 0f) + exWorkload * share
                }
            }
        }

        return perMuscle
    }

    // -------------------------- Workload choice (same semantics as LoadCalculator) --------------------------

    private sealed interface Workload {
        data object Volume : Workload
        data object Reps : Workload
    }

    private fun chooseWorkload(exercises: List<ExerciseState>): Workload {
        var tonnageLike = 0.0
        exercises.forEach { ex ->
            ex.iterations.forEach { itn ->
                val w = (itn.volume.value ?: 0f)
                val r = (itn.repetitions.value ?: 0).coerceAtLeast(0)
                val load = if (w > WEIGHT_EPS_KG) w else 0f
                if (r > 0 && load > 0f) tonnageLike += (load * r)
            }
        }
        return if (tonnageLike > 0.0) Workload.Volume else Workload.Reps
    }

    // -------------------------- Instructions --------------------------

    /** Reuse your muscle-load copy per scale (titles/descriptions already exist in your strings). */
    private fun instructionForMuscleHeatmap(period: PeriodState): Instruction {
        val scale = deriveScale(period)
        val (titleRes, descRes) = when (scale) {
            BucketScale.EXERCISE ->
                Res.string.tooltip_muscle_load_title_training to
                        Res.string.tooltip_muscle_load_description_training

            BucketScale.DAY ->
                Res.string.tooltip_muscle_load_title_day to
                        Res.string.tooltip_muscle_load_description_day

            BucketScale.WEEK ->
                Res.string.tooltip_muscle_load_title_month to
                        Res.string.tooltip_muscle_load_description_month

            BucketScale.MONTH -> {
                val days = daysInclusive(period.range.from.date, period.range.to.date)
                if (days >= 365)
                    Res.string.tooltip_muscle_load_title_year to
                            Res.string.tooltip_muscle_load_description_year
                else
                    Res.string.tooltip_muscle_load_title_month to
                            Res.string.tooltip_muscle_load_description_month
            }
        }
        return Instruction(title = UiText.Res(titleRes), description = UiText.Res(descRes))
    }

    // -------------------------- Time bucketing --------------------------

    private fun buildBuckets(range: DateRange, scale: BucketScale): List<Bucket> = when (scale) {
        BucketScale.DAY -> days(range.from, range.to)
        BucketScale.WEEK -> weeks(range.from, range.to)
        BucketScale.MONTH -> months(range.from, range.to)
        BucketScale.EXERCISE -> days(range.from, range.to) // single-day behaves as 1 column
    }

    private suspend fun defaultTimeLabels(buckets: List<Bucket>, scale: BucketScale): List<String> {
        val w = stringProvider.get(Res.string.w)
        return when (scale) {
            BucketScale.DAY -> buckets.map {
                DateTimeUtils.format(
                    it.start,
                    DateFormat.DATE_DD_MMM
                )
            }   // e.g., "Mon 02"
            BucketScale.WEEK -> buckets.map {
                "$w${isoWeekNumber(it.start)}-${
                    DateTimeUtils.format(
                        it.start,
                        DateFormat.MONTH_SHORT
                    )
                }"
            }  // e.g., "W36 Sep"
            BucketScale.MONTH -> buckets.map {
                DateTimeUtils.format(
                    it.start,
                    DateFormat.MONTH_SHORT
                )
            }                                   // e.g., "Sep"
            BucketScale.EXERCISE -> buckets.map {
                DateTimeUtils.format(
                    it.start,
                    DateFormat.DATE_DD_MMM
                )
            }
        }
    }

    // ---- Day/Week/Month ranges (inclusive) ----
    private fun days(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var d = from.date
        val end = to.date
        while (d <= end) {
            val start = maxDT(LocalDateTime(d, LocalTime(0, 0)), from)
            val stop = minDT(LocalDateTime(d, LocalTime(23, 59, 59, 999_000_000)), to)
            out += Bucket(start, stop)
            d = d.plus(DatePeriod(days = 1))
        }
        return out
    }

    private fun weeks(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var wStart = startOfWeek(from)
        while (wStart <= to) {
            val wEndDate = wStart.date.plus(DatePeriod(days = 6))
            val wEnd = LocalDateTime(wEndDate, LocalTime(23, 59, 59, 999_000_000))
            out += Bucket(maxDT(wStart, from), minDT(wEnd, to))
            wStart = LocalDateTime(wStart.date.plus(DatePeriod(days = 7)), LocalTime(0, 0))
        }
        return out
    }

    private fun months(from: LocalDateTime, to: LocalDateTime): List<Bucket> {
        val out = mutableListOf<Bucket>()
        var mStart = startOfMonth(from)
        while (mStart <= to) {
            val firstOfNext =
                LocalDate(mStart.year, mStart.monthNumber, 1).plus(DatePeriod(months = 1))
            val lastDay = firstOfNext.minus(DatePeriod(days = 1))
            val mEnd = LocalDateTime(lastDay, LocalTime(23, 59, 59, 999_000_000))
            out += Bucket(maxDT(mStart, from), minDT(mEnd, to))
            mStart = LocalDateTime(mStart.date.plus(DatePeriod(months = 1)), LocalTime(0, 0))
        }
        return out
    }

    // ---- Labels & week helpers ----
    private fun startOfWeek(d: LocalDateTime): LocalDateTime {
        val shift = d.date.dayOfWeek.isoDayNumber - 1
        val monday = d.date.minus(DatePeriod(days = shift))
        return LocalDateTime(monday, LocalTime(0, 0))
    }

    private fun startOfMonth(d: LocalDateTime): LocalDateTime =
        LocalDateTime(LocalDate(d.year, d.monthNumber, 1), LocalTime(0, 0))

    private fun isoWeekNumber(weekStartMonday: LocalDateTime): Int {
        val date = weekStartMonday.date
        val firstJan = LocalDate(date.year, 1, 1)
        val doy = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
        return ((doy - 1) / 7) + 1
    }

    // -------------------------- Math & utils --------------------------

    /** Normalize in place to [0..1]; if max==0 → keep zeros. */
    private fun normalize01(values: FloatArray, max: Float): FloatArray {
        if (max <= 0f) return values
        for (i in values.indices) values[i] = (values[i] / max).coerceIn(0f, 1f)
        return values
    }

    private fun TrainingState.belongsTo(bucket: Bucket): Boolean =
        createdAt >= bucket.start && createdAt <= bucket.end

    private fun minDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a <= b) a else b
    private fun maxDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a >= b) a else b

    // -------------------------- Config --------------------------

    private companion object {
        private const val WEIGHT_EPS_KG: Float = 0.5f
    }

    // -------------------------- Scale derivation (same policy you used) --------------------------

    private fun deriveScale(period: PeriodState): BucketScale = when (period) {
        is PeriodState.ThisDay -> BucketScale.DAY
        is PeriodState.ThisWeek -> BucketScale.DAY
        is PeriodState.ThisMonth -> BucketScale.WEEK
        is PeriodState.CUSTOM -> deriveCustomScale(period.range)
    }

    private fun deriveCustomScale(range: DateRange): BucketScale {
        val from = range.from.date
        val to = range.to.date
        if (from >= to) return BucketScale.DAY
        val days = daysInclusive(from, to)
        val wholeMonths = isWholeMonths(range)
        return when {
            days <= 31 && !wholeMonths -> if (days % 7 == 0) BucketScale.WEEK else BucketScale.DAY
            days <= 365 && wholeMonths -> BucketScale.MONTH
            days <= 180 -> BucketScale.WEEK
            else -> BucketScale.MONTH
        }
    }

    // Placeholders if not already in your codebase:
    private fun daysInclusive(from: LocalDate, to: LocalDate): Int =
        (to.toEpochDays() - from.toEpochDays()) + 1

    private fun isWholeMonths(range: DateRange): Boolean {
        val from = range.from
        val to = range.to
        val fromIsFirst = from.date.dayOfMonth == 1 && from.time == LocalTime(0, 0)
        val lastOfToMonth = LocalDate(to.year, to.monthNumber, 1).plus(DatePeriod(months = 1))
            .minus(DatePeriod(days = 1))
        val toIsLast = to.date == lastOfToMonth && to.time >= LocalTime(23, 59, 59)
        return fromIsFirst && toIsLast
    }

    private operator fun DateRange.contains(ts: LocalDateTime): Boolean =
        (ts >= from) && (ts <= to)
}
