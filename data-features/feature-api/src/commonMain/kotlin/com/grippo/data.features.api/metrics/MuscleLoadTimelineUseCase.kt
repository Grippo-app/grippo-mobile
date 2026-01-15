package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.metrics.models.MuscleLoadTimeline
import com.grippo.data.features.api.metrics.models.MuscleLoadTimelineBucket
import com.grippo.data.features.api.metrics.models.MuscleLoadTimelineMetric
import com.grippo.data.features.api.metrics.models.MuscleLoadTimelineNormalization
import com.grippo.data.features.api.metrics.models.MuscleLoadTimelineRow
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroup
import com.grippo.data.features.api.training.models.Iteration
import com.grippo.data.features.api.training.models.Training
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRange
import com.grippo.toolkit.date.utils.DateTimeUtils
import com.grippo.toolkit.date.utils.contains
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

public class MuscleLoadTimelineUseCase(
    private val exerciseExampleFeature: ExerciseExampleFeature,
    private val muscleFeature: MuscleFeature,
) {

    private companion object {
        private const val EPS = 1e-3f
        private const val WEEK_PREFIX = "W"
    }

    public suspend fun fromTrainings(
        trainings: List<Training>,
        range: DateRange,
        metric: MuscleLoadTimelineMetric = MuscleLoadTimelineMetric.Repetitions,
        normalization: MuscleLoadTimelineNormalization = MuscleLoadTimelineNormalization.PerColumn,
    ): MuscleLoadTimeline? {
        val inRange = trainings.filter { it.createdAt in range }
        if (inRange.isEmpty()) return null

        val scale = deriveScale(range)
        val buckets = buildBuckets(range, scale).sortedBy { it.start }
        val cols = buckets.size
        if (cols == 0) return null

        val exampleIds = inRange
            .flatMap { training -> training.exercises.map { it.exerciseExample.id } }
            .toSet()
        if (exampleIds.isEmpty()) return null

        val exampleMap = loadExamples(exampleIds)
        if (exampleMap.isEmpty()) return null

        val groups = loadMuscleGroups()
        val rowSpec = buildRowSpec(groups, exampleMap.values.toList())
        val rows = rowSpec.rows.size
        if (rows == 0) return null

        val trainingsPerBucket = distributeTrainings(inRange, buckets)
        val raw = FloatArray(rows * cols)
        val sessionsPerBucket = IntArray(cols)

        for (c in buckets.indices) {
            val bucketTrainings = trainingsPerBucket[c]
            sessionsPerBucket[c] = bucketTrainings.size
            if (bucketTrainings.isEmpty()) continue

            val perMuscle = computeBucketMuscleMeasure(bucketTrainings, exampleMap, metric)
            if (perMuscle.isEmpty()) continue

            rowSpec.rows.forEachIndexed { r, row ->
                val value = row.muscles.fold(0f) { acc, muscle ->
                    val muscleValue = perMuscle[muscle] ?: 0f
                    acc + muscleValue
                }
                raw[r * cols + c] = value
            }
        }

        val maxValue = raw.filter { it.isFinite() }.maxOrNull() ?: 0f
        if (maxValue <= EPS) return null

        val values = raw.toList()
        val values01 = normalize(raw, rows, cols, normalization).toList()
        val labels = buildBucketLabels(buckets, scale)

        val timelineRows = rowSpec.rows.map { row ->
            MuscleLoadTimelineRow(
                id = row.id,
                label = row.label,
                muscles = row.muscles,
            )
        }

        val timelineBuckets = buckets.mapIndexed { index, bucket ->
            MuscleLoadTimelineBucket(
                label = labels[index],
                start = bucket.start,
                end = bucket.end,
                sessionsCount = sessionsPerBucket[index],
            )
        }

        return MuscleLoadTimeline(
            rows = timelineRows,
            buckets = timelineBuckets,
            values = values,
            values01 = values01,
            normalization = normalization,
        )
    }

    private suspend fun loadExamples(ids: Set<String>): Map<String, ExerciseExample> {
        if (ids.isEmpty()) return emptyMap()
        return exerciseExampleFeature.observeExerciseExamples(ids.toList())
            .first()
            .associateBy { it.value.id }
    }

    private suspend fun loadMuscleGroups(): List<MuscleGroup> {
        return muscleFeature.observeMuscles().first()
    }

    private fun distributeTrainings(
        trainings: List<Training>,
        buckets: List<TimeBucket>,
    ): Array<MutableList<Training>> {
        val cols = buckets.size
        val result = Array(cols) { mutableListOf<Training>() }
        if (trainings.isEmpty() || buckets.isEmpty()) return result

        val sorted = trainings.sortedBy { it.createdAt }
        var tIdx = 0

        buckets.forEachIndexed { index, bucket ->
            while (tIdx < sorted.size && sorted[tIdx].createdAt < bucket.start) {
                tIdx++
            }
            var cursor = tIdx
            while (cursor < sorted.size && sorted[cursor].createdAt < bucket.end) {
                result[index] += sorted[cursor]
                cursor++
            }
            tIdx = cursor
            if (tIdx >= sorted.size) return result
        }

        return result
    }

    private fun computeBucketMuscleMeasure(
        trainings: List<Training>,
        exampleMap: Map<String, ExerciseExample>,
        metric: MuscleLoadTimelineMetric,
    ): Map<MuscleEnum, Float> {
        val result = HashMap<MuscleEnum, Float>()

        trainings.forEach { training ->
            training.exercises.forEach { exercise ->
                val exampleId = exercise.exerciseExample.id
                val example = exampleMap[exampleId] ?: return@forEach

                val base = when (metric) {
                    MuscleLoadTimelineMetric.Volume -> exercise.iterations.fold(0f) { acc, iteration ->
                        acc + tonnage(iteration)
                    }

                    MuscleLoadTimelineMetric.Repetitions -> exercise.iterations.fold(0) { acc, iteration ->
                        acc + iteration.repetitions.coerceAtLeast(0)
                    }.toFloat()

                    MuscleLoadTimelineMetric.Sets -> exercise.iterations.count { it.repetitions > 0 }
                        .toFloat()
                }

                if (base <= EPS) return@forEach

                val totalShare = example.bundles.fold(0f) { acc, bundle ->
                    acc + bundle.percentage.coerceAtLeast(0).toFloat()
                }
                if (totalShare <= EPS) return@forEach

                example.bundles.forEach { bundle ->
                    val share = bundle.percentage.coerceAtLeast(0)
                    if (share == 0) return@forEach
                    val ratio = share / totalShare
                    val muscle = bundle.muscle.type
                    val currentValue = result[muscle] ?: 0f
                    result[muscle] = currentValue + base * ratio
                }
            }
        }

        return result
    }

    private fun tonnage(iteration: Iteration): Float {
        val weight = iteration.volume
        val reps = iteration.repetitions
        if (weight <= EPS || reps <= 0) return 0f
        return weight * reps.toFloat()
    }

    private sealed interface Row {
        val id: String
        val label: String
        val muscles: List<MuscleEnum>

        data class Group(
            override val id: String,
            override val label: String,
            override val muscles: List<MuscleEnum>,
        ) : Row

        data class Single(
            override val id: String,
            override val label: String,
            override val muscles: List<MuscleEnum>,
        ) : Row
    }

    private data class RowSpec(
        val rows: List<Row>,
    )

    private fun buildRowSpec(
        groups: List<MuscleGroup>,
        examples: List<ExerciseExample>,
    ): RowSpec {
        if (groups.isNotEmpty()) {
            val rows = groups.map { group ->
                Row.Group(
                    id = group.id,
                    label = group.name,
                    muscles = group.muscles.map { it.type },
                )
            }
            return RowSpec(rows = rows)
        }

        val muscles = buildMuscleList(examples)
        val rows = muscles.map { (type, label) ->
            val resolvedName = label.ifBlank { formatMuscleName(type) }
            Row.Single(
                id = type.name,
                label = resolvedName,
                muscles = listOf(type),
            )
        }
        return RowSpec(rows = rows)
    }

    private fun buildMuscleList(examples: List<ExerciseExample>): List<Pair<MuscleEnum, String>> {
        val ordered = linkedMapOf<MuscleEnum, String>()
        examples.forEach { example ->
            example.bundles.forEach { bundle ->
                val type = bundle.muscle.type
                if (type !in ordered) {
                    ordered[type] = bundle.muscle.name
                }
            }
        }
        return ordered.entries.map { it.key to it.value }
    }

    private fun formatMuscleName(type: MuscleEnum): String {
        val parts = type.name.lowercase().replace('_', ' ').split(' ')
        return parts.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> ch.uppercaseChar() }
        }
    }

    private fun normalize(
        values: FloatArray,
        rows: Int,
        cols: Int,
        normalization: MuscleLoadTimelineNormalization,
    ): FloatArray {
        if (rows <= 0 || cols <= 0) return FloatArray(values.size)

        fun safe01(v: Float): Float = if (!v.isFinite()) 0f else v.coerceIn(0f, 1f)

        return when (normalization) {
            MuscleLoadTimelineNormalization.Absolute -> {
                val maxValue = values.filter { it.isFinite() }.maxOrNull() ?: 0f
                if (maxValue <= EPS) return FloatArray(values.size)
                FloatArray(values.size) { idx -> safe01(values[idx] / maxValue) }
            }

            MuscleLoadTimelineNormalization.PerRow -> {
                val result = FloatArray(values.size)
                for (r in 0 until rows) {
                    var rowMax = 0f
                    for (c in 0 until cols) {
                        val v = values[r * cols + c]
                        if (v.isFinite() && v > rowMax) rowMax = v
                    }
                    for (c in 0 until cols) {
                        val v = values[r * cols + c]
                        result[r * cols + c] = if (rowMax <= EPS) 0f else safe01(v / rowMax)
                    }
                }
                result
            }

            MuscleLoadTimelineNormalization.PerColumn -> {
                val result = FloatArray(values.size)
                for (c in 0 until cols) {
                    var colMax = 0f
                    for (r in 0 until rows) {
                        val v = values[r * cols + c]
                        if (v.isFinite() && v > colMax) colMax = v
                    }
                    for (r in 0 until rows) {
                        val v = values[r * cols + c]
                        result[r * cols + c] = if (colMax <= EPS) 0f else safe01(v / colMax)
                    }
                }
                result
            }
        }
    }

    private enum class BucketScale {
        EXERCISE,
        DAY,
        WEEK,
        MONTH,
    }

    private data class TimeBucket(
        val start: LocalDateTime,
        val end: LocalDateTime,
    )

    private fun deriveScale(range: DateRange): BucketScale = when (range.range()) {
        is DateRange.Range.Daily -> BucketScale.EXERCISE
        is DateRange.Range.Weekly -> BucketScale.DAY
        is DateRange.Range.Monthly -> BucketScale.WEEK
        is DateRange.Range.Yearly -> BucketScale.MONTH
        else -> deriveCustomScale(range)
    }

    private fun deriveCustomScale(range: DateRange): BucketScale {
        if (range.from > range.to) return BucketScale.DAY

        val fromDate = range.from.date
        val toDate = range.to.date
        if (fromDate == toDate) return BucketScale.EXERCISE

        val days = (toDate.toEpochDays() - fromDate.toEpochDays() + 1).toInt()
        val weeks = (days + DayOfWeek.entries.size - 1) / DayOfWeek.entries.size

        return when {
            days <= 24 -> BucketScale.DAY
            weeks <= 24 -> BucketScale.WEEK
            else -> BucketScale.MONTH
        }
    }

    private fun buildBuckets(range: DateRange, scale: BucketScale): List<TimeBucket> =
        when (scale) {
            BucketScale.EXERCISE -> emptyList()
            BucketScale.DAY -> buildDayBuckets(range)
            BucketScale.WEEK -> buildWeekBuckets(range)
            BucketScale.MONTH -> buildMonthBuckets(range)
        }

    private fun buildDayBuckets(range: DateRange): List<TimeBucket> {
        val buckets = mutableListOf<TimeBucket>()
        var cursor = range.from.date

        while (cursor <= range.to.date) {
            val dayStart = LocalDateTime(cursor, LocalTime(0, 0))
            val dayEnd = LocalDateTime(cursor, LocalTime(23, 59, 59, 999_000_000))
            val start = maxDT(dayStart, range.from)
            val end = minDT(dayEnd, range.to)
            buckets += TimeBucket(start = start, end = end)
            cursor = cursor.plus(DatePeriod(days = 1))
        }

        return buckets
    }

    private fun buildWeekBuckets(range: DateRange): List<TimeBucket> {
        val buckets = mutableListOf<TimeBucket>()
        val fromDate = range.from.date
        val weekStartDate = fromDate.minus(DatePeriod(days = fromDate.dayOfWeek.ordinal))
        var weekStart = DateTimeUtils.startOfDay(weekStartDate)

        while (weekStart <= range.to) {
            val weekEndDate = weekStart.date.plus(DatePeriod(days = 6))
            val weekEnd = LocalDateTime(weekEndDate, LocalTime(23, 59, 59, 999_000_000))
            val start = maxDT(weekStart, range.from)
            val end = minDT(weekEnd, range.to)
            buckets += TimeBucket(start = start, end = end)
            val nextWeekDate = weekStart.date.plus(DatePeriod(days = 7))
            weekStart = DateTimeUtils.startOfDay(nextWeekDate)
        }

        return buckets
    }

    private fun buildMonthBuckets(range: DateRange): List<TimeBucket> {
        val buckets = mutableListOf<TimeBucket>()
        var monthStart = DateTimeUtils.startOfDay(
            LocalDate(range.from.year, range.from.month.number, 1)
        )

        while (monthStart <= range.to) {
            val firstOfNext = LocalDate(monthStart.year, monthStart.month.number, 1)
                .plus(DatePeriod(months = 1))
            val lastDay = firstOfNext.minus(DatePeriod(days = 1))
            val monthEnd = LocalDateTime(lastDay, LocalTime(23, 59, 59, 999_000_000))
            val start = maxDT(monthStart, range.from)
            val end = minDT(monthEnd, range.to)
            buckets += TimeBucket(start = start, end = end)
            val nextMonthDate = monthStart.date.plus(DatePeriod(months = 1))
            monthStart = DateTimeUtils.startOfDay(nextMonthDate)
        }

        return buckets
    }

    private fun buildBucketLabels(
        buckets: List<TimeBucket>,
        scale: BucketScale,
    ): List<String> = when (scale) {
        BucketScale.DAY -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DateOnly.DateDdMmm)
        }

        BucketScale.WEEK -> buckets.map {
            val date = it.start.date
            val firstJan = LocalDate(date.year, 1, 1)
            val dayOfYear = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
            val week = ((dayOfYear - 1) / DayOfWeek.entries.size) + 1
            val month = DateTimeUtils.format(it.start, DateFormat.DateOnly.MonthShort)
            "$WEEK_PREFIX$week-$month"
        }

        BucketScale.MONTH -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DateOnly.MonthShort)
        }

        BucketScale.EXERCISE -> emptyList()
    }

    private fun minDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a <= b) a else b
    private fun maxDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a >= b) a else b
}
