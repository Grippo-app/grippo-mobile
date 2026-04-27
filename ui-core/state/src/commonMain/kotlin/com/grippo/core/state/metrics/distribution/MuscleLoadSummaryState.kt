package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.muscle_load_summary_action_balanced
import com.grippo.design.resources.provider.muscle_load_summary_action_clear_split_no_target
import com.grippo.design.resources.provider.muscle_load_summary_action_clear_split_with_target
import com.grippo.design.resources.provider.muscle_load_summary_action_mild_bias_no_target
import com.grippo.design.resources.provider.muscle_load_summary_action_mild_bias_with_target
import com.grippo.design.resources.provider.muscle_load_summary_action_strong_focus_no_target
import com.grippo.design.resources.provider.muscle_load_summary_action_strong_focus_with_target
import com.grippo.design.resources.provider.muscle_load_summary_assist_suppressed
import com.grippo.design.resources.provider.muscle_load_summary_empty
import com.grippo.design.resources.provider.muscle_load_summary_headline_balanced
import com.grippo.design.resources.provider.muscle_load_summary_headline_clear_split
import com.grippo.design.resources.provider.muscle_load_summary_headline_mild_bias
import com.grippo.design.resources.provider.muscle_load_summary_headline_strong_focus
import com.grippo.design.resources.provider.muscle_load_summary_note
import com.grippo.design.resources.provider.muscle_load_summary_second_driver
import com.grippo.design.resources.provider.muscle_load_summary_stimulus_higher_than_volume
import com.grippo.design.resources.provider.muscle_load_summary_stimulus_volume_match
import com.grippo.design.resources.provider.muscle_load_summary_tail_with_weakest
import com.grippo.design.resources.provider.muscle_load_summary_tail_without_weakest
import com.grippo.design.resources.provider.muscle_load_summary_top_group_fallback
import com.grippo.design.resources.provider.muscle_load_summary_top_muscle
import com.grippo.design.resources.provider.muscle_load_summary_volume_higher_than_stimulus
import com.grippo.design.resources.provider.muscle_load_summary_weakest_group_fallback
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToInt
import kotlin.random.Random

// Hoisted: assist relationships are static and never change.
private val ASSIST_GROUPS_BY_TOP: Map<MuscleGroupEnumState, Set<MuscleGroupEnumState>> = mapOf(
    MuscleGroupEnumState.CHEST_MUSCLES to
            setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS, MuscleGroupEnumState.SHOULDER_MUSCLES),
    MuscleGroupEnumState.BACK_MUSCLES to
            setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS),
    MuscleGroupEnumState.SHOULDER_MUSCLES to
            setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS),
    MuscleGroupEnumState.LEGS to emptySet(),
    MuscleGroupEnumState.ABDOMINAL_MUSCLES to emptySet(),
    MuscleGroupEnumState.ARMS_AND_FOREARMS to emptySet(),
)

@Immutable
public data class MuscleLoadSummaryState(
    val meta: MuscleLoadMetaState,
    val perGroup: MuscleLoadBreakdownState,
    val perMuscle: MuscleLoadBreakdownState,
    val volumePerGroup: MuscleLoadBreakdownState,
    val volumePerMuscle: MuscleLoadBreakdownState,
    val dominance: MuscleLoadDominanceState,
    val groupDominance: MuscleLoadDominanceState,
) {

    @Composable
    public fun tip(): String {
        // Pure step: figure out the headline kind, percentages and which groups to mention.
        val plan = remember(this) { computeTipPlan() }
        if (plan == null) {
            return AppTokens.strings.res(Res.string.muscle_load_summary_empty)
        }

        // Resolve all required group/muscle names into strings (composable step).
        val strings = AppTokens.strings
        val topGroupName: String = plan.topGroup
            ?.let { strings.res(it.title().resOrNull() ?: return@let null) }
            ?: strings.res(Res.string.muscle_load_summary_top_group_fallback)

        val weakestGroupName: String? =
            plan.weakestGroup?.let { strings.res(it.title().resOrNull() ?: return@let null) }

        val secondGroupName: String? =
            plan.secondGroup?.let { strings.res(it.title().resOrNull() ?: return@let null) }

        val topMuscleName: String? =
            plan.topMuscle?.let { strings.res(it.title().resOrNull() ?: return@let null) }

        // Render: headline + actionable + extras + note. All pieces are composable
        // (strings.res itself is @Composable), so we can't wrap them in remember.
        val headline: String = when (plan.headlineKind) {
            HeadlineKind.STRONG_FOCUS -> strings.res(
                Res.string.muscle_load_summary_headline_strong_focus,
                topGroupName, plan.top1i, plan.top2i,
            )

            HeadlineKind.CLEAR_SPLIT -> strings.res(
                Res.string.muscle_load_summary_headline_clear_split,
                plan.top2i, plan.top1i, plan.secondI,
            )

            HeadlineKind.BALANCED -> strings.res(
                Res.string.muscle_load_summary_headline_balanced,
                plan.top1i, plan.top2i,
            )

            HeadlineKind.MILD_BIAS -> strings.res(
                Res.string.muscle_load_summary_headline_mild_bias,
                plan.top1i, plan.top2i, plan.restI,
            )
        }

        val actionable: String = when (plan.headlineKind) {
            HeadlineKind.STRONG_FOCUS -> if (weakestGroupName != null) {
                strings.res(
                    Res.string.muscle_load_summary_action_strong_focus_with_target,
                    weakestGroupName
                )
            } else {
                strings.res(Res.string.muscle_load_summary_action_strong_focus_no_target)
            }

            HeadlineKind.CLEAR_SPLIT -> if (weakestGroupName != null) {
                strings.res(
                    Res.string.muscle_load_summary_action_clear_split_with_target,
                    weakestGroupName
                )
            } else {
                strings.res(Res.string.muscle_load_summary_action_clear_split_no_target)
            }

            HeadlineKind.BALANCED ->
                strings.res(Res.string.muscle_load_summary_action_balanced)

            HeadlineKind.MILD_BIAS -> if (weakestGroupName != null) {
                strings.res(
                    Res.string.muscle_load_summary_action_mild_bias_with_target,
                    weakestGroupName
                )
            } else {
                strings.res(Res.string.muscle_load_summary_action_mild_bias_no_target)
            }
        }

        val topMuscleLine: String? = if (topMuscleName != null && plan.topMusclePercent != null) {
            strings.res(
                Res.string.muscle_load_summary_top_muscle,
                topMuscleName,
                plan.topMusclePercent
            )
        } else null

        val volumeStyleLine: String? = when (plan.volumeStyle) {
            VolumeStyle.HIGHER_THAN_STIMULUS -> topGroupName.let {
                strings.res(Res.string.muscle_load_summary_volume_higher_than_stimulus, it)
            }

            VolumeStyle.LOWER_THAN_STIMULUS -> topGroupName.let {
                strings.res(Res.string.muscle_load_summary_stimulus_higher_than_volume, it)
            }

            VolumeStyle.MATCH ->
                strings.res(Res.string.muscle_load_summary_stimulus_volume_match)

            null -> null
        }

        val assistLine: String? = when (plan.assist) {
            AssistHint.SUPPRESSED ->
                strings.res(Res.string.muscle_load_summary_assist_suppressed)

            AssistHint.SECOND_DRIVER -> secondGroupName?.let {
                strings.res(Res.string.muscle_load_summary_second_driver, it)
            }

            null -> null
        }

        val tailLine: String? = plan.tail?.let { tail ->
            val name = weakestGroupName
                ?: strings.res(Res.string.muscle_load_summary_weakest_group_fallback)
            if (tail.weakestShare != null) {
                strings.res(
                    Res.string.muscle_load_summary_tail_with_weakest,
                    tail.restPercent,
                    name,
                    tail.weakestShare
                )
            } else {
                strings.res(Res.string.muscle_load_summary_tail_without_weakest, tail.restPercent)
            }
        }

        val note = strings.res(Res.string.muscle_load_summary_note)

        // Single allocation, no intermediate List.
        return buildString {
            append(headline)
            append(' ')
            append(actionable)
            val extra = buildExtra(topMuscleLine, volumeStyleLine, assistLine, tailLine)
            if (extra.isNotEmpty()) {
                append(' ')
                append(extra)
            }
            if (note.isNotBlank()) {
                append(' ')
                append(note)
            }
        }
    }

    /**
     * Pure (non-composable) computation. Returns null when there is nothing to say
     * (both per-group and per-muscle breakdowns are empty).
     */
    private fun computeTipPlan(): TipPlan? {
        val groupEntries = perGroup.entries
        val muscleEntries = perMuscle.entries
        if (groupEntries.isEmpty() && muscleEntries.isEmpty()) return null

        val sortedGroups = groupEntries.sortedByDescending { it.value }
        val sortedMuscles = muscleEntries.sortedByDescending { it.value }

        val top1 = groupDominance.top1SharePercent.coerceIn(0f, 100f)
        val top2 = groupDominance.top2SharePercent.coerceIn(0f, 100f)
        val top1i = top1.roundToInt().coerceIn(0, 100)
        val top2i = top2.roundToInt().coerceIn(0, 100)
        val secondShare = (top2 - top1).coerceIn(0f, 100f)
        val restShare = (100f - top2).coerceIn(0f, 100f)
        val secondI = secondShare.roundToInt().coerceIn(0, 100)
        val restI = restShare.roundToInt().coerceIn(0, 100)

        val topGroupEntry = sortedGroups.firstOrNull()
        val secondGroupEntry = sortedGroups.getOrNull(1)
        val weakestGroupEntry = sortedGroups.lastOrNull()

        val topGroup = topGroupEntry?.group
        val weakestGroup = weakestGroupEntry?.group
        val weakestShare = weakestGroupEntry?.value?.coerceIn(0f, 100f)

        val topMuscleEntry = sortedMuscles.firstOrNull()
        val topMuscle = topMuscleEntry?.muscles?.firstOrNull()
        val topMusclePercent = topMuscleEntry?.value
            ?.coerceIn(0f, 100f)
            ?.roundToInt()
            ?.coerceIn(0, 100)

        val headlineKind = when {
            top1 >= 60f -> HeadlineKind.STRONG_FOCUS
            top2 >= 80f -> HeadlineKind.CLEAR_SPLIT
            top1 <= 42f && top2 <= 70f -> HeadlineKind.BALANCED
            else -> HeadlineKind.MILD_BIAS
        }

        val volumeStyle = computeVolumeStyle(topGroup)
        val assist = computeAssistHint(topGroupEntry, secondGroupEntry)
        val tail = computeTail(restShare, weakestShare)

        return TipPlan(
            headlineKind = headlineKind,
            topGroup = topGroup,
            secondGroup = secondGroupEntry?.group,
            weakestGroup = weakestGroup,
            topMuscle = topMuscle,
            top1i = top1i,
            top2i = top2i,
            secondI = secondI,
            restI = restI,
            topMusclePercent = topMusclePercent,
            volumeStyle = volumeStyle,
            assist = assist,
            tail = tail,
        )
    }

    private fun computeVolumeStyle(topGroup: MuscleGroupEnumState?): VolumeStyle? {
        if (topGroup == null) return null
        val s = perGroup.entries.firstOrNull { it.group == topGroup }?.value
            ?.coerceIn(0f, 100f) ?: return null
        val v = volumePerGroup.entries.firstOrNull { it.group == topGroup }?.value
            ?.coerceIn(0f, 100f) ?: return null
        val delta = v - s
        return when {
            delta >= 8f -> VolumeStyle.HIGHER_THAN_STIMULUS
            delta <= -8f -> VolumeStyle.LOWER_THAN_STIMULUS
            else -> VolumeStyle.MATCH
        }
    }

    private fun computeAssistHint(
        topGroupEntry: MuscleLoadEntryState?,
        secondGroupEntry: MuscleLoadEntryState?,
    ): AssistHint? {
        val topGroup = topGroupEntry?.group ?: return null
        val topShare = topGroupEntry.value.coerceIn(0f, 100f)
        if (topShare < 35f) return null

        val likelyAssistGroups = ASSIST_GROUPS_BY_TOP[topGroup] ?: emptySet()

        if (likelyAssistGroups.isEmpty()) {
            return if (secondGroupEntry?.group != null) AssistHint.SECOND_DRIVER else null
        }

        var assistStimulus = 0.0
        var assistVolume = 0.0
        perGroup.entries.forEach { e ->
            if (e.group in likelyAssistGroups) assistStimulus += e.value.toDouble()
        }
        volumePerGroup.entries.forEach { e ->
            if (e.group in likelyAssistGroups) assistVolume += e.value.toDouble()
        }
        val s = assistStimulus.toFloat().coerceIn(0f, 100f)
        val v = assistVolume.toFloat().coerceIn(0f, 100f)
        val isSuppressed = s <= 10f && v >= 12f

        return when {
            isSuppressed -> AssistHint.SUPPRESSED
            secondGroupEntry?.group != null -> AssistHint.SECOND_DRIVER
            else -> null
        }
    }

    private fun computeTail(restShare: Float, weakestShare: Float?): TailInfo? {
        val rest = restShare.coerceIn(0f, 100f)
        if (rest >= 28f) return null
        val wShare = weakestShare?.coerceIn(0f, 100f)?.roundToInt()?.coerceIn(0, 100)
        return TailInfo(restPercent = rest.roundToInt(), weakestShare = wShare)
    }

    private fun buildExtra(
        topMuscleLine: String?,
        volumeStyleLine: String?,
        assistLine: String?,
        tailLine: String?,
    ): String {
        if (topMuscleLine.isNullOrBlank() &&
            volumeStyleLine.isNullOrBlank() &&
            assistLine.isNullOrBlank() &&
            tailLine.isNullOrBlank()
        ) return ""
        return buildString {
            fun appendIfNotBlank(s: String?) {
                if (!s.isNullOrBlank()) {
                    if (isNotEmpty()) append(' ')
                    append(s)
                }
            }
            appendIfNotBlank(topMuscleLine)
            appendIfNotBlank(volumeStyleLine)
            appendIfNotBlank(assistLine)
            appendIfNotBlank(tailLine)
        }
    }

    @Immutable
    private data class TipPlan(
        val headlineKind: HeadlineKind,
        val topGroup: MuscleGroupEnumState?,
        val secondGroup: MuscleGroupEnumState?,
        val weakestGroup: MuscleGroupEnumState?,
        val topMuscle: MuscleEnumState?,
        val top1i: Int,
        val top2i: Int,
        val secondI: Int,
        val restI: Int,
        val topMusclePercent: Int?,
        val volumeStyle: VolumeStyle?,
        val assist: AssistHint?,
        val tail: TailInfo?,
    )

    @Immutable
    private data class TailInfo(val restPercent: Int, val weakestShare: Int?)

    private enum class HeadlineKind { STRONG_FOCUS, CLEAR_SPLIT, BALANCED, MILD_BIAS }
    private enum class VolumeStyle { HIGHER_THAN_STIMULUS, LOWER_THAN_STIMULUS, MATCH }
    private enum class AssistHint { SUPPRESSED, SECOND_DRIVER }
}

// Helper: extract the StringResource out of UiText.Res so we can call AppString.res
// only once at the call site, instead of going through UiText.text() each time.
private fun UiText.resOrNull(): StringResource? =
    (this as? UiText.Res)?.value

public fun stubMuscleLoadSummary(): MuscleLoadSummaryState {
    return MuscleLoadSummaryState(
        meta = MuscleLoadMetaState(
            trainingsCount = Random.nextInt(5, 60),
            totalExercises = Random.nextInt(40, 400),
            totalSets = Random.nextInt(120, 1500),
            totalRepetitions = Random.nextInt(800, 12000),
            totalVolume = Random.nextDouble(8_000.0, 120_000.0).toFloat(),
            dominantGroup = MuscleGroupEnumState.entries.random(),
        ),
        perGroup = MuscleLoadBreakdownState(
            entries = persistentListOf(
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.CHEST_MUSCLES,
                    value = 42f,
                    muscles = persistentListOf(
                        MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                        MuscleEnumState.PECTORALIS_MAJOR_STERNOCOSTAL
                    ),
                    hitTrainingsCount = 12,
                    primaryTrainingsCount = 8,
                    avgStimulusPerHitSession = 18.5f,
                    maxStimulusInOneSession = 42.3f,
                    avgVolumePerHitSession = 920f,
                    maxVolumeInOneSession = 1580f,
                    topExampleIds = persistentListOf("bench_press", "incline_bench_press"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.BACK_MUSCLES,
                    value = 27f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI),
                    hitTrainingsCount = 10,
                    primaryTrainingsCount = 6,
                    avgStimulusPerHitSession = 14.2f,
                    maxStimulusInOneSession = 33.4f,
                    avgVolumePerHitSession = 860f,
                    maxVolumeInOneSession = 1320f,
                    topExampleIds = persistentListOf("pull_up"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS, MuscleEnumState.TRICEPS),
                    hitTrainingsCount = 9,
                    primaryTrainingsCount = 3,
                    avgStimulusPerHitSession = 10.1f,
                    maxStimulusInOneSession = 22.0f,
                    avgVolumePerHitSession = 540f,
                    maxVolumeInOneSession = 900f,
                    topExampleIds = persistentListOf("cable_curl"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.LEGS,
                    value = 13f,
                    muscles = persistentListOf(
                        MuscleEnumState.QUADRICEPS,
                        MuscleEnumState.HAMSTRINGS
                    ),
                    hitTrainingsCount = 7,
                    primaryTrainingsCount = 2,
                    avgStimulusPerHitSession = 9.5f,
                    maxStimulusInOneSession = 19.8f,
                    avgVolumePerHitSession = 1100f,
                    maxVolumeInOneSession = 1750f,
                    topExampleIds = persistentListOf("squat"),
                )
            )
        ),
        perMuscle = MuscleLoadBreakdownState(
            entries = persistentListOf(
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.CHEST_MUSCLES,
                    value = 52f,
                    muscles = persistentListOf(MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR),
                    hitTrainingsCount = 12,
                    primaryTrainingsCount = 8,
                    avgStimulusPerHitSession = 20.2f,
                    maxStimulusInOneSession = 44.1f,
                    avgVolumePerHitSession = 510f,
                    maxVolumeInOneSession = 780f,
                    topExampleIds = persistentListOf("incline_bench_press"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.BACK_MUSCLES,
                    value = 34f,
                    muscles = persistentListOf(MuscleEnumState.LATISSIMUS_DORSI),
                    hitTrainingsCount = 10,
                    primaryTrainingsCount = 6,
                    avgStimulusPerHitSession = 15.9f,
                    maxStimulusInOneSession = 31.7f,
                    avgVolumePerHitSession = 480f,
                    maxVolumeInOneSession = 720f,
                    topExampleIds = persistentListOf("pull_up"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.ARMS_AND_FOREARMS,
                    value = 21f,
                    muscles = persistentListOf(MuscleEnumState.BICEPS),
                    hitTrainingsCount = 9,
                    primaryTrainingsCount = 4,
                    avgStimulusPerHitSession = 12.0f,
                    maxStimulusInOneSession = 22.5f,
                    avgVolumePerHitSession = 260f,
                    maxVolumeInOneSession = 410f,
                    topExampleIds = persistentListOf("cable_curl"),
                ),
                MuscleLoadEntryState(
                    group = MuscleGroupEnumState.LEGS,
                    value = 18f,
                    muscles = persistentListOf(MuscleEnumState.QUADRICEPS),
                    hitTrainingsCount = 7,
                    primaryTrainingsCount = 2,
                    avgStimulusPerHitSession = 11.2f,
                    maxStimulusInOneSession = 19.4f,
                    avgVolumePerHitSession = 610f,
                    maxVolumeInOneSession = 980f,
                    topExampleIds = persistentListOf("squat"),
                ),
            )
        ),
        volumePerGroup = MuscleLoadBreakdownState(entries = persistentListOf()),
        volumePerMuscle = MuscleLoadBreakdownState(entries = persistentListOf()),
        dominance = MuscleLoadDominanceState(
            top1SharePercent = 52f,
            top2SharePercent = 74f,
        ),
        groupDominance = MuscleLoadDominanceState(
            top1SharePercent = 42f,
            top2SharePercent = 69f,
        ),
    )
}

