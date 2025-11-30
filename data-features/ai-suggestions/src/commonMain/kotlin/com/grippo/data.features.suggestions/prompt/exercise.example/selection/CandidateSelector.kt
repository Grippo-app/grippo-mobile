package com.grippo.data.features.suggestions.prompt.exercise.example.selection

import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.suggestions.prompt.exercise.example.config.CandidateTierConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.HabitCycleConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.PromptGuidelineConfig
import com.grippo.data.features.suggestions.prompt.exercise.example.config.SuggestionMath
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleCatalog
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExampleContext
import com.grippo.data.features.suggestions.prompt.exercise.example.model.ExerciseSummary
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PeriodicHabit
import com.grippo.data.features.suggestions.prompt.exercise.example.model.PredictionSignals
import com.grippo.data.features.suggestions.prompt.exercise.example.model.SessionHabit
import com.grippo.data.features.suggestions.prompt.exercise.example.model.TrainingSummary
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.PromptMath
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.isPrimaryRecoveredByResidual
import com.grippo.data.features.suggestions.prompt.exercise.example.utils.unrecoveredWeightedShare
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus
import org.koin.core.annotation.Single
import kotlin.math.abs

/**
 * Ranks and filters example contexts using muscle deficits, cycles, equipment
 * conflicts, and other heuristics to choose the final candidate list.
 */
@Single
internal class CandidateSelector {

    fun select(
        catalog: ExampleCatalog,
        signals: PredictionSignals,
        nowDateTime: LocalDateTime
    ): List<ExampleContext> {
        val allUnrecShares = catalog.contexts
            .map { it.unrecoveredWeightedShare(signals.residualFatigueByMuscle) }
        val q75Unrec = PromptMath.quantileDouble(allUnrecShares, 0.75).coerceIn(0.0, 1.0)
        val tierALimit = minOf(CandidateTierConfig.STRICT_UNREC_WEIGHTED_SHARE_LIMIT, q75Unrec)

        val comparator = exampleComparator(signals, nowDateTime)

        fun isPreferred(ctx: ExampleContext): Boolean {
            val pm = ctx.primaryMuscleId() ?: return false
            val st =
                combinedCycleState(pm, nowDateTime, signals.periodicHabits, signals.sessionHabits)
            return st == CycleState.DUE || st == CycleState.OVERDUE
        }

        fun rankWithinTier(seq: Sequence<ExampleContext>): List<ExampleContext> {
            val base = seq
                .filterNot { it.id in signals.performedExampleIds }
                .filter { it.muscles.isNotEmpty() }
                .toList()

            val preferred = base.filter(::isPreferred).sortedWith(comparator)
            if (preferred.size >= CandidateTierConfig.MAX_CANDIDATE_COUNT) {
                return preferred.take(CandidateTierConfig.MAX_CANDIDATE_COUNT)
            }

            val others = base.filterNot(::isPreferred).sortedWith(comparator)
            return (preferred + others).take(CandidateTierConfig.MAX_CANDIDATE_COUNT)
        }

        val tierA = rankWithinTier(
            catalog.contexts.asSequence()
                .filter { it.isPrimaryRecoveredByResidual(signals.residualFatigueByMuscle) }
                .filter { it.unrecoveredWeightedShare(signals.residualFatigueByMuscle) <= tierALimit }
        )
        if (tierA.isNotEmpty()) return tierA

        val tierB = rankWithinTier(
            catalog.contexts.asSequence()
                .filter { it.isPrimaryRecoveredByResidual(signals.residualFatigueByMuscle) }
        )
        if (tierB.isNotEmpty()) return tierB

        return rankWithinTier(catalog.contexts.asSequence())
    }

    private fun exampleComparator(
        signals: PredictionSignals,
        nowDateTime: LocalDateTime
    ): Comparator<ExampleContext> {
        return Comparator { left, right ->
            fun primaryDeficit(ctx: ExampleContext): Double {
                val pm = ctx.primaryMuscleId() ?: return 0.0
                return (signals.muscleDeficits[pm] ?: 0.0).coerceAtLeast(0.0)
            }

            val dL = primaryDeficit(left)
            val dR = primaryDeficit(right)
            val deficitCmp =
                dR.compareTo(dL).takeIf { abs(dL - dR) >= SuggestionMath.DEFICIT_EPS } ?: 0
            if (deficitCmp != 0) return@Comparator deficitCmp

            fun cycleRank(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId() ?: return 2
                return when (combinedCycleState(
                    pm,
                    nowDateTime,
                    signals.periodicHabits,
                    signals.sessionHabits
                )) {
                    CycleState.OVERDUE -> 0
                    CycleState.DUE -> 1
                    CycleState.NONE -> 2
                }
            }

            val cL = cycleRank(left)
            val cR = cycleRank(right)
            val cycleCmp = cL.compareTo(cR)
            if (cycleCmp != 0) return@Comparator cycleCmp

            fun equipPenalty(ctx: ExampleContext): Int {
                val conflict = ctx.equipmentIds.any { it in signals.usedEquipmentIds }
                return if (conflict) 1 else 0
            }

            val eL = equipPenalty(left)
            val eR = equipPenalty(right)
            val equipCmp = eL.compareTo(eR)
            if (equipCmp != 0) return@Comparator equipCmp

            fun prefMismatch(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId()
                val preferred =
                    preferredCategoryForMuscleNext(pm, signals.trainings, signals.session)
                return if (preferred != null && ctx.category != preferred) 1 else 0
            }

            val pmL = prefMismatch(left)
            val pmR = prefMismatch(right)
            val pmCmp = pmL.compareTo(pmR)
            if (pmCmp != 0) return@Comparator pmCmp

            val catCmp = signals.categoryStats.priorityFor(left.category, signals.stage)
                .compareTo(signals.categoryStats.priorityFor(right.category, signals.stage))
            if (catCmp != 0) return@Comparator catCmp

            fun mono(ctx: ExampleContext): Int {
                val pm = ctx.primaryMuscleId() ?: return 0
                val residual = (signals.residualFatigueByMuscle[pm] ?: 0.0).coerceIn(0.0, 1.0)
                if (residual < CandidateTierConfig.RESIDUAL_DEAD_ZONE) return 0
                return antiMonotonyPenalty(ctx, nowDateTime, signals.lastLoadByMuscleDateTime)
            }

            val mL = mono(left)
            val mR = mono(right)
            val monoCmp = mL.compareTo(mR)
            if (monoCmp != 0) return@Comparator monoCmp

            val useCmp = left.usageCount.compareTo(right.usageCount)
            if (useCmp != 0) return@Comparator useCmp

            val lastUsedCmp = compareLastUsed(left.lastUsed, right.lastUsed)
            if (lastUsedCmp != 0) return@Comparator lastUsedCmp

            val idCmp = left.id.compareTo(right.id)
            if (idCmp != 0) return@Comparator idCmp

            0
        }
    }

    private enum class CycleState { NONE, DUE, OVERDUE }

    private fun dayCycleState(
        muscleId: String,
        now: LocalDateTime,
        periodicHabits: Map<String, PeriodicHabit>
    ): CycleState {
        val h = periodicHabits[muscleId] ?: return CycleState.NONE
        val today = now.date
        val due = h.nextDue
        val graceBefore = due.minus(HabitCycleConfig.GRACE_DAYS, DateTimeUnit.DAY)
        return when {
            today > due -> CycleState.OVERDUE
            today >= graceBefore -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun sessionCycleState(
        muscleId: String,
        sessionHabits: Map<String, SessionHabit>
    ): CycleState {
        val h = sessionHabits[muscleId] ?: return CycleState.NONE
        val lastSeen = h.lastSeenIdx
        if (lastSeen < 0) return CycleState.NONE
        val sessionsSince = lastSeen
        return when {
            sessionsSince > h.medianIntervalSessions + HabitCycleConfig.GRACE_SESSIONS -> CycleState.OVERDUE
            sessionsSince >= h.medianIntervalSessions - HabitCycleConfig.GRACE_SESSIONS -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun combinedCycleState(
        muscleId: String,
        now: LocalDateTime,
        periodicHabits: Map<String, PeriodicHabit>,
        sessionHabits: Map<String, SessionHabit>
    ): CycleState {
        val d = dayCycleState(muscleId, now, periodicHabits)
        val s = sessionCycleState(muscleId, sessionHabits)
        return when {
            d == CycleState.OVERDUE || s == CycleState.OVERDUE -> CycleState.OVERDUE
            d == CycleState.DUE || s == CycleState.DUE -> CycleState.DUE
            else -> CycleState.NONE
        }
    }

    private fun antiMonotonyPenalty(
        ctx: ExampleContext,
        nowDateTime: LocalDateTime,
        lastLoadByMuscleDateTime: Map<String, LocalDateTime>
    ): Int {
        val pm = ctx.primaryMuscleId() ?: return 0
        val last = lastLoadByMuscleDateTime[pm] ?: return 0

        val hours =
            (DateTimeUtils.asInstant(nowDateTime) - DateTimeUtils.asInstant(last)).inWholeHours
        return if (hours < PromptGuidelineConfig.ANTI_MONOTONY_HOURS.toLong()) 1 else 0
    }

    private fun preferredCategoryForMuscleNext(
        muscleId: String?,
        trainings: List<TrainingSummary>,
        session: List<ExerciseSummary>
    ): String? {
        if (muscleId.isNullOrBlank()) return null

        var histSessions = 0
        var histCompound = 0.0
        var histIsolation = 0.0

        trainings.forEach { tr ->
            var c = 0.0
            var i = 0.0
            tr.exercises.forEach { ex ->
                if (ex.muscles.firstOrNull()?.id == muscleId) {
                    when (ex.category) {
                        CategoryEnum.COMPOUND.key -> c += 1.0
                        CategoryEnum.ISOLATION.key -> i += 1.0
                    }
                }
            }
            if (c > 0.0 || i > 0.0) {
                histSessions += 1
                histCompound += c
                histIsolation += i
            }
        }

        val avgCompound = if (histSessions > 0) histCompound / histSessions.toDouble() else 1.0
        val avgIsolation = if (histSessions > 0) histIsolation / histSessions.toDouble() else 1.0

        var curCompound = 0.0
        var curIsolation = 0.0
        session.forEach { ex ->
            if (ex.muscles.firstOrNull()?.id == muscleId) {
                when (ex.category) {
                    CategoryEnum.COMPOUND.key -> curCompound += 1.0
                    CategoryEnum.ISOLATION.key -> curIsolation += 1.0
                }
            }
        }

        val progCompound = if (avgCompound > 0) curCompound / avgCompound else 1.0
        val progIsolation = if (avgIsolation > 0) curIsolation / avgIsolation else 1.0

        return when {
            progCompound < progIsolation -> CategoryEnum.COMPOUND.key
            progIsolation < progCompound -> CategoryEnum.ISOLATION.key
            else -> null
        }
    }

    private fun compareLastUsed(left: LocalDateTime?, right: LocalDateTime?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> -1
            right == null -> 1
            else -> left.compareTo(right)
        }
    }
}
