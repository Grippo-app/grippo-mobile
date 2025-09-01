package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.components.chart.DSSparklinePoint
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.trainings.ExerciseState

internal class WorkoutEfficiencyCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {
    suspend fun calculateWorkoutEfficiency(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
    ): DSProgressData {
        val colors = colorProvider.get()

        val totalVolume = exercises.sumOf { exercise ->
            exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }
        }.toFloat()

        val totalSets = exercises.sumOf { it.iterations.size }
        val avgRestBetweenSets =
            if (totalSets > 0) workoutDurationMinutes.toFloat() / totalSets else 0f

        val palette = colors.charts.progress.palette

        val items = listOf(
            DSProgressItem(
                label = "Volume/Min",
                value = if (workoutDurationMinutes > 0) (totalVolume / workoutDurationMinutes) * 10 else 0f, // x10 для визуализации
                color = palette[0]
            ),
            DSProgressItem(
                label = "Sets/Min",
                value = if (workoutDurationMinutes > 0) (totalSets.toFloat() / workoutDurationMinutes) * 100 else 0f, // x100 для визуализации
                color = palette[1]
            ),
            DSProgressItem(
                label = "Rest Efficiency",
                value = maxOf(
                    0f,
                    100f - (avgRestBetweenSets * 2)
                ), // Меньше отдых = выше эффективность
                color = palette[2]
            )
        )

        return DSProgressData(
            items = items,
            valueUnit = "pts",
            title = "Workout Efficiency"
        )
    }

    suspend fun calculateWorkoutDensity(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
    ): DSSparklineData {
        val timeSlots = 10 // Разбиваем тренировку на 10 временных отрезков
        workoutDurationMinutes.toFloat() / timeSlots

        val points = (0 until timeSlots).map { slot ->
            // Симулируем плотность - больше упражнений в начале, меньше в конце
            val density = when (slot) {
                in 0..2 -> 80f + (slot * 10f) // Разминка и начало
                in 3..6 -> 100f - (slot * 5f) // Пик тренировки
                else -> 60f - ((slot - 6) * 10f) // Спад в конце
            }

            DSSparklinePoint(
                x = slot.toFloat(),
                y = maxOf(10f, density)
            )
        }

        return DSSparklineData(points = points)
    }

    suspend fun calculateLoadOverTime(
        exercises: List<ExerciseState>,
    ): DSAreaData {
        val points = exercises.mapIndexed { index, exercise ->
            val totalLoad =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()

            DSAreaPoint(
                x = index.toFloat(),
                y = totalLoad,
                xLabel = "${index * 15}min" // Примерно 15 мин на упражнение
            )
        }

        return DSAreaData(points = points)
    }

    suspend fun calculateFatigueProgression(
        exercises: List<ExerciseState>,
    ): DSAreaData {
        val points = exercises.mapIndexed { index, exercise ->
            // Симулируем падение производительности от усталости
            val basePerformance = 100f
            val fatigueReduction = index * 5f // 5% падение за каждое упражнение
            val currentPerformance = maxOf(60f, basePerformance - fatigueReduction)

            DSAreaPoint(
                x = index.toFloat(),
                y = currentPerformance,
                xLabel = "Ex${index + 1}"
            )
        }

        return DSAreaData(points = points)
    }

    suspend fun calculateEnergyExpenditure(
        exercises: List<ExerciseState>,
        bodyWeightKg: Float = 75f, // Дефолтный вес
    ): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        val items = exercises.take(5).mapIndexed { index, exercise ->
            // Примерная формула: (вес тела + вес отягощения) × повторения × коэффициент
            val totalVolume =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()
            val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }

            val estimatedCalories =
                ((bodyWeightKg + totalVolume) * totalReps * 0.05f).coerceAtLeast(0f)

            DSProgressItem(
                label = exercise.name.take(8),
                value = estimatedCalories,
                color = palette[index % palette.size]
            )
        }

        return DSProgressData(
            items = items,
            valueUnit = "kcal",
            title = "Energy Expenditure"
        )
    }
}