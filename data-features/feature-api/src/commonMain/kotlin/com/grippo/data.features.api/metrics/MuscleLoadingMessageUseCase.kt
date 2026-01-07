package com.grippo.data.features.api.metrics

import com.grippo.data.features.api.metrics.models.DominanceInfo
import com.grippo.data.features.api.metrics.models.FocusTarget
import com.grippo.data.features.api.metrics.models.GroupShare
import com.grippo.data.features.api.metrics.models.MuscleLoadEntry
import com.grippo.data.features.api.metrics.models.MuscleLoadSummary
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessage
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageKind
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageStrength
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageSuffix
import com.grippo.data.features.api.metrics.models.MuscleLoadingMessageTone
import com.grippo.data.features.api.metrics.models.MuscleShare
import com.grippo.data.features.api.muscle.models.MuscleEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum

public class MuscleLoadingMessageUseCase {

    private companion object Companion {
        private const val EPS = 1e-3f

        private const val DOMINANT_GROUP_TOP1_MIN = 40f
        private const val DOMINANT_GROUP_GAP_MIN = 12f

        private const val BALANCED_GROUP_TOP1_MAX = 35f
        private const val BALANCED_GROUP_GAP_MAX = 10f

        private const val DUAL_GROUP_TOP2_SUM_MIN = 65f

        private const val MUSCLE_LEAD_TOP1_MIN = 70f
        private const val MUSCLE_LEAD_GAP_MIN = 18f

        private const val TOP_GROUPS_KEEP = 4
        private const val TOP_MUSCLES_KEEP = 6
    }

    public fun fromSummary(
        summary: MuscleLoadSummary,
        isFirstWorkout: Boolean,
        tone: MuscleLoadingMessageTone = MuscleLoadingMessageTone.Compact,
        includeSuffix: Boolean = true,
    ): MuscleLoadingMessage {
        val groupEntries = summary.perGroup.entries
            .filter { it.percentage.isFinite() && it.percentage > EPS }
            .sortedByDescending { it.percentage }

        val muscleEntries = summary.perMuscle.entries
            .filter { it.percentage.isFinite() && it.percentage > EPS }
            .sortedByDescending { it.percentage }

        val suffix = resolveSuffix(
            isFirstWorkout = isFirstWorkout,
            includeSuffix = includeSuffix,
        )

        val dominance = DominanceInfo(
            top1SharePercent = summary.dominance.top1SharePercent.coerceIn(0f, 100f),
            top2SharePercent = summary.dominance.top2SharePercent.coerceIn(0f, 100f),
        )

        if (groupEntries.isEmpty()) {
            return MuscleLoadingMessage(
                kind = MuscleLoadingMessageKind.Empty,
                tone = tone,
                suffix = suffix,
                variant = 0,
                strength = MuscleLoadingMessageStrength.None,
                primary = null,
                secondary = null,
                involvedGroups = emptyList(),
                topGroups = emptyList(),
                topMuscles = emptyList(),
                dominance = dominance,
            )
        }

        val top1GroupEntry = groupEntries.getOrNull(0)
        val top2GroupEntry = groupEntries.getOrNull(1)

        val top1Percent = (top1GroupEntry?.percentage ?: 0f).coerceIn(0f, 100f)
        val top2Percent = (top2GroupEntry?.percentage ?: 0f).coerceIn(0f, 100f)

        val gap = top1Percent - top2Percent
        val top2Sum = top1Percent + top2Percent

        val kind = resolveKind(
            top1Percent = top1Percent,
            gap = gap,
            top2Sum = top2Sum,
        )

        val involvedGroups = groupEntries.map { it.group }
        val topGroups = groupEntries
            .take(TOP_GROUPS_KEEP)
            .map { GroupShare(group = it.group, percent = it.percentage.coerceIn(0f, 100f)) }

        val topMuscles = muscleEntries
            .take(TOP_MUSCLES_KEEP)
            .mapNotNull { entry ->
                val muscle = entry.muscles.firstOrNull() ?: return@mapNotNull null
                MuscleShare(
                    group = entry.group,
                    muscle = muscle,
                    score = entry.percentage.coerceIn(0f, 100f),
                )
            }

        val primaryGroup = top1GroupEntry?.group
        val secondaryGroup = top2GroupEntry?.group

        val primary = if (primaryGroup != null) {
            buildPrimaryFocus(
                group = primaryGroup,
                muscleEntries = muscleEntries,
                kind = kind,
            )
        } else {
            null
        }

        val secondary = if (kind == MuscleLoadingMessageKind.DualGroup && secondaryGroup != null) {
            FocusTarget(group = secondaryGroup, muscle = null)
        } else {
            null
        }

        val primaryLead = if (primary != null) {
            computeMuscleLeadInGroup(primary.group, muscleEntries)
        } else {
            null
        }

        val strength = resolveStrength(
            top1Percent = top1Percent,
            gap = gap,
            dominanceTop1 = summary.dominance.top1SharePercent,
            primaryMuscleLeadGap = primaryLead?.gap,
            primaryHasMuscleFocus = primary?.muscle != null,
        )

        return MuscleLoadingMessage(
            kind = kind,
            tone = tone,
            suffix = suffix,
            variant = 0,
            strength = strength,
            primary = primary,
            secondary = secondary,
            involvedGroups = involvedGroups,
            topGroups = topGroups,
            topMuscles = topMuscles,
            dominance = dominance,
        )
    }

    private fun resolveSuffix(
        isFirstWorkout: Boolean,
        includeSuffix: Boolean,
    ): MuscleLoadingMessageSuffix {
        if (!includeSuffix) return MuscleLoadingMessageSuffix.None
        return if (isFirstWorkout) MuscleLoadingMessageSuffix.First else MuscleLoadingMessageSuffix.Regular
    }

    private fun resolveKind(
        top1Percent: Float,
        gap: Float,
        top2Sum: Float,
    ): MuscleLoadingMessageKind {
        val isDominant = top1Percent >= DOMINANT_GROUP_TOP1_MIN || gap >= DOMINANT_GROUP_GAP_MIN
        if (isDominant) return MuscleLoadingMessageKind.DominantGroup

        val isBalanced = top1Percent <= BALANCED_GROUP_TOP1_MAX && gap <= BALANCED_GROUP_GAP_MAX
        if (isBalanced) return MuscleLoadingMessageKind.Balanced

        val isDual = top2Sum >= DUAL_GROUP_TOP2_SUM_MIN
        if (isDual) return MuscleLoadingMessageKind.DualGroup

        return MuscleLoadingMessageKind.Mixed
    }

    private fun buildPrimaryFocus(
        group: MuscleGroupEnum,
        muscleEntries: List<MuscleLoadEntry>,
        kind: MuscleLoadingMessageKind,
    ): FocusTarget {
        val topMuscle = pickTopMuscleInGroup(group, muscleEntries)
            ?: return FocusTarget(group = group, muscle = null)

        val lead = computeMuscleLeadInGroup(
            group = group,
            muscleEntries = muscleEntries,
        )

        val allowMuscleFocus = when (kind) {
            MuscleLoadingMessageKind.DominantGroup -> true
            MuscleLoadingMessageKind.DualGroup -> true
            MuscleLoadingMessageKind.Mixed -> true
            MuscleLoadingMessageKind.Balanced -> false
            MuscleLoadingMessageKind.Empty -> false
        }

        val shouldUseMuscle = allowMuscleFocus && lead.isStrongLead
        return if (shouldUseMuscle) {
            FocusTarget(group = group, muscle = topMuscle)
        } else {
            FocusTarget(group = group, muscle = null)
        }
    }

    private fun pickTopMuscleInGroup(
        group: MuscleGroupEnum,
        muscleEntries: List<MuscleLoadEntry>,
    ): MuscleEnum? {
        val entry = muscleEntries.firstOrNull { it.group == group } ?: return null
        return entry.muscles.firstOrNull()
    }

    private fun computeMuscleLeadInGroup(
        group: MuscleGroupEnum,
        muscleEntries: List<MuscleLoadEntry>,
    ): MuscleLead {
        val inGroup = muscleEntries
            .filter { it.group == group }
            .sortedByDescending { it.percentage }

        val top1 = inGroup.getOrNull(0)
        val top2 = inGroup.getOrNull(1)

        val p1 = (top1?.percentage ?: 0f).coerceIn(0f, 100f)
        val p2 = (top2?.percentage ?: 0f).coerceIn(0f, 100f)

        val gap = p1 - p2
        val strong = p1 >= MUSCLE_LEAD_TOP1_MIN || gap >= MUSCLE_LEAD_GAP_MIN

        return MuscleLead(
            gap = gap,
            isStrongLead = strong,
        )
    }

    private fun resolveStrength(
        top1Percent: Float,
        gap: Float,
        dominanceTop1: Float,
        primaryMuscleLeadGap: Float?,
        primaryHasMuscleFocus: Boolean,
    ): MuscleLoadingMessageStrength {
        val t1 = top1Percent.coerceIn(0f, 100f)
        val g = gap.coerceIn(-100f, 100f)
        val d1 = dominanceTop1.coerceIn(0f, 100f)

        val strongByGroup = t1 >= 45f || g >= 18f
        val strongByDominance = d1 >= 40f
        val strongByMuscleLead =
            primaryHasMuscleFocus && (primaryMuscleLeadGap != null && primaryMuscleLeadGap >= 18f)

        if (strongByGroup || strongByDominance || strongByMuscleLead) {
            return MuscleLoadingMessageStrength.Strong
        }

        val medium = t1 >= 35f || g >= 10f || d1 >= 25f
        if (medium) return MuscleLoadingMessageStrength.Medium

        return MuscleLoadingMessageStrength.Light
    }

    private data class MuscleLead(
        val gap: Float,
        val isStrongLead: Boolean,
    )
}
