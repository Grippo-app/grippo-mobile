package com.grippo.data.features.api.training

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.api.exercise.example.models.CategoryEnum
import com.grippo.data.features.api.exercise.example.models.ExamplePage
import com.grippo.data.features.api.exercise.example.models.ExampleQueries
import com.grippo.data.features.api.exercise.example.models.ExampleSortingEnum
import com.grippo.data.features.api.exercise.example.models.ExerciseExample
import com.grippo.data.features.api.exercise.example.models.ExerciseExampleComponents
import com.grippo.data.features.api.exercise.example.models.ExperienceEnum
import com.grippo.data.features.api.exercise.example.models.ForceTypeEnum
import com.grippo.data.features.api.exercise.example.models.UserExerciseExampleRules
import com.grippo.data.features.api.goal.GoalFeature
import com.grippo.data.features.api.goal.models.GoalPrimaryGoalEnum
import com.grippo.data.features.api.goal.models.GoalSecondaryGoalEnum
import com.grippo.data.features.api.muscle.models.MuscleGroupEnum
import com.grippo.data.features.api.training.models.PresetExercise
import com.grippo.data.features.api.training.models.PresetIteration
import com.grippo.data.features.api.training.models.PresetTraining
import com.grippo.data.features.api.training.models.PresetWeight
import com.grippo.data.features.api.user.UserFeature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.max
import kotlin.math.roundToInt

public class GeneratePresetTrainingUseCase(
    private val userFeature: UserFeature,
    private val goalFeature: GoalFeature,
    private val excludedMusclesFeature: ExcludedMusclesFeature,
    private val excludedEquipmentsFeature: ExcludedEquipmentsFeature,
    private val exerciseExampleFeature: ExerciseExampleFeature,
) {

    public suspend fun execute(): PresetTraining? {
        val user = userFeature.observeUser().firstOrNull() ?: return null
        val bodyWeight = user.weight.takeIf { it > 0f } ?: return null
        val goal = goalFeature.observeGoal().firstOrNull()

        val excludedMuscleIds = excludedMusclesFeature.observeExcludedMuscles().first()
            .mapTo(mutableSetOf()) { it.id }
        val excludedEquipmentIds = excludedEquipmentsFeature.observeExcludedEquipments().first()
            .mapTo(mutableSetOf()) { it.id }

        val primary = goal?.primaryGoal ?: GoalPrimaryGoalEnum.GENERAL_FITNESS
        val secondary = goal?.secondaryGoal

        val profile = applySecondary(buildProfile(primary, user.experience), secondary)

        val pool = loadExamplePool(
            experience = user.experience,
            excludedMuscleIds = excludedMuscleIds,
            excludedEquipmentIds = excludedEquipmentIds,
        ) ?: return null

        val selected = selectBalanced(pool, profile)
        if (selected.size < MIN_EXERCISES_FOR_TRAINING) return null

        val exercises = selected.map { example ->
            PresetExercise(
                exerciseExample = example.value,
                iterations = buildIterations(example, profile, bodyWeight),
            )
        }

        return PresetTraining(exercises = exercises)
    }

    // -------------------------------------------------------------------------
    // Profile: primaryGoal × experience, then soft-tune by secondaryGoal
    // -------------------------------------------------------------------------

    private fun buildProfile(
        primary: GoalPrimaryGoalEnum,
        experience: ExperienceEnum,
    ): TrainingProfile {
        val base = when (primary) {
            GoalPrimaryGoalEnum.GET_STRONGER -> TrainingProfile(
                exerciseCount = 4,
                setsPerExercise = 5,
                repsRange = 3..6,
                loadFactor = 1.1f,
                compoundShare = 0.85f,
            )

            GoalPrimaryGoalEnum.BUILD_MUSCLE -> TrainingProfile(
                exerciseCount = 6,
                setsPerExercise = 4,
                repsRange = 8..12,
                loadFactor = 0.95f,
                compoundShare = 0.55f,
            )

            GoalPrimaryGoalEnum.LOSE_FAT -> TrainingProfile(
                exerciseCount = 7,
                setsPerExercise = 3,
                repsRange = 12..18,
                loadFactor = 0.7f,
                compoundShare = 0.55f,
            )

            GoalPrimaryGoalEnum.RETURN_TO_TRAINING -> TrainingProfile(
                exerciseCount = 4,
                setsPerExercise = 3,
                repsRange = 10..12,
                loadFactor = 0.65f,
                compoundShare = 0.5f,
            )

            GoalPrimaryGoalEnum.MAINTAIN -> TrainingProfile(
                exerciseCount = 5,
                setsPerExercise = 3,
                repsRange = 8..10,
                loadFactor = 0.85f,
                compoundShare = 0.6f,
            )

            GoalPrimaryGoalEnum.GENERAL_FITNESS -> TrainingProfile(
                exerciseCount = 5,
                setsPerExercise = 3,
                repsRange = 8..15,
                loadFactor = 0.85f,
                compoundShare = 0.55f,
            )
        }

        val experienceFactor = when (experience) {
            ExperienceEnum.BEGINNER -> 0.4f
            ExperienceEnum.INTERMEDIATE -> 0.65f
            ExperienceEnum.ADVANCED -> 0.9f
            ExperienceEnum.PRO -> 1.15f
        }

        return when (experience) {
            ExperienceEnum.BEGINNER -> base.copy(
                exerciseCount = max(MIN_EXERCISES, base.exerciseCount - 1),
                setsPerExercise = max(MIN_SETS, base.setsPerExercise - 1),
                loadFactor = base.loadFactor * experienceFactor,
            )

            ExperienceEnum.INTERMEDIATE -> base.copy(
                loadFactor = base.loadFactor * experienceFactor,
            )

            ExperienceEnum.ADVANCED -> base.copy(
                exerciseCount = base.exerciseCount + 1,
                loadFactor = base.loadFactor * experienceFactor,
            )

            ExperienceEnum.PRO -> base.copy(
                exerciseCount = base.exerciseCount + 2,
                setsPerExercise = base.setsPerExercise + 1,
                loadFactor = base.loadFactor * experienceFactor,
            )
        }
    }

    private fun applySecondary(
        profile: TrainingProfile,
        secondary: GoalSecondaryGoalEnum?,
    ): TrainingProfile = when (secondary) {
        GoalSecondaryGoalEnum.LOSE_FAT -> profile.copy(
            repsRange = profile.repsRange.first..(profile.repsRange.last + 4),
        )

        GoalSecondaryGoalEnum.GET_STRONGER -> {
            val first = max(MIN_REPS_FOR_STRENGTH, profile.repsRange.first - 2)
            val last = max(first + 2, profile.repsRange.last - 3)
            profile.copy(
                loadFactor = profile.loadFactor * 1.05f,
                compoundShare = (profile.compoundShare + 0.1f).coerceAtMost(0.9f),
                repsRange = first..last,
            )
        }

        GoalSecondaryGoalEnum.BUILD_MUSCLE -> profile.copy(
            setsPerExercise = profile.setsPerExercise + 1,
        )

        GoalSecondaryGoalEnum.CONSISTENCY -> profile.copy(
            exerciseCount = max(MIN_EXERCISES, profile.exerciseCount - 1),
            setsPerExercise = max(MIN_SETS, profile.setsPerExercise - 1),
        )

        else -> profile
    }

    // -------------------------------------------------------------------------
    // Pool loading (popular first; fallback down the experience ladder)
    // -------------------------------------------------------------------------

    private suspend fun loadExamplePool(
        experience: ExperienceEnum,
        excludedMuscleIds: Set<String>,
        excludedEquipmentIds: Set<String>,
    ): List<ExerciseExample>? {
        val queries = ExampleQueries(name = null, muscleGroupId = null)
        val page = ExamplePage(limits = POOL_LIMIT, number = 1)

        return experienceLadder(experience).firstNotNullOfOrNull { exp ->
            val rules = UserExerciseExampleRules(
                excludedEquipmentIds = excludedEquipmentIds,
                excludedMuscleIds = excludedMuscleIds,
                experience = exp,
            )
            exerciseExampleFeature
                .observeExerciseExamples(
                    queries = queries,
                    sorting = ExampleSortingEnum.MostlyUsed,
                    rules = rules,
                    page = page,
                    experience = exp,
                )
                .first()
                .takeIf { it.isNotEmpty() }
        }
    }

    private fun experienceLadder(experience: ExperienceEnum): List<ExperienceEnum> =
        when (experience) {
            ExperienceEnum.PRO -> listOf(
                ExperienceEnum.PRO,
                ExperienceEnum.ADVANCED,
                ExperienceEnum.INTERMEDIATE,
                ExperienceEnum.BEGINNER,
            )

            ExperienceEnum.ADVANCED -> listOf(
                ExperienceEnum.ADVANCED,
                ExperienceEnum.INTERMEDIATE,
                ExperienceEnum.BEGINNER,
            )

            ExperienceEnum.INTERMEDIATE -> listOf(
                ExperienceEnum.INTERMEDIATE,
                ExperienceEnum.BEGINNER,
            )

            ExperienceEnum.BEGINNER -> listOf(ExperienceEnum.BEGINNER)
        }

    // -------------------------------------------------------------------------
    // Balanced selection by MuscleGroup, ForceType, Compound/Isolation share.
    // Pool is already sorted by popularity — we walk it in order, no shuffle.
    // -------------------------------------------------------------------------

    private fun selectBalanced(
        pool: List<ExerciseExample>,
        profile: TrainingProfile,
    ): List<ExerciseExample> {
        val targetCount = profile.exerciseCount
        val targetCompound =
            (targetCount * profile.compoundShare).roundToInt().coerceIn(0, targetCount)
        val targetIsolation = targetCount - targetCompound

        val compoundBucket = pool.filter { it.value.category == CategoryEnum.COMPOUND }
        val isolationBucket = pool.filter { it.value.category == CategoryEnum.ISOLATION }

        val usedMuscleGroups = mutableSetOf<MuscleGroupEnum>()
        val forceTypeCounts = ForceTypeEnum.entries.associateWith { 0 }.toMutableMap()
        val seenIds = mutableSetOf<String>()

        val compounds = pickFromBucket(
            source = compoundBucket,
            target = targetCompound,
            usedMuscleGroups = usedMuscleGroups,
            forceTypeCounts = forceTypeCounts,
            seenIds = seenIds,
            allowMuscleGroupRepeat = false,
        )

        val isolations = pickFromBucket(
            source = isolationBucket,
            target = targetIsolation,
            usedMuscleGroups = usedMuscleGroups,
            forceTypeCounts = forceTypeCounts,
            seenIds = seenIds,
            allowMuscleGroupRepeat = true,
        )

        val combined = (compounds + isolations).toMutableList()
        if (combined.size < targetCount) {
            pool.asSequence()
                .filter { it.value.id !in seenIds }
                .take(targetCount - combined.size)
                .forEach {
                    combined.add(it)
                    seenIds.add(it.value.id)
                }
        }

        return combined
    }

    private fun pickFromBucket(
        source: List<ExerciseExample>,
        target: Int,
        usedMuscleGroups: MutableSet<MuscleGroupEnum>,
        forceTypeCounts: MutableMap<ForceTypeEnum, Int>,
        seenIds: MutableSet<String>,
        allowMuscleGroupRepeat: Boolean,
    ): List<ExerciseExample> {
        if (target <= 0 || source.isEmpty()) return emptyList()

        val picked = mutableListOf<ExerciseExample>()

        for (example in source) {
            if (picked.size >= target) break
            if (example.value.id in seenIds) continue
            val group = dominantMuscleGroup(example) ?: continue
            if (!allowMuscleGroupRepeat && group in usedMuscleGroups) continue
            val ft = example.value.forceType
            val minFt = forceTypeCounts.values.minOrNull() ?: 0
            if (forceTypeCounts.getValue(ft) - minFt > 1) continue

            picked.add(example)
            seenIds.add(example.value.id)
            usedMuscleGroups.add(group)
            forceTypeCounts[ft] = forceTypeCounts.getValue(ft) + 1
        }

        if (picked.size < target) {
            for (example in source) {
                if (picked.size >= target) break
                if (example.value.id in seenIds) continue
                val group = dominantMuscleGroup(example) ?: continue
                if (!allowMuscleGroupRepeat && group in usedMuscleGroups) continue

                picked.add(example)
                seenIds.add(example.value.id)
                usedMuscleGroups.add(group)
                forceTypeCounts[example.value.forceType] =
                    forceTypeCounts.getValue(example.value.forceType) + 1
            }
        }

        if (picked.size < target) {
            for (example in source) {
                if (picked.size >= target) break
                if (example.value.id in seenIds) continue
                picked.add(example)
                seenIds.add(example.value.id)
                dominantMuscleGroup(example)?.let { usedMuscleGroups.add(it) }
                forceTypeCounts[example.value.forceType] =
                    forceTypeCounts.getValue(example.value.forceType) + 1
            }
        }

        return picked
    }

    private fun dominantMuscleGroup(example: ExerciseExample): MuscleGroupEnum? =
        example.bundles.maxByOrNull { it.percentage }?.muscle?.type?.group()

    // -------------------------------------------------------------------------
    // Iterations: identical sets per exercise; the suggested weight variant is
    // dictated by the exercise's component shape.
    // -------------------------------------------------------------------------

    private fun buildIterations(
        example: ExerciseExample,
        profile: TrainingProfile,
        bodyWeight: Float,
    ): List<PresetIteration> {
        val sets = profile.setsPerExercise
        val reps = (profile.repsRange.first + profile.repsRange.last) / 2

        val suggestedWeight: PresetWeight = when (val components = example.components) {
            is ExerciseExampleComponents.External -> PresetWeight.External(
                value = computeExternalWeight(example, profile, bodyWeight),
            )

            is ExerciseExampleComponents.BodyOnly -> PresetWeight.BodyOnly(
                bodyWeight = bodyWeight,
                multiplier = components.multiplier,
            )

            is ExerciseExampleComponents.BodyAndExtra -> PresetWeight.BodyAndExtra(
                bodyWeight = bodyWeight,
                multiplier = components.bodyMultiplier,
                extra = computeExtraWeight(profile, bodyWeight),
            )

            is ExerciseExampleComponents.BodyAndAssist -> PresetWeight.BodyAndAssist(
                bodyWeight = bodyWeight,
                multiplier = components.bodyMultiplier,
                assist = computeAssistWeight(profile, bodyWeight, components.bodyMultiplier),
            )
        }

        val template = PresetIteration(
            repetitions = reps,
            suggestedWeight = suggestedWeight,
        )
        return List(sets) { template }
    }

    private fun computeExternalWeight(
        example: ExerciseExample,
        profile: TrainingProfile,
        bodyWeight: Float,
    ): Float {
        val categoryMultiplier = when (example.value.category) {
            CategoryEnum.COMPOUND -> 1.0f
            CategoryEnum.ISOLATION -> 0.35f
        }
        val forceTypeMultiplier = when (example.value.forceType) {
            ForceTypeEnum.HINGE -> 1.2f
            ForceTypeEnum.PUSH -> 1.0f
            ForceTypeEnum.PULL -> 0.85f
        }
        val raw = bodyWeight * profile.loadFactor * categoryMultiplier * forceTypeMultiplier
        return raw.coerceAtLeast(MIN_EXTERNAL_WEIGHT).round25()
    }

    private fun computeExtraWeight(profile: TrainingProfile, bodyWeight: Float): Float {
        if (profile.loadFactor <= EXTRA_WEIGHT_THRESHOLD) return 0f
        val raw = bodyWeight * (profile.loadFactor - EXTRA_WEIGHT_THRESHOLD) * EXTRA_WEIGHT_SCALE
        return raw.coerceAtLeast(0f).round25()
    }

    private fun computeAssistWeight(
        profile: TrainingProfile,
        bodyWeight: Float,
        bodyMultiplier: Float,
    ): Float {
        val assistFraction = ((ASSIST_PIVOT - profile.loadFactor) / ASSIST_PIVOT)
            .coerceIn(0f, MAX_ASSIST_FRACTION)
        val rawAssist = bodyWeight * assistFraction
        val maxAssist = bodyWeight * bodyMultiplier - MIN_NET_LOAD
        return rawAssist
            .coerceAtMost(maxAssist)
            .coerceAtLeast(0f)
            .round25()
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private data class TrainingProfile(
        val exerciseCount: Int,
        val setsPerExercise: Int,
        val repsRange: IntRange,
        val loadFactor: Float,
        val compoundShare: Float,
    )

    private fun Float.round25(): Float = (this / 2.5f).roundToInt() * 2.5f

    private companion object {
        private const val POOL_LIMIT = 200
        private const val MIN_EXERCISES = 3
        private const val MIN_SETS = 2
        private const val MIN_REPS_FOR_STRENGTH = 3
        private const val MIN_EXERCISES_FOR_TRAINING = 2
        private const val MIN_EXTERNAL_WEIGHT = 2.5f
        private const val MIN_NET_LOAD = 5f
        private const val EXTRA_WEIGHT_THRESHOLD = 0.6f
        private const val EXTRA_WEIGHT_SCALE = 0.25f
        private const val ASSIST_PIVOT = 0.85f
        private const val MAX_ASSIST_FRACTION = 0.6f
    }
}
