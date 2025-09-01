package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.volume

internal class ExerciseAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider
) {
    suspend fun calculateExerciseVolumeChart(
        exercises: List<com.grippo.state.trainings.ExerciseState>,
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            val volume = exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()
            DSBarItem(
                label = exercise.name.take(10), // Truncate long names
                value = volume,
                color = palette[index % palette.size]
            )
        }

        val yName = stringProvider.get(Res.string.volume)

        return DSBarData(
            items = items,
            xName = "Exercise",
            yName = yName,
            yUnit = "kg"
        )
    }

    suspend fun calculateIntensityDistribution(
        exercises: List<com.grippo.state.trainings.ExerciseState>,
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            val avgIntensity = exercise.iterations.mapNotNull { it.volume.value }
                .let { volumes ->
                    if (volumes.isNotEmpty()) {
                        // Примерная формула интенсивности: объем / повторения
                        val totalVolume = volumes.sum()
                        val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
                        if (totalReps > 0) totalVolume / totalReps else 0f
                    } else 0f
                }

            DSBarItem(
                label = exercise.name.take(8),
                value = avgIntensity,
                color = when {
                    avgIntensity < 20f -> palette[0] // Легкая интенсивность - зеленый
                    avgIntensity < 40f -> palette[1] // Средняя - желтый
                    else -> palette[2] // Высокая - красный
                }
            )
        }

        return DSBarData(
            items = items,
            xName = "Exercise",
            yName = "Avg Weight",
            yUnit = "kg"
        )
    }

    suspend fun calculateIntraWorkoutProgression(
        exercises: List<com.grippo.state.trainings.ExerciseState>,
    ): DSAreaData {
        colorProvider.get()
        val points = exercises.mapIndexed { index, exercise ->
            val avgWeight = exercise.iterations.mapNotNull { it.volume.value }
                .let { volumes ->
                    val totalReps = exercise.iterations.sumOf { it.repetitions.value ?: 0 }
                    if (volumes.isNotEmpty() && totalReps > 0) {
                        volumes.sum() / totalReps
                    } else 0f
                }

            DSAreaPoint(
                x = index.toFloat(),
                y = avgWeight,
                xLabel = "Ex${index + 1}"
            )
        }

        return DSAreaData(points = points)
    }

    suspend fun calculateWeakPoints(
        exercises: List<com.grippo.state.trainings.ExerciseState>,
    ): DSProgressData {
        val colors = colorProvider.get()
        val palette = colors.charts.progress.palette

        val items = exercises.map { exercise ->
            val avgIntensity = exercise.iterations.mapNotNull { iteration ->
                val reps = iteration.repetitions.value ?: 0
                val volume = iteration.volume.value ?: 0f
                if (reps > 0) volume / reps else null
            }.let { weights ->
                if (weights.isNotEmpty()) weights.average().toFloat() else 0f
            }

            exercise to avgIntensity
        }
            .sortedBy { it.second } // Сортируем по возрастанию интенсивности
            .take(5) // Берем 5 самых слабых
            .mapIndexed { index, (exercise, intensity) ->
                DSProgressItem(
                    label = exercise.name.take(10),
                    value = intensity * 2, // x2 для визуализации
                    color = palette[index % palette.size]
                )
            }

        return DSProgressData(
            items = items,
            valueUnit = "kg",
            title = "Potential Weak Points"
        )
    }

    suspend fun calculateEstimated1RM(
        exercises: List<com.grippo.state.trainings.ExerciseState>,
    ): DSBarData {
        val colors = colorProvider.get()
        val palette = colors.charts.categorical.palette

        val items = exercises.mapIndexed { index, exercise ->
            // Используем формулу Brzycki: 1RM = weight / (1.0278 - 0.0278 × reps)
            val estimated1RM = exercise.iterations.mapNotNull { iteration ->
                val weight = iteration.volume.value ?: 0f
                val reps = iteration.repetitions.value ?: 0
                if (weight > 0 && reps > 0 && reps <= 12) {
                    (weight / reps) / (1.0278 - 0.0278 * reps)
                } else null
            }.maxOrNull()?.toFloat() ?: 0f

            DSBarItem(
                label = exercise.name.take(8),
                value = estimated1RM,
                color = palette[index % palette.size]
            )
        }

        return DSBarData(
            items = items,
            xName = "Exercise",
            yName = "Est. 1RM",
            yUnit = "kg"
        )
    }
}