package com.grippo.core.state.metrics.distribution

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt
import kotlin.random.Random

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
        val stimulusGroups = perGroup.entries.sortedByDescending { it.value }
        val stimulusMuscles = perMuscle.entries.sortedByDescending { it.value }

        if (stimulusGroups.isEmpty() && stimulusMuscles.isEmpty()) {
            return AppTokens.strings.res(Res.string.muscle_load_summary_empty)
        }

        val top1 = groupDominance.top1SharePercent.coerceIn(0f, 100f)
        val top2 = groupDominance.top2SharePercent.coerceIn(0f, 100f)

        val top1i = top1.roundToInt().coerceIn(0, 100)
        val top2i = top2.roundToInt().coerceIn(0, 100)

        val secondShare = (top2 - top1).coerceIn(0f, 100f)
        val restShare = (100f - top2).coerceIn(0f, 100f)

        val secondI = secondShare.roundToInt().coerceIn(0, 100)
        val restI = restShare.roundToInt().coerceIn(0, 100)

        val topGroupEntry = stimulusGroups.firstOrNull()
        val secondGroupEntry = stimulusGroups.getOrNull(1)
        val weakestGroupEntry = stimulusGroups.lastOrNull()

        val topGroup = topGroupEntry?.group
        val topGroupName = topGroup?.title()?.text()
            ?: AppTokens.strings.res(Res.string.muscle_load_summary_top_group_fallback)

        val weakestGroupName = weakestGroupEntry?.group?.title()?.text()
        val weakestGroupShare = weakestGroupEntry?.value?.coerceIn(0f, 100f)

        val topMuscleEntry = stimulusMuscles.firstOrNull()
        val topMuscleName = topMuscleEntry?.muscles?.firstOrNull()?.title()?.text()
        val topMusclePercent =
            topMuscleEntry?.value?.coerceIn(0f, 100f)?.roundToInt()?.coerceIn(0, 100)

        val volumeStyleHint = buildVolumeVsStimulusHintForTopGroup(topGroup)
        val assistSuppressionHint = buildAssistSuppressionHint(topGroupEntry, secondGroupEntry)
        val tailHint = buildTailHint(
            restShare = restShare,
            weakestGroupName = weakestGroupName,
            weakestShare = weakestGroupShare
        )

        val note = AppTokens.strings.res(Res.string.muscle_load_summary_note)

        val headline = when {
            top1 >= 60f -> AppTokens.strings.res(
                Res.string.muscle_load_summary_headline_strong_focus,
                topGroupName, top1i, top2i
            )

            top2 >= 80f -> AppTokens.strings.res(
                Res.string.muscle_load_summary_headline_clear_split,
                top2i, top1i, secondI
            )

            top1 <= 42f && top2 <= 70f -> AppTokens.strings.res(
                Res.string.muscle_load_summary_headline_balanced,
                top1i, top2i
            )

            else -> AppTokens.strings.res(
                Res.string.muscle_load_summary_headline_mild_bias,
                top1i, top2i, restI
            )
        }

        val actionable = when {
            top1 >= 60f -> {
                val target = weakestGroupName
                if (target != null) {
                    AppTokens.strings.res(
                        Res.string.muscle_load_summary_action_strong_focus_with_target,
                        target
                    )
                } else {
                    AppTokens.strings.res(Res.string.muscle_load_summary_action_strong_focus_no_target)
                }
            }

            top2 >= 80f -> {
                val target = weakestGroupName
                if (target != null) {
                    AppTokens.strings.res(
                        Res.string.muscle_load_summary_action_clear_split_with_target,
                        target
                    )
                } else {
                    AppTokens.strings.res(Res.string.muscle_load_summary_action_clear_split_no_target)
                }
            }

            top1 <= 42f && top2 <= 70f -> {
                AppTokens.strings.res(Res.string.muscle_load_summary_action_balanced)
            }

            else -> {
                val target = weakestGroupName
                if (target != null) {
                    AppTokens.strings.res(
                        Res.string.muscle_load_summary_action_mild_bias_with_target,
                        target
                    )
                } else {
                    AppTokens.strings.res(Res.string.muscle_load_summary_action_mild_bias_no_target)
                }
            }
        }

        val topMuscleLine = if (topMuscleName != null && topMusclePercent != null) {
            AppTokens.strings.res(
                Res.string.muscle_load_summary_top_muscle,
                topMuscleName, topMusclePercent
            )
        } else null

        val extra = buildString {
            if (!topMuscleLine.isNullOrBlank()) append(topMuscleLine).append(" ")
            if (!volumeStyleHint.isNullOrBlank()) append(volumeStyleHint).append(" ")
            if (!assistSuppressionHint.isNullOrBlank()) append(assistSuppressionHint).append(" ")
            if (!tailHint.isNullOrBlank()) append(tailHint).append(" ")
        }.trim()

        return listOf(headline, actionable, extra, note)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }

    @Composable
    private fun buildVolumeVsStimulusHintForTopGroup(topGroup: MuscleGroupEnumState?): String? {
        if (topGroup == null) return null

        val s = perGroup.entries.firstOrNull { it.group == topGroup }?.value
            ?.coerceIn(0f, 100f) ?: return null

        val v = volumePerGroup.entries.firstOrNull { it.group == topGroup }?.value
            ?.coerceIn(0f, 100f) ?: return null

        val delta = v - s

        return when {
            delta >= 8f -> AppTokens.strings.res(
                Res.string.muscle_load_summary_volume_higher_than_stimulus,
                topGroup.title().text()
            )

            delta <= -8f -> AppTokens.strings.res(
                Res.string.muscle_load_summary_stimulus_higher_than_volume,
                topGroup.title().text()
            )

            else -> AppTokens.strings.res(Res.string.muscle_load_summary_stimulus_volume_match)
        }
    }

    @Composable
    private fun buildAssistSuppressionHint(
        topGroupEntry: MuscleLoadEntryState?,
        secondGroupEntry: MuscleLoadEntryState?,
    ): String? {
        val topGroup = topGroupEntry?.group ?: return null
        val topShare = topGroupEntry.value.coerceIn(0f, 100f)

        if (topShare < 35f) return null

        val likelyAssistGroups: Set<MuscleGroupEnumState> = when (topGroup) {
            MuscleGroupEnumState.CHEST_MUSCLES ->
                setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS, MuscleGroupEnumState.SHOULDER_MUSCLES)

            MuscleGroupEnumState.BACK_MUSCLES ->
                setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS)

            MuscleGroupEnumState.SHOULDER_MUSCLES ->
                setOf(MuscleGroupEnumState.ARMS_AND_FOREARMS)

            MuscleGroupEnumState.LEGS ->
                emptySet() // no glutes/calves split in this enum

            MuscleGroupEnumState.ABDOMINAL_MUSCLES ->
                emptySet()

            MuscleGroupEnumState.ARMS_AND_FOREARMS ->
                emptySet()
        }

        if (likelyAssistGroups.isEmpty()) {
            val second = secondGroupEntry?.group?.title()?.text()
            return if (second != null) {
                AppTokens.strings.res(Res.string.muscle_load_summary_second_driver, second)
            } else null
        }

        val assistStimulus = perGroup.entries
            .filter { it.group in likelyAssistGroups }
            .sumOf { it.value.toDouble() }
            .toFloat()
            .coerceIn(0f, 100f)

        val assistVolume = volumePerGroup.entries
            .filter { it.group in likelyAssistGroups }
            .sumOf { it.value.toDouble() }
            .toFloat()
            .coerceIn(0f, 100f)

        val isSuppressed = assistStimulus <= 10f && assistVolume >= 12f

        return if (isSuppressed) {
            AppTokens.strings.res(Res.string.muscle_load_summary_assist_suppressed)
        } else {
            val second = secondGroupEntry?.group?.title()?.text()
            if (second != null) {
                AppTokens.strings.res(Res.string.muscle_load_summary_second_driver, second)
            } else null
        }
    }


    @Composable
    private fun buildTailHint(
        restShare: Float,
        weakestGroupName: String?,
        weakestShare: Float?,
    ): String? {
        val rest = restShare.coerceIn(0f, 100f)
        if (rest >= 28f) return null

        val wName = weakestGroupName
            ?: AppTokens.strings.res(Res.string.muscle_load_summary_weakest_group_fallback)
        val wShare = weakestShare?.coerceIn(0f, 100f)?.roundToInt()?.coerceIn(0, 100)

        return if (wShare != null) {
            AppTokens.strings.res(
                Res.string.muscle_load_summary_tail_with_weakest,
                rest.roundToInt(), wName, wShare
            )
        } else {
            AppTokens.strings.res(
                Res.string.muscle_load_summary_tail_without_weakest,
                rest.roundToInt()
            )
        }
    }
}

@Immutable
public data class MuscleLoadBreakdownState(
    val entries: List<MuscleLoadEntryState>,
)

@Immutable
public data class MuscleLoadMetaState(
    val trainingsCount: Int,
    val totalExercises: Int,
    val totalSets: Int,
    val totalRepetitions: Int,
    val totalVolume: Float,
    val dominantGroup: MuscleGroupEnumState?,
)

@Immutable
public data class MuscleLoadEntryState(
    val group: MuscleGroupEnumState,
    val value: Float,
    val muscles: ImmutableList<MuscleEnumState>,
    val hitTrainingsCount: Int = 0,
    val primaryTrainingsCount: Int = 0,
    val avgStimulusPerHitSession: Float = 0f,
    val maxStimulusInOneSession: Float = 0f,
    val avgVolumePerHitSession: Float = 0f,
    val maxVolumeInOneSession: Float = 0f,
    val topExampleIds: List<String> = emptyList(),
)

@Immutable
public data class MuscleLoadDominanceState(
    val top1SharePercent: Float,
    val top2SharePercent: Float,
)

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
            entries = listOf(
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
                    topExampleIds = listOf("bench_press", "incline_bench_press"),
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
                    topExampleIds = listOf("pull_up"),
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
                    topExampleIds = listOf("cable_curl"),
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
                    topExampleIds = listOf("squat"),
                )
            )
        ),
        perMuscle = MuscleLoadBreakdownState(
            entries = listOf(
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
                    topExampleIds = listOf("incline_bench_press"),
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
                    topExampleIds = listOf("pull_up"),
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
                    topExampleIds = listOf("cable_curl"),
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
                    topExampleIds = listOf("squat"),
                ),
            )
        ),
        volumePerGroup = MuscleLoadBreakdownState(entries = emptyList()),
        volumePerMuscle = MuscleLoadBreakdownState(entries = emptyList()),
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
