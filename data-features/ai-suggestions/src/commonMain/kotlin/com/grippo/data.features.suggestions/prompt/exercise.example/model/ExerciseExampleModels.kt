package com.grippo.data.features.suggestions.prompt.exercise.example.model

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleValue
import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal data class ExampleCatalog(
    val contexts: List<ExampleContext>,
    val byId: Map<String, ExampleContext>
)

internal data class ExampleContext(
    val id: String,
    val displayName: String,
    val muscles: List<MuscleShare>,
    val category: String,
    val forceType: String,
    val weightType: String,
    val experience: String,
    val usageCount: Int,
    val lastUsed: LocalDateTime?,
    val equipmentIds: Set<String>,
    val value: ExerciseExampleValue
) {
    fun primaryMuscleId(): String? = muscles.firstOrNull()?.id
}

internal data class MuscleShare(
    val id: String,
    val name: String,
    val percentage: Int,
    val recovery: Int? = null
)

internal data class PredictionSignals(
    val session: List<ExerciseSummary>,
    val stage: Int,
    val trainings: List<TrainingSummary>,
    val historicalToday: List<ExerciseSummary>,
    val performedExampleIds: Set<String>,
    val muscleLoads: List<MuscleLoad>,
    val categoryStats: CategoryStats,
    val muscleTargets: List<MuscleTarget>,
    val muscleDeficits: Map<String, Double>,
    val forceMix: Map<String, Int>,
    val weightMix: Map<String, Int>,
    val experienceMix: Map<String, Int>,
    val lastLoadByMuscleDateTime: Map<String, LocalDateTime>,
    val residualFatigueByMuscle: Map<String, Double>,
    val periodicHabits: Map<String, PeriodicHabit>,
    val sessionHabits: Map<String, SessionHabit>,
    val usedEquipmentIds: Set<String>
)

internal data class TrainingSummary(
    val performedAt: LocalDateTime,
    val dayOfWeek: DayOfWeek,
    val exercises: List<ExerciseSummary>,
    val totalVolume: Float,
    val totalRepetitions: Int,
    val avgIntensity: Float
)

internal data class ExerciseSummary(
    val exampleId: String,
    val displayName: String,
    val muscles: List<MuscleShare>,
    val category: String,
    val forceType: String,
    val weightType: String,
    val experience: String,
    val equipmentIds: Set<String>
)

internal data class MuscleLoad(
    val name: String,
    var total: Int,
    var sessions: Int
)

internal data class MuscleTarget(
    val id: String,
    val name: String,
    val average: Double,
    val current: Double
) {
    val deficit: Double get() = average - current
}

internal data class CategoryStats(
    val average: Map<String, Double>,
    val current: Map<String, Int>
) {
    fun deficit(category: String): Double {
        val avg = average[category] ?: 0.0
        val done = current[category] ?: 0
        return avg - done
    }

    fun priorityFor(category: String, stage: Int): Int {
        val compoundKey = CategoryEnum.COMPOUND.key
        val isolationKey = CategoryEnum.ISOLATION.key
        return when (category) {
            compoundKey -> when {
                stage < EARLY_STAGE_COMPOUND_LIMIT -> 0
                deficit(compoundKey) > SuggestionMath.DEFICIT_EPS -> 0
                else -> 1
            }

            isolationKey -> when {
                stage < EARLY_STAGE_COMPOUND_LIMIT -> 1
                deficit(isolationKey) > SuggestionMath.DEFICIT_EPS -> 0
                else -> 1
            }

            else -> 1
        }
    }

    private companion object {
        private const val EARLY_STAGE_COMPOUND_LIMIT = 2
    }
}

internal data class PeriodicHabit(
    val muscleId: String,
    val muscleName: String,
    val medianIntervalDays: Int,
    val confidence: Double,
    val lastDate: LocalDate,
    val nextDue: LocalDate
)

internal data class SessionHabit(
    val muscleId: String,
    val medianIntervalSessions: Int,
    val lastSeenIdx: Int,
    val nextDueIdx: Int
)
