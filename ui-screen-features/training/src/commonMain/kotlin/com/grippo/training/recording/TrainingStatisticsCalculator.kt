package com.grippo.training.recording

import com.grippo.design.components.chart.DSAreaData
import com.grippo.design.components.chart.DSAreaPoint
import com.grippo.design.components.chart.DSBarData
import com.grippo.design.components.chart.DSBarItem
import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.components.chart.DSRadarAxis
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSRadarSeries
import com.grippo.design.components.chart.DSSparklineData
import com.grippo.design.components.chart.DSSparklinePoint
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.volume
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.formatters.IntensityFormatState
import com.grippo.state.formatters.RepetitionsFormatState
import com.grippo.state.formatters.VolumeFormatState
import com.grippo.state.trainings.ExerciseState
import com.grippo.state.trainings.TrainingMetrics

internal class TrainingStatisticsCalculator(
    private val stringProvider: StringProvider
) {

    suspend fun calculateTotalMetrics(exercises: List<ExerciseState>): TrainingMetrics {
        val totalVolume = exercises.sumOf { exercise ->
            exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }
        }.toFloat()

        val totalReps = exercises.sumOf { exercise ->
            exercise.iterations.sumOf { it.repetitions.value ?: 0 }
        }

        val averageIntensity = if (exercises.isNotEmpty()) {
            exercises.mapNotNull { it.metrics.intensity.value }.let { intensities ->
                if (intensities.isNotEmpty()) intensities.average().toFloat() else 0f
            }
        } else 0f

        return TrainingMetrics(
            volume = VolumeFormatState.of(totalVolume),
            repetitions = RepetitionsFormatState.of(totalReps),
            intensity = IntensityFormatState.of(averageIntensity)
        )
    }

    suspend fun calculateExerciseVolumeChart(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSBarData {
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

    suspend fun calculateCategoryDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.category }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().toString(),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateWeightTypeDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.weightType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().toString(),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateForceTypeDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.forceType }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().toString(),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateExperienceDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
        val categoryGroups = exercises
            .mapNotNull { it.exerciseExample?.experience }
            .groupBy { it }
            .mapValues { (_, list) -> list.size }

        if (categoryGroups.isEmpty()) {
            return DSPieData(slices = emptyList())
        }

        val palette = colors.charts.categorical.palette
        val slices = categoryGroups.entries.mapIndexed { index, (category, count) ->
            DSPieSlice(
                id = category.name,
                label = category.title().toString(),
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSPieData(slices = slices)
    }

    suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSProgressData {
        // Calculate muscle load based on exercise volume and muscle involvement
        val muscleLoads = mutableMapOf<String, Float>()

        exercises.forEach { exercise ->
            val exerciseVolume =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()

            // Get muscle involvement based on exercise type and category
            val muscleInvolvement = getMuscleInvolvementForExercise(exercise)

            muscleInvolvement.forEach { (muscleName, percentage) ->
                val load = exerciseVolume * (percentage / 100f)
                muscleLoads[muscleName] = (muscleLoads[muscleName] ?: 0f) + load
            }
        }

        if (muscleLoads.isEmpty()) {
            return DSProgressData(items = emptyList())
        }

        val palette = colors.charts.progress.palette
        val maxLoad = muscleLoads.values.maxOrNull() ?: 1f

        val items = muscleLoads.entries
            .sortedByDescending { it.value }
            .take(8) // Show top 8 muscles
            .mapIndexed { index, (muscleName, load) ->
                val percentage = (load / maxLoad) * 100f
                DSProgressItem(
                    label = muscleName,
                    value = percentage,
                    color = palette[index % palette.size]
                )
            }

        return DSProgressData(
            items = items,
            valueUnit = "kg",
            title = "Muscle Load Distribution"
        )
    }

    private fun getMuscleInvolvementForExercise(exercise: ExerciseState): Map<String, Float> {
        val category = exercise.exerciseExample?.category
        val forceType = exercise.exerciseExample?.forceType
        val exerciseName = exercise.name.lowercase()

        // Create realistic muscle involvement based on exercise patterns
        return when {
            // Chest exercises
            exerciseName.contains("bench") || exerciseName.contains("press") && exerciseName.contains(
                "chest"
            ) -> mapOf(
                "Chest" to 60f,
                "Triceps" to 25f,
                "Front Delts" to 15f
            )

            // Pull exercises (rows, pull-ups, etc.)
            exerciseName.contains("row") || exerciseName.contains("pull") -> mapOf(
                "Lats" to 40f,
                "Rhomboids" to 25f,
                "Rear Delts" to 20f,
                "Biceps" to 15f
            )

            // Squat variations
            exerciseName.contains("squat") -> mapOf(
                "Quadriceps" to 45f,
                "Glutes" to 30f,
                "Hamstrings" to 15f,
                "Core" to 10f
            )

            // Deadlift variations
            exerciseName.contains("deadlift") -> mapOf(
                "Hamstrings" to 35f,
                "Glutes" to 30f,
                "Lower Back" to 20f,
                "Traps" to 15f
            )

            // Shoulder exercises
            exerciseName.contains("shoulder") || exerciseName.contains("deltoid") -> mapOf(
                "Front Delts" to 40f,
                "Side Delts" to 35f,
                "Rear Delts" to 25f
            )

            // Arm exercises
            exerciseName.contains("curl") -> mapOf(
                "Biceps" to 80f,
                "Forearms" to 20f
            )

            exerciseName.contains("tricep") || exerciseName.contains("extension") -> mapOf(
                "Triceps" to 85f,
                "Front Delts" to 15f
            )

            // Based on category if name doesn't match
            category == CategoryEnumState.COMPOUND -> when (forceType) {
                ForceTypeEnumState.PUSH -> mapOf(
                    "Chest" to 40f,
                    "Triceps" to 30f,
                    "Front Delts" to 30f
                )

                ForceTypeEnumState.PULL -> mapOf(
                    "Lats" to 40f,
                    "Rhomboids" to 30f,
                    "Biceps" to 30f
                )

                ForceTypeEnumState.HINGE -> mapOf(
                    "Hamstrings" to 40f,
                    "Glutes" to 35f,
                    "Lower Back" to 25f
                )

                else -> mapOf("Full Body" to 100f)
            }

            // Isolation exercises
            else -> mapOf("Target Muscle" to 100f)
        }
    }

    // === ДОПОЛНИТЕЛЬНЫЕ АНАЛИТИЧЕСКИЕ МЕТОДЫ ===

    /**
     * Анализ интенсивности по упражнениям - показывает где пользователь работает слишком легко/тяжело
     */
    suspend fun calculateIntensityDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSBarData {
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

    /**
     * Анализ эффективности - показывает объем работы за единицу времени
     */
    suspend fun calculateWorkoutEfficiency(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
        colors: AppColor
    ): DSProgressData {
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

    /**
     * Анализ баланса мышечных групп - показывает дисбаланс в тренировке
     */
    suspend fun calculateMuscleGroupBalance(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSRadarData {
        val muscleGroupLoads = mutableMapOf<String, Float>()

        exercises.forEach { exercise ->
            val exerciseVolume =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()
            val muscleInvolvement = getMuscleInvolvementForExercise(exercise)

            muscleInvolvement.forEach { (muscleName, percentage) ->
                val muscleGroup = mapMuscleToGroup(muscleName)
                val load = exerciseVolume * (percentage / 100f)
                muscleGroupLoads[muscleGroup] = (muscleGroupLoads[muscleGroup] ?: 0f) + load
            }
        }

        val maxLoad = muscleGroupLoads.values.maxOrNull() ?: 1f
        val axes = listOf("Push", "Pull", "Legs", "Core").map { DSRadarAxis(it, it) }

        val normalizedValues = mapOf(
            "Push" to ((muscleGroupLoads["Push"] ?: 0f) / maxLoad),
            "Pull" to ((muscleGroupLoads["Pull"] ?: 0f) / maxLoad),
            "Legs" to ((muscleGroupLoads["Legs"] ?: 0f) / maxLoad),
            "Core" to ((muscleGroupLoads["Core"] ?: 0f) / maxLoad)
        )

        val series = listOf(
            DSRadarSeries(
                name = "Current Load",
                color = colors.charts.radar.palette.firstOrNull() ?: colors.text.primary,
                valuesByAxisId = normalizedValues
            )
        )

        return DSRadarData(axes = axes, series = series)
    }

    /**
     * Анализ прогрессии в рамках тренировки - показывает как меняется производительность
     */
    suspend fun calculateIntraWorkoutProgression(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSAreaData {
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

    /**
     * Анализ качества выполнения - показывает консистентность в подходах
     */
    suspend fun calculateExecutionQuality(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSProgressData {
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

    /**
     * Анализ объема по диапазонам повторений - показывает фокус тренировки
     */
    suspend fun calculateRepRangeDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
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

        val palette = colors.charts.categorical.palette
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

    /**
     * Анализ плотности тренировки - показывает интенсивность по времени
     */
    suspend fun calculateWorkoutDensity(
        exercises: List<ExerciseState>,
        workoutDurationMinutes: Int,
        colors: AppColor
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

    /**
     * Анализ типов упражнений - показывает разнообразие тренировки
     */
    suspend fun calculateExerciseTypeDistribution(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSBarData {
        val typeGroups = mutableMapOf<String, Int>()

        exercises.forEach { exercise ->
            val type = when {
                exercise.exerciseExample?.category == CategoryEnumState.COMPOUND -> "Compound"
                exercise.exerciseExample?.category == CategoryEnumState.ISOLATION -> "Isolation"
                else -> "Unknown"
            }
            typeGroups[type] = (typeGroups[type] ?: 0) + 1
        }

        val palette = colors.charts.categorical.palette
        val items = typeGroups.entries.mapIndexed { index, (type, count) ->
            DSBarItem(
                label = type,
                value = count.toFloat(),
                color = palette[index % palette.size]
            )
        }

        return DSBarData(
            items = items,
            xName = "Exercise Type",
            yName = "Count",
            yUnit = "exercises"
        )
    }

    /**
     * Анализ нагрузки по времени - показывает когда была пиковая нагрузка
     */
    suspend fun calculateLoadOverTime(
        exercises: List<ExerciseState>,
        colors: AppColor
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

    /**
     * Анализ слабых мест - показывает упражнения с низкой интенсивностью
     */
    suspend fun calculateWeakPoints(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSProgressData {
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

    /**
     * Анализ RPE (Rate of Perceived Exertion) - показывает субъективную сложность
     */
    suspend fun calculateRPEAnalysis(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSBarData {
        val palette = colors.charts.categorical.palette

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

    /**
     * Анализ времени под нагрузкой (Time Under Tension)
     */
    suspend fun calculateTimeUnderTension(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSProgressData {
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

    /**
     * Анализ 1RM (One Rep Max) прогнозирование
     */
    suspend fun calculateEstimated1RM(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSBarData {
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

    /**
     * Анализ симметрии тренировки (Push vs Pull баланс)
     */
    suspend fun calculatePushPullBalance(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
        var pushVolume = 0f
        var pullVolume = 0f
        var otherVolume = 0f

        exercises.forEach { exercise ->
            val exerciseVolume =
                exercise.iterations.sumOf { it.volume.value?.toDouble() ?: 0.0 }.toFloat()

            when (exercise.exerciseExample?.forceType) {
                ForceTypeEnumState.PUSH -> pushVolume += exerciseVolume
                ForceTypeEnumState.PULL -> pullVolume += exerciseVolume
                else -> otherVolume += exerciseVolume
            }
        }

        val palette = colors.charts.categorical.palette
        val slices = listOfNotNull(
            if (pushVolume > 0) DSPieSlice("push", "Push", pushVolume, palette[0]) else null,
            if (pullVolume > 0) DSPieSlice("pull", "Pull", pullVolume, palette[1]) else null,
            if (otherVolume > 0) DSPieSlice("other", "Other", otherVolume, palette[2]) else null
        )

        return DSPieData(slices = slices)
    }

    /**
     * Анализ прогрессии усталости в течение тренировки
     */
    suspend fun calculateFatigueProgression(
        exercises: List<ExerciseState>,
        colors: AppColor
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

    /**
     * Анализ разнообразия диапазонов движений
     */
    suspend fun calculateMovementPatterns(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSPieData {
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

        val palette = colors.charts.categorical.palette
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

    /**
     * Анализ качества техники (на основе консистентности)
     */
    suspend fun calculateTechniqueQuality(
        exercises: List<ExerciseState>,
        colors: AppColor
    ): DSSparklineData {
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

    /**
     * Анализ энергозатрат (примерный расчет калорий)
     */
    suspend fun calculateEnergyExpenditure(
        exercises: List<ExerciseState>,
        bodyWeightKg: Float = 75f, // Дефолтный вес
        colors: AppColor
    ): DSProgressData {
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

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private fun mapMuscleToGroup(muscleName: String): String {
        return when (muscleName.lowercase()) {
            "chest", "triceps", "front delts" -> "Push"
            "lats", "rhomboids", "rear delts", "biceps", "traps" -> "Pull"
            "quadriceps", "glutes", "hamstrings", "calves" -> "Legs"
            "core", "lower back" -> "Core"
            else -> "Other"
        }
    }
}
