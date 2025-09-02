package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.components.chart.DSSparklinePoint
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.trainings.ExerciseState

internal class QualityAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {
    suspend fun calculateExecutionQuality(
        exercises: List<ExerciseState>,
    ): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        val items = exercises.take(5).mapIndexed { index, exercise ->
            // Рассчитываем консистентность веса между подходами
            val weights = exercise.iterations.mapNotNull { iteration ->
                val reps = iteration.repetitions.value ?: 0
                val volume = iteration.volume.value ?: 0f
                if (reps > 0) volume / reps else null
            }

            val consistency = if (weights.size > 1) {
                val avg = weights.average()
                val variance = weights.map { (it - avg) * (it - avg) }.average()
                val stdDev = kotlin.math.sqrt(variance).toFloat()
                val cv = if (avg > 0) (stdDev / avg.toFloat()) * 100f else 100f
                maxOf(0f, 100f - cv) // Меньше вариативность = выше качество
            } else 100f

            DSProgressItem(
                label = exercise.name.take(8),
                value = consistency,
                color = palette[index % palette.size]
            )
        }

        return DSProgressData(
            items = items,
            valueUnit = "%",
            title = "Execution Consistency"
        )
    }

    suspend fun calculateRepRangeDistribution(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()
        val repRanges = mutableMapOf<String, Int>()

        exercises.forEach { exercise ->
            exercise.iterations.forEach { iteration ->
                val reps = iteration.repetitions.value ?: 0
                val range = when {
                    reps <= 5 -> "Strength (1-5)"
                    reps <= 8 -> "Power (6-8)"
                    reps <= 12 -> "Hypertrophy (9-12)"
                    reps <= 20 -> "Endurance (13-20)"
                    else -> "High Endurance (20+)"
                }
                repRanges[range] = (repRanges[range] ?: 0) + 1
            }
        }

        val palette = colors.charts.categorical.palette1
        val slices = repRanges.entries.mapIndexed { index, (range, count) ->
            DSPieSlice(
                id = range,
                label = range,
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateTechniqueQuality(
        exercises: List<ExerciseState>,
    ): DSSparklineData {
        colorProvider.get()

        val points = exercises.mapIndexed { index, exercise ->
            // Анализируем консистентность повторений в подходах
            val repConsistency = exercise.iterations.map { it.repetitions.value ?: 0 }
                .let { reps ->
                    if (reps.size > 1) {
                        val avg = reps.average()
                        val variance = reps.map { (it - avg) * (it - avg) }.average()
                        val cv = if (avg > 0) kotlin.math.sqrt(variance) / avg else 1.0
                        (100 * (1 - cv)).toFloat().coerceIn(0f, 100f)
                    } else 100f
                }

            DSSparklinePoint(
                x = index.toFloat(),
                y = repConsistency
            )
        }

        return DSSparklineData(points = points)
    }

    suspend fun calculateTimeUnderTension(
        exercises: List<ExerciseState>,
    ): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        val items = exercises.take(6).mapIndexed { index, exercise ->
            // Примерное время: 3 сек на повторение
            val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
            val estimatedTUT = totalReps * 3 // секунды

            DSProgressItem(
                label = exercise.name.take(8),
                value = estimatedTUT.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSProgressData(
            items = items,
            valueUnit = "sec",
            title = "Time Under Tension"
        )
    }

    suspend fun calculateRPEAnalysis(
        exercises: List<ExerciseState>,
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette1

        val items = exercises.mapIndexed { index, exercise ->
            // Симулируем RPE на основе интенсивности и объема
            val totalVolume =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()
            val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
            val avgWeight = if (totalReps > 0) totalVolume / totalReps else 0f

            val simulatedRPE = when {
                avgWeight > 50f -> 8.5f + (avgWeight - 50f) / 20f // Тяжелые веса
                avgWeight > 30f -> 6f + (avgWeight - 30f) / 10f // Средние веса
                else -> 4f + avgWeight / 15f // Легкие веса
            }.coerceIn(1f, 10f)

            DSBarItem(
                label = exercise.name.take(8),
                value = simulatedRPE,
                color = when {
                    simulatedRPE < 4f -> palette[0] // Легко - зеленый
                    simulatedRPE < 7f -> palette[1] // Средне - желтый
                    else -> palette[2] // Тяжело - красный
                }
            )
        }

        return DSBarData(
            items = items,
            xName = "Exercise",
            yName = "RPE",
            yUnit = "/10"
        )
    }

    suspend fun calculateMovementPatterns(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()
        val patterns = mutableMapOf<String, Int>()

        exercises.forEach { exercise ->
            val pattern = when {
                exercise.name.lowercase().contains("squat") ||
                        exercise.name.lowercase().contains("lunge") -> "Knee Dominant"

                exercise.name.lowercase().contains("deadlift") ||
                        exercise.name.lowercase().contains("rdl") -> "Hip Dominant"

                exercise.name.lowercase().contains("press") ||
                        exercise.name.lowercase().contains("push") -> "Vertical Push"

                exercise.name.lowercase().contains("row") ||
                        exercise.name.lowercase().contains("pull") -> "Horizontal Pull"

                else -> "Other"
            }
            patterns[pattern] = (patterns[pattern] ?: 0) + 1
        }

        val palette = colors.charts.categorical.palette1
        val slices = patterns.entries.mapIndexed { index, (pattern, count) ->
            DSPieSlice(
                id = pattern,
                label = pattern,
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }
}