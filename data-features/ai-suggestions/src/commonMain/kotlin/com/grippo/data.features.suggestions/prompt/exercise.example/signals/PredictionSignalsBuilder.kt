package com.grippo.data.features.suggestions.prompt.exercise.example.signals

import com.grippo.data.features.suggestions.prompt.exercise.example.config.HabitCycleConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import com.grippo.data.features.suggestions.prompt.exercise.example.model.CategoryStats
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleCatalog
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExerciseSummary
import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleLoad
import com.grippo.data.features.suggestions.prompt.exercise.example.model.MuscleTarget
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PeriodicHabit
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PredictionSignals
import com.grippo.data.features.suggestions.prompt.exercise.example.model.SessionHabit
import com.grippo.data.features.suggestions.prompt.exercise.example.model.TrainingSummary
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.PromptMath
import com.grippo.database.dao.DraftTrainingDao
import com.grippo.database.dao.TrainingDao
import com.grippo.database.models.DraftTrainingPack
import com.grippo.database.models.TrainingPack
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.koin.core.annotation.Single
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days

/**
 * Aggregates draft session data and historical trainings into the structured
 * signals consumed by the prompt composer and candidate selector.
 */
@Single
internal class PredictionSignalsBuilder(
    private val draftTrainingDao: DraftTrainingDao,
    private val trainingDao: TrainingDao
) {

    private companion object {
        private const val LOOKBACK_DAYS = 90
        private const val RECENT_TRAININGS_LIMIT = 16
        private const val MAX_MUSCLE_SUMMARY = 12
    }

    suspend fun build(now: LocalDateTime, catalog: ExampleCatalog): PredictionSignals {
        val sessionExercises = draftTrainingDao.get()
            .firstOrNull()
            ?.toSessionSummaries(catalog.byId)
            ?: emptyList()

        val lookbackFrom = DateTimeUtils.minus(now, LOOKBACK_DAYS.days)
        val rawTrainings = trainingDao.get(
            from = DateTimeUtils.toUtcIso(lookbackFrom),
            to = DateTimeUtils.toUtcIso(now)
        ).firstOrNull().orEmpty()

        val trainingSummaries = rawTrainings
            .sortedByDescending { it.training.createdAt }
            .take(RECENT_TRAININGS_LIMIT)
            .map { it.toSummary(catalog.byId) }
            .filter { it.exercises.isNotEmpty() }

        val todayHistory = trainingSummaries
            .filter { it.performedAt.date == now.date }
            .flatMap { it.exercises }
            .filterNot { sessionExercises.any { session -> session.exampleId == it.exampleId } }

        val performedIds = (sessionExercises + todayHistory)
            .map { it.exampleId }
            .toSet()

        val usedEquipmentIds: Set<String> =
            sessionExercises.flatMap { it.equipmentIds }.toSet()

        val categoryStats = computeCategoryStats(trainingSummaries, sessionExercises)
        val muscleTargets = computeMuscleTargetsPrimaryOnly(trainingSummaries, sessionExercises)

        val lastLoadByMuscleDateTime = computeLastLoadDateTimeByMuscle(trainingSummaries)
        val residualFatigueByMuscle = computeResidualFatigueByMuscle(now, trainingSummaries)
        val periodicHabits = computePeriodicHabits(trainingSummaries)
        val sessionHabits = computeSessionHabits(trainingSummaries)

        return PredictionSignals(
            session = sessionExercises,
            stage = sessionExercises.size,
            trainings = trainingSummaries,
            historicalToday = todayHistory,
            performedExampleIds = performedIds,
            muscleLoads = computeMuscleLoads(trainingSummaries, catalog.byId),
            categoryStats = categoryStats,
            muscleTargets = muscleTargets,
            muscleDeficits = muscleTargets.associate { it.id to it.deficit },
            forceMix = sessionExercises.groupingBy { it.forceType }.eachCount(),
            weightMix = sessionExercises.groupingBy { it.weightType }.eachCount(),
            experienceMix = sessionExercises.groupingBy { it.experience }.eachCount(),
            lastLoadByMuscleDateTime = lastLoadByMuscleDateTime,
            residualFatigueByMuscle = residualFatigueByMuscle,
            periodicHabits = periodicHabits,
            sessionHabits = sessionHabits,
            usedEquipmentIds = usedEquipmentIds
        )
    }

    private fun DraftTrainingPack.toSessionSummaries(
        exampleContextMap: Map<String, ExampleContext>
    ): List<ExerciseSummary> {
        return exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank() || context.displayName.isBlank() || context.muscles.isEmpty()) return@mapNotNull null

            val forceType = context.forceType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val weightType = context.weightType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val experience = context.experience.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val category = context.category.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            ExerciseSummary(
                exampleId = context.id,
                displayName = context.displayName,
                muscles = context.muscles,
                category = category,
                forceType = forceType,
                weightType = weightType,
                experience = experience,
                equipmentIds = context.equipmentIds
            )
        }
    }

    private fun TrainingPack.toSummary(
        exampleContextMap: Map<String, ExampleContext>
    ): TrainingSummary {
        val performedAt = DateTimeUtils.toLocalDateTime(training.createdAt)

        val exSummaries = exercises.mapNotNull { pack ->
            val context =
                exampleContextMap[pack.exercise.exerciseExampleId] ?: return@mapNotNull null
            if (context.id.isBlank() || context.displayName.isBlank() || context.muscles.isEmpty()) return@mapNotNull null

            val forceType = context.forceType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val weightType = context.weightType.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val experience = context.experience.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val category = context.category.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            ExerciseSummary(
                exampleId = context.id,
                displayName = context.displayName,
                muscles = context.muscles,
                category = category,
                forceType = forceType,
                weightType = weightType,
                experience = experience,
                equipmentIds = context.equipmentIds
            )
        }

        val volume = training.volume
        val reps = training.repetitions
        val intensityFromData = if (reps > 0) volume / reps.toFloat() else training.intensity

        return TrainingSummary(
            performedAt = performedAt,
            dayOfWeek = performedAt.date.dayOfWeek,
            exercises = exSummaries,
            totalVolume = volume,
            totalRepetitions = reps,
            avgIntensity = intensityFromData
        )
    }

    private fun computeCategoryStats(
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): CategoryStats {
        val history = trainings.flatMap { it.exercises }
        val average = history.groupingBy { it.category }.eachCount().mapValues { (_, count) ->
            if (trainings.isNotEmpty()) count.toDouble() / trainings.size else 0.0
        }
        val current = session.groupingBy { it.category }.eachCount()
        return CategoryStats(average = average, current = current)
    }

    private fun computeMuscleTargetsPrimaryOnly(
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): List<MuscleTarget> {
        fun primaryCount(exercises: List<ExerciseSummary>): Map<String, Double> {
            val map = mutableMapOf<String, Double>()
            exercises.forEach { ex ->
                val primary = ex.muscles.firstOrNull() ?: return@forEach
                map[primary.id] = (map[primary.id] ?: 0.0) + 1.0
            }
            return map
        }

        val totals = mutableMapOf<String, Double>()
        val occurrences = mutableMapOf<String, Int>()
        val names = mutableMapOf<String, String>()

        trainings.forEach { training ->
            if (training.exercises.isEmpty()) return@forEach
            val w = primaryCount(training.exercises)
            if (w.isEmpty()) return@forEach

            w.forEach { (id, value) -> totals[id] = (totals[id] ?: 0.0) + value }
            w.keys.forEach { id -> occurrences[id] = (occurrences[id] ?: 0) + 1 }
            training.exercises.mapNotNull { it.muscles.firstOrNull() }.forEach { share ->
                if (!names.containsKey(share.id)) names[share.id] = share.name
            }
        }

        session.mapNotNull { it.muscles.firstOrNull() }.forEach { share ->
            if (!names.containsKey(share.id)) names[share.id] = share.name
        }

        val currentPrimary = primaryCount(session)
        val ids = (totals.keys + currentPrimary.keys).toSet()
        if (ids.isEmpty()) return emptyList()

        return ids.map { id ->
            val occ = occurrences[id] ?: trainings.size.coerceAtLeast(1)
            val average = (totals[id] ?: 0.0) / occ.toDouble()
            val current = currentPrimary[id] ?: 0.0
            val name = names[id] ?: id
            MuscleTarget(id = id, name = name, average = average, current = current)
        }.sortedByDescending { it.deficit }
    }

    private fun computeMuscleLoads(
        trainings: List<TrainingSummary>,
        exampleContextMap: Map<String, ExampleContext>
    ): List<MuscleLoad> {
        val loads = mutableMapOf<String, MuscleLoad>()
        trainings.forEach { training ->
            val musclesSeen = mutableSetOf<String>()
            training.exercises.forEach { exercise ->
                val context = exampleContextMap[exercise.exampleId] ?: return@forEach
                context.muscles.forEach { muscle ->
                    val load = loads.getOrPut(muscle.name) { MuscleLoad(muscle.name, 0, 0) }
                    load.total += muscle.percentage
                    if (musclesSeen.add(muscle.name)) load.sessions += 1
                }
            }
        }
        return loads.values.sortedByDescending { it.total }.take(MAX_MUSCLE_SUMMARY)
    }

    private fun computeResidualFatigueByMuscle(
        now: LocalDateTime,
        trainings: List<TrainingSummary>
    ): Map<String, Double> {
        if (trainings.isEmpty()) return emptyMap()

        val rawIntensities = trainings.mapNotNull { tr ->
            tr.avgIntensity.takeIf { it.isFinite() && it > 0f }?.toDouble()
        }
        if (rawIntensities.isEmpty()) return emptyMap()

        val sorted = rawIntensities.sorted()
        val q10 = PromptMath.quantileDouble(sorted, 0.10)
        val q90 = PromptMath.quantileDouble(sorted, 0.90)

        fun robustNorm(i: Double): Double {
            if (!(q90 > q10)) return 1.0
            val iWin = i.coerceIn(q10, q90)
            return ((iWin - q10) / (q90 - q10)).coerceIn(0.0, 1.0)
        }

        val residual = mutableMapOf<String, Double>()

        trainings.forEach { tr ->
            val iRaw = tr.avgIntensity.toDouble()
            if (!iRaw.isFinite() || iRaw <= 0.0) return@forEach
            val iNorm = robustNorm(iRaw)
            if (iNorm <= 0.0) return@forEach


            val hoursSince =
                (DateTimeUtils.asInstant(now) - DateTimeUtils.asInstant(tr.performedAt)).inWholeHours

            tr.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    val rh = share.recovery ?: return@forEach
                    if (rh <= 0) return@forEach

                    val decay = 1.0 - (hoursSince.toDouble() / rh.toDouble())
                    if (decay <= 0.0) return@forEach

                    val pct = share.percentage.coerceAtLeast(0).toDouble() / 100.0
                    val contrib = pct * iNorm * PromptMath.clamp01(decay)
                    if (contrib <= 0.0) return@forEach

                    residual[share.id] = ((residual[share.id] ?: 0.0) + contrib).coerceAtMost(1.0)
                }
            }
        }
        return residual
    }

    private fun computeLastLoadDateTimeByMuscle(
        trainings: List<TrainingSummary>,
    ): Map<String, LocalDateTime> {
        val last = mutableMapOf<String, LocalDateTime>()
        trainings.forEach { training ->
            val ts = training.performedAt
            training.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    if (share.percentage >= SuggestionMath.SIGNIFICANT_SHARE_THRESHOLD) {
                        val prev = last[share.id]
                        if (prev == null || prev < ts) last[share.id] = ts
                    }
                }
            }
        }
        return last
    }

    private fun computePeriodicHabits(
        trainings: List<TrainingSummary>,
    ): Map<String, PeriodicHabit> {
        val byMuscleDates = mutableMapOf<String, MutableList<LocalDate>>()
        val names = mutableMapOf<String, String>()

        trainings.sortedBy { it.performedAt }.forEach { tr ->
            val date = tr.performedAt.date
            tr.exercises.forEach { ex ->
                ex.muscles.forEach { share ->
                    if (share.percentage >= SuggestionMath.SIGNIFICANT_SHARE_THRESHOLD) {
                        val list = byMuscleDates.getOrPut(share.id) { mutableListOf() }
                        if (list.lastOrNull() != date) list.add(date)
                        names.getOrPut(share.id) { share.name }
                    }
                }
            }
        }

        return buildMap {
            byMuscleDates.forEach { (id, dates) ->
                if (dates.size < HabitCycleConfig.MIN_EVENTS) return@forEach
                val intervals = dates.zipWithNext { a, b -> a.daysUntil(b) }.filter { it > 0 }
                if (intervals.size < HabitCycleConfig.MIN_INTERVALS) return@forEach

                val median = PromptMath.medianInt(intervals).coerceAtLeast(1)
                val recent = intervals.takeLast(HabitCycleConfig.RECENT_WINDOW)
                val recentMedian = PromptMath.medianInt(recent)

                val iqr = PromptMath.iqrInt(intervals).toDouble()
                val spread = if (median > 0) (iqr / median.toDouble()) else 1.0
                val confidence = PromptMath.clamp01(1.0 - spread)

                val alpha = 0.5 * (1.0 - confidence)
                val blended =
                    ((1 - alpha) * median + alpha * recentMedian).roundToInt().coerceAtLeast(1)

                val lastDate = dates.last()
                val nextDue = lastDate.plus(blended, DateTimeUnit.DAY)
                put(
                    id,
                    PeriodicHabit(
                        muscleId = id,
                        muscleName = names[id] ?: id,
                        medianIntervalDays = blended,
                        confidence = confidence,
                        lastDate = lastDate,
                        nextDue = nextDue
                    )
                )
            }
        }
    }

    private fun computeSessionHabits(
        trainings: List<TrainingSummary>,
    ): Map<String, SessionHabit> {
        val indicesByMuscle = mutableMapOf<String, MutableList<Int>>()

        trainings.forEachIndexed { idx, tr ->
            tr.exercises.forEach { ex ->
                val primary = ex.muscles.firstOrNull()
                if (primary != null && primary.percentage >= SuggestionMath.SIGNIFICANT_SHARE_THRESHOLD) {
                    indicesByMuscle.getOrPut(primary.id) { mutableListOf() }.add(idx)
                }
            }
        }

        return buildMap {
            indicesByMuscle.forEach { (muscleId, rawIdxs) ->
                val idxsAsc = rawIdxs.sorted()
                if (idxsAsc.size < HabitCycleConfig.MIN_EVENTS) return@forEach
                val intervals = idxsAsc.zipWithNext { a, b -> b - a }.filter { it > 0 }
                if (intervals.size < HabitCycleConfig.MIN_INTERVALS) return@forEach

                val median = PromptMath.medianInt(intervals).coerceAtLeast(1)
                val lastSeenIdx = idxsAsc.firstOrNull() ?: -1
                val nextDueIdx = if (lastSeenIdx >= 0) lastSeenIdx + median else median

                put(
                    muscleId,
                    SessionHabit(
                        muscleId = muscleId,
                        medianIntervalSessions = median,
                        lastSeenIdx = lastSeenIdx,
                        nextDueIdx = nextDueIdx
                    )
                )
            }
        }
    }
}
