package com.grippo.training.recording.calculation

import com.grippo.design.components.chart.DSPieData
import com.grippo.design.components.chart.DSPieSlice
import com.grippo.design.components.chart.DSProgressData
import com.grippo.design.components.chart.DSProgressItem
import com.grippo.design.components.chart.DSRadarAxis
import com.grippo.design.components.chart.DSRadarData
import com.grippo.design.components.chart.DSRadarSeries
import com.grippo.design.resources.provider.providers.ColorProvider
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.state.exercise.examples.CategoryEnumState
import com.grippo.state.exercise.examples.ForceTypeEnumState
import com.grippo.state.trainings.ExerciseState

internal class MuscleAnalyticsCalculator(
    private val stringProvider: StringProvider,
    private val colorProvider: ColorProvider,
) {
    suspend fun calculateMuscleLoadDistribution(
        exercises: List<ExerciseState>,
    ): DSProgressData {
        val colors = colorProvider.get()
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

    suspend fun calculateMuscleGroupBalance(
        exercises: List<ExerciseState>,
    ): DSRadarData {
        val colors = colorProvider.get()
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

    suspend fun calculatePushPullBalance(
        exercises: List<ExerciseState>,
    ): DSPieData {
        val colors = colorProvider.get()
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